package com.ohuang.filemanager.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 文件刷新工具类
 * 兼容 Android 所有版本，自动选择最优方案通知相册和文件管理器刷新
 */
public class MediaRefreshHelper {

    /**
     * 刷新文件到媒体库（自动判断版本和文件类型）
     *
     * @param context  上下文
     * @param filePath 文件绝对路径
     * @param callback 刷新完成回调（可为 null）
     */
    public static void refreshFile(Context context, String filePath, OnRefreshCompleteListener callback) {
        if (context == null || filePath == null || filePath.isEmpty()) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        // 使用 MediaScannerConnection 通知媒体库扫描已存在的文件
        // 这是刷新已存在文件的安全方式，不会修改或删除原文件
        // 注意：不要使用 MediaStore.insert + 复制的方式，那会导致同路径文件被截断清空
        refreshByMediaScanner(context, file, callback);
    }

    /**
     * 方式一：MediaScannerConnection（兼容 Android 10 以下，也适用于部分高版本场景）
     */
    private static void refreshByMediaScanner(Context context, File file, OnRefreshCompleteListener callback) {
        MediaScannerConnection.scanFile(
                context,
                new String[]{file.getAbsolutePath()},
                new String[]{getMimeType(file.getAbsolutePath())},
                (path, uri) -> {
                    if (callback != null) callback.onComplete(uri != null);
                }
        );
    }

