# Android 应用模块指南

> 最后更新：2026-08-30
> 位置：`app/`

## 1. 概述

该模块产出数独助手 TV APK。`build.gradle.kts` 定义包名、SDK、Release 压缩和签名；`src/main` 保存生产代码与资源；`src/test` 保存纯 JVM 规则测试。

## 2. 核心组件

- `build.gradle.kts`：`applicationId` 为 `com.kanayama.sudokuassistant`，最低 API 24，当前版本为 `1.2.0 (7)`；Release 必须启用 `isMinifyEnabled` 与 `isShrinkResources`。
- `proguard-rules.pro`：仅承载应用专属 R8 规则；当前业务无反射序列化。
- `src/main/AndroidManifest.xml`：同时声明普通 Launcher 与 Leanback Launcher，横屏运行且不要求触摸屏。
- `src/main/java/com/kanayama/sudokuassistant/MainActivity.kt`：应用、遥控器和系统返回入口。
- `src/main/java/com/kanayama/sudokuassistant/SudokuGameView.kt`：页面状态机、绘制、计时、遥控器和触摸输入核心。
- `src/main/java/com/kanayama/sudokuassistant/ViewportTransform.kt`：将不同宽高比设备的视图坐标等比映射到 1920×1080 设计坐标。
- `src/test/java/com/kanayama/sudokuassistant/model/SudokuGeneratorTest.kt`：生成器、整盘规则校验与多解题回归测试。

## 3. 设计约定

- 生产 UI 使用原生 View，不引入 Compose；这是小米电视首键延迟的兼容性约束。
- 横屏手机和平板使用与电视相同的 1920×1080 设计坐标，必须等比居中并通过 `ViewportTransform` 反算触摸坐标。
- 游戏中确定键打开普通填数窗口；空格按菜单键打开预选窗口，窗口内菜单键切换最多 4 个预选数字、确定键保存。
- 数字面板默认焦点由 `BoardSize.defaultPickerValue` 决定：四宫和六宫为 2，九宫为 5；空白预选面板沿用该值。
- 完成一盘后按原始题面及行、列、宫规则判定，不得要求玩家答案与生成时保留的某一组解逐格相同。
- Release 当前用 debug signingConfig 便于同一台开发电视覆盖安装；正式商店分发前必须替换为受控发布密钥。
- 不得提升最低 SDK 而不验证 Android 7.0 电视兼容性。
- 不得添加网络权限或远程服务；应用应保持完全离线。

## 4. 典型调用路径

```text
系统启动 MainActivity
  → 创建 SudokuGameView
  → dispatchKeyEvent 转发遥控器按键
  → SudokuGameView 更新状态并 invalidate
  → onDraw 立即绘制新画面
```

构建：`./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease`。

## 5. 资源维护

- `src/main/res/mipmap-nodpi/ic_launcher_hd.png`：Manifest、圆形图标和 Android 12+ 启动画面引用的 1254×1254 高清图标。
- `src/main/res/drawable-nodpi/app_icon_source.png`：README 展示和后续重制使用的源图。
- `src/main/res/drawable-nodpi/tv_banner_hd.png`：1280×720 的电视桌面横幅。
- `src/main/res/values/themes.xml`：API 24+ 基础主题；`values-v31/themes.xml` 增加系统启动画面。
- Android `res/` 及其子目录只能包含合法资源文件，禁止在其中放置 `AGENTS.md` 或其他任意扩展名文件。
- 主图标使用 `nodpi` 高清资源，避免电视桌面选择低分辨率 mipmap 后再次放大。
