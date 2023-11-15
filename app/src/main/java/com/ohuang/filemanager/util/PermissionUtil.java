package com.ohuang.filemanager.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {

    private static final int request_code = 1234;

    public static boolean check(Activity activity,String[] permissions){
        List<String> data = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            int checkSelfPermission = activity.checkPermission(permissions[i], Process.myPid(), Process.myUid());
            if (PackageManager.PERMISSION_GRANTED == checkSelfPermission) {
                continue;
            }
            data.add(permissions[i]);
        }
        return data.size() == 0;
    }
    public static boolean check(Activity activity,List<String> permissions){
        String[] p = new String[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            p[i] = permissions.get(i);
        }
        return check(activity,p);
    }

    public static boolean checkAndRequest(Activity context, List<String> permissions){
        String[] p = new String[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            p[i] = permissions.get(i);
        }
        return checkAndRequest(context,p);
    }

    public static boolean checkAndRequest(Activity context, String[] permissions) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            int checkSelfPermission = context.checkPermission(permissions[i], Process.myPid(), Process.myUid());
            if (PackageManager.PERMISSION_GRANTED == checkSelfPermission) {
                continue;
            }
            data.add(permissions[i]);
        }
      if (data.size()==0){
          return true;
      }else {
          request(context,data);
          return false;
      }
    }

    public static void request(Activity context, String[] permission) {
        request(context, permission, request_code);
    }

    public static void request(Activity context, String[] permission, int code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.requestPermissions(permission, code);
        }
    }

    public static void request(Activity context, List<String> permission, int code) {
        String[] p = new String[permission.size()];
        for (int i = 0; i < permission.size(); i++) {
            p[i] = permission.get(i);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.requestPermissions(p, code);
        }
    }

    public static void request(Activity context, List<String> permission) {
        request(context, permission, request_code);
    }

    public static boolean onRequestPermissionsResult(int code, int[] grantResults, int requestCode) {
        if (code == requestCode) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean onRequestPermissionsResult(int code, int[] grantResults) {
        return onRequestPermissionsResult(code, grantResults, request_code);
    }
}