    /**
     * 方式二：MediaStore API（Android 10+ 分区存储标准做法）
     */
    private static void refreshByMediaStore(Context context, File file, OnRefreshCompleteListener callback) {
        ContentResolver resolver = context.getContentResolver();
        Uri collectionUri = getMediaStoreUri(file);

        if (collectionUri == null) {
            // 如果无法确定类型，降级使用 MediaScanner
            refreshByMediaScanner(context, file, callback);
            return;
        }

        // 检查是否已存在
        String[] projection = {MediaStore.MediaColumns._ID};
        String selection = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = {file.getAbsolutePath()};

        try (android.database.Cursor cursor = resolver.query(collectionUri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                // 文件已存在，更新记录
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                Uri existingUri = Uri.withAppendedPath(collectionUri, String.valueOf(id));
                ContentValues values = buildContentValues(file);
                resolver.update(existingUri, values, null, null);
                if (callback != null) callback.onComplete(true);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 文件不存在，插入新记录
        ContentValues values = buildContentValues(file);
        Uri insertUri = resolver.insert(collectionUri, values);

        // 如果插入失败，降级使用 MediaScanner（兼容某些非标准文件）
        if (insertUri == null) {
            refreshByMediaScanner(context, file, callback);
        } else {
            // 成功插入，还需要实际写入文件内容（如果文件不在媒体库管理的目录下）
            // 注意：对于已经存在的文件，如果使用 MediaStore 插入，需要复制文件内容
            copyFileToMediaStore(context, resolver, insertUri, file);
            if (callback != null) callback.onComplete(true);
        }
    }

    /**
     * 将已存在的文件复制到 MediaStore 管理的 Uri
     * 注意：如果文件已经在公共目录（如 Pictures/），此方法可跳过
     */
    private static void copyFileToMediaStore(Context context, ContentResolver resolver, Uri uri, File sourceFile) {
        // 检查是否真的需要复制（如果文件在外部存储的公共目录，可能不需要）
        // 这里简单处理：如果文件路径包含 Environment 的公共目录，跳过复制
        String path = sourceFile.getAbsolutePath();
        boolean isInPublicDir = path.contains(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath())
                || path.contains(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath())
                || path.contains(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath());

        if (isInPublicDir && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 分区存储下，文件可能已经在公共目录，无需复制
            return;
        }

        // 如果文件路径指向应用私有目录，或者非标准目录，需要复制到 MediaStore
        try (FileInputStream fis = new FileInputStream(sourceFile);
             OutputStream os = resolver.openOutputStream(uri)) {
            if (os == null) return;
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据文件类型获取对应的 MediaStore Uri
     */
    private static Uri getMediaStoreUri(File file) {
        String mimeType = getMimeType(file.getAbsolutePath());
        if (mimeType == null) {
            return MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }

        if (mimeType.startsWith("image/")) {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("video/")) {
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("audio/")) {
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            return MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
    }

    /**
     * 构建 ContentValues
     */
    private static ContentValues buildContentValues(File file) {
        ContentValues values = new ContentValues();
        String name = file.getName();
        String mimeType = getMimeType(file.getAbsolutePath());

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");

        // Android 10+ 使用 RELATIVE_PATH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = getRelativePath(file);
            if (relativePath != null) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            }
        } else {
            // Android 10 以下使用 DATA（绝对路径）
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        }

        // 设置文件大小
        values.put(MediaStore.MediaColumns.SIZE, file.length());

        // 如果是图片或视频，可以添加额外信息（可选）
        if (mimeType != null && mimeType.startsWith("image/")) {
            // 可以添加宽高等信息，这里省略
        }

        return values;
    }

    /**
     * 获取文件的 MIME 类型
     */
    private static String getMimeType(String filePath) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        if (extension == null || extension.isEmpty()) {
            // 尝试从文件名中提取扩展名
            int dotIndex = filePath.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
                extension = filePath.substring(dotIndex + 1).toLowerCase();
            }
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    /**
     * 获取文件的相对路径（用于 Android 10+）
     */
    private static String getRelativePath(File file) {
        String path = file.getAbsolutePath();
        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        // 如果文件在外部存储下，提取相对路径
        if (path.startsWith(externalPath)) {
            String relative = path.substring(externalPath.length());
            if (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }
            // 判断文件类型，放到合适的目录
            String mimeType = getMimeType(path);
            if (mimeType != null) {
                if (mimeType.startsWith("image/")) {
                    return Environment.DIRECTORY_PICTURES + File.separator + getParentPath(relative);
                } else if (mimeType.startsWith("video/")) {
                    return Environment.DIRECTORY_MOVIES + File.separator + getParentPath(relative);
                } else if (mimeType.startsWith("audio/")) {
                    return Environment.DIRECTORY_MUSIC + File.separator + getParentPath(relative);
                }
            }
            // 默认放到 Download 目录
            return Environment.DIRECTORY_DOWNLOADS + File.separator + getParentPath(relative);
        }

        // 无法判断，返回 null（使用 MediaScanner 降级）
        return null;
    }

    /**
     * 获取父路径（去掉文件名）
     */
    private static String getParentPath(String path) {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            return path.substring(0, lastSep);
        }
        return "";
    }

    /**
     * 刷新完成回调接口
     */
    public interface OnRefreshCompleteListener {
        void onComplete(boolean success);
    }

    // ========== 简化版本（如果你只需要一个简单的调用） ==========

    /**
     * 最简调用方式（适用于大多数场景）
     * 内部自动选择最佳方案，推荐使用
     */
    public static void refreshFileSimple(Context context, String filePath) {
        refreshFile(context, filePath, null);
    }

    /**
     * 批量刷新多个文件（单次 MediaScannerConnection 调用，性能更优）
     * 适用于文件夹下载完成后的批量刷新，避免每个文件单独绑定媒体扫描服务
     */
    public static void refreshFilesSimple(Context context, String[] filePaths) {
        if (context == null || filePaths == null || filePaths.length == 0) return;

        java.util.List<String> paths = new java.util.ArrayList<>();
        java.util.List<String> mimes = new java.util.ArrayList<>();
        for (String p : filePaths) {
            if (p == null || p.isEmpty()) continue;
            File f = new File(p);
            if (f.exists()) {
                paths.add(p);
                mimes.add(getMimeType(p));
            }
        }
        if (paths.isEmpty()) return;

        MediaScannerConnection.scanFile(
                context,
                paths.toArray(new String[0]),
                mimes.toArray(new String[0]),
                null
        );
    }

    /**
     * 批量刷新多个文件（Android 10+ 可批量操作，低版本循环调用）
     */
    public static void refreshFiles(Context context, String[] filePaths, OnRefreshCompleteListener callback) {
        if (filePaths == null || filePaths.length == 0) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        // Android 10+ 可以使用 MediaStore 批量插入，简化版本这里用循环
        final int[] completed = {0};
        final boolean[] allSuccess = {true};

        for (String path : filePaths) {
            refreshFile(context, path, success -> {
                completed[0]++;
                if (!success) allSuccess[0] = false;
                if (completed[0] == filePaths.length && callback != null) {
                    callback.onComplete(allSuccess[0]);
                }
            });
        }
    }
}