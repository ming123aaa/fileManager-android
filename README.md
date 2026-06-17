# File Manager

一个基于 Android 的现代化文件管理器应用，支持文件浏览、预览、编辑、上传和管理功能。同时内置 AndServer 服务端，可作为独立的文件管理服务使用。

服务端项目-[FileManager Server](https://github.com/ming123aaa/fileManager-SpringBoot)

## 功能特性

### 文件管理
- 树形文件目录浏览
- 文件搜索和过滤
- 文件排序（名称、大小、日期）

### 文件操作
- 文件下载（支持系统下载器）
- 文件上传
- 文件重命名
- 文件移动
- 文件删除

### 文件预览与编辑
- 图片预览
- 文本文件编辑
- WebView 预览（支持视频全屏播放）

### 界面特性
- 日间/夜间主题
- 响应式设计
- 输入法避让
- 上传成功自动刷新列表

### 服务管理
- 内置 AndServer HTTP 服务
- 服务启动/停止控制
- 端口配置
- 文件目录选择
- 开机自启设置
- 权限管理

## 技术栈

- **框架**: Android Jetpack Compose
- **语言**: Kotlin / Java
- **HTTP 服务**: AndServer 2.1.12

## 项目结构

```
app/
├── src/main/java/com/ohuang/filemanager/
│   ├── MainActivity.kt              # 主入口
│   ├── MainComposeActivity.kt       # 主界面
│   ├── WebActivity.java             # WebView 预览页面
│   ├── UploadActivity.kt            # 文件上传页面
│   ├── ServiceLauncherActivity.kt   # 服务管理页面
│   ├── MyApp.kt                     # 应用入口
│   ├── ui/
│   │   ├── components/              # UI 组件
│   │   │   ├── Dialogs.kt           # 对话框组件
│   │   │   ├── FileCard.kt          # 文件卡片
│   │   │   ├── FileList.kt          # 文件列表
│   │   │   └── FileListItem.kt      # 文件列表项
│   │   ├── screens/                 # 屏幕页面
│   │   │   ├── FileManagerScreen.kt
│   │   │   └── SettingsScreen.kt    # 设置页面
│   │   └── viewmodel/               # 视图模型
│   │       └── FileViewModel.kt
│   ├── server/                      # AndServer 服务端
│   │   ├── MutableWebServer.kt      # Web 服务器配置
│   │   ├── NormalServer.java        # 服务器实现
│   │   ├── adapter/                 # 适配器
│   │   │   └── DownloadAdapter.kt   # 下载适配器
│   │   ├── bean/                    # 数据模型
│   │   │   └── FileBean.kt          # 文件实体
│   │   ├── config/                  # 配置
│   │   │   └── AppConfig.kt         # 应用配置
│   │   ├── controller/              # API 控制器
│   │   │   ├── MainApiController.kt # 主 API 控制器
│   │   │   └── TestApiController.kt # 测试 API 控制器
│   │   ├── interceptor/             # 拦截器
│   │   │   └── LogResolver.kt       # 日志拦截器
│   │   └── util/                    # 工具类
│   │       ├── AppContext.kt        # 全局上下文
│   │       └── DownloadUtil.java    # 下载工具
│   ├── service/                     # Android 服务
│   │   └── UploadService.kt         # 上传服务
│   └── util/                        # 工具类
│       ├── ClipboardUtils.kt        # 剪贴板工具
│       ├── NetWorkUtil.kt           # 网络工具
│       └── SPUtil.kt                # 存储工具
└── src/main/res/                    # 资源文件
    ├── layout/                      # 布局文件
    └── values/                      # 配置文件
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

### 服务管理
1. 进入服务管理页面（设置 → 服务管理）
2. 选择文件目录作为服务根目录
3. 配置服务端口（默认 8080）
4. 点击启动按钮启动 HTTP 服务
5. 其他设备可通过 IP:端口 访问服务

### API 接口
服务启动后提供以下 RESTful API：
- `GET /file/list` - 获取文件列表
- `GET /file/download` - 下载文件
- `POST /file/upload` - 上传文件
- `POST /file/delete` - 删除文件
- `POST /file/rename` - 重命名文件
- `POST /file/move` - 移动文件
- `POST /folder/create` - 创建文件夹

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
- 新的 API 接口请放在 `server/controller/` 目录下

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

---

**Version**: 1.0
**Author**: Ohuang
**Date**: 2024