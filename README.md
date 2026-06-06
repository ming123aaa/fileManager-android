# File Manager

一个基于 Android 的现代化文件管理器应用，支持文件浏览、预览、编辑、上传和管理功能。

需配合服务端使用-[FileManager Server](https://github.com/ming123aaa/fileManager-SpringBoot)

## 功能特性

### 文件管理
-  树形文件目录浏览
-  文件搜索和过滤
-  文件排序（名称、大小、日期）


### 文件操作
-  文件下载（支持系统下载器）
-  文件上传
-  文件重命名
-  文件移动
-  文件删除

### 文件预览与编辑
-  图片预览
-  文本文件编辑
-  WebView 预览（支持视频全屏播放）


### 界面特性
-  日间/夜间主题
-  响应式设计
-  输入法避让
-  上传成功自动刷新列表

## 技术栈

- **框架**: Android Jetpack Compose
- **语言**: Kotlin / Java


## 项目结构

```
app/
├── src/main/java/com/ohuang/filemanager/
│   ├── MainActivity.kt          # 主入口
│   ├── WebActivity.java         # WebView 预览页面
│   ├── UploadActivity.kt        # 文件上传页面
│   ├── ui/
│   │   ├── components/          # UI 组件
│   │   │   ├── Dialogs.kt       # 对话框组件
│   │   │   ├── FileCard.kt      # 文件卡片
│   │   │   └── FileList.kt      # 文件列表
│   │   ├── screens/             # 屏幕页面
│   │   │   └── FileManagerScreen.kt
│   │   └── viewmodel/           # 视图模型
│   │       └── FileViewModel.kt
│   ├── data/                    # 数据层
│   ├── service/                 # 服务层
│   └── util/                    # 工具类
└── src/main/res/                # 资源文件
    ├── layout/                  # 布局文件
    └── values/                  # 配置文件
```

## 安装与运行


### 构建命令

```bash
# 构建调试版本
./gradlew assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

## 使用说明

### 基本操作
1. 打开应用进入文件管理器首页
2. 点击文件夹进入子目录
3. 点击文件根据类型执行相应操作：
   - 图片/视频/音频：预览
   - 文本文件：编辑
   - 其他文件：下载

### 上下文菜单
长按文件或文件夹可以打开上下文菜单，包含：
- 打开/预览
- 下载
- 复制链接
- 重命名
- 移动
- 删除

## 开发说明

### 代码规范
- 使用 Kotlin 编写新代码
- 遵循 Android Jetpack 架构模式
- 使用 ViewModel 管理 UI 状态
- 使用 Compose 构建 UI

### 扩展功能
- 添加新功能时请在 `FileViewModel.kt` 中添加对应的方法
- 新的 UI 组件请放在 `ui/components/` 目录下
- 新的屏幕页面请放在 `ui/screens/` 目录下

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

---

**Version**: 1.0
**Author**: Ohuang
**Date**: 2024