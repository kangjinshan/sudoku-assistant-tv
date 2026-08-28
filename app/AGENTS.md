# Android 应用模块指南

> 最后更新：2026-08-28  
> 位置：`app/`

## 1. 概述

该模块产出数独助手 TV APK。`build.gradle.kts` 定义包名、SDK、Release 压缩和签名；`src/main` 保存生产代码与资源；`src/test` 保存纯 JVM 规则测试。

## 2. 核心组件

- `build.gradle.kts`：`applicationId` 为 `com.kanayama.sudokuassistant`，最低 API 24；Release 必须启用 `isMinifyEnabled` 与 `isShrinkResources`。
- `proguard-rules.pro`：仅承载应用专属 R8 规则；当前业务无反射序列化。
- `src/main/AndroidManifest.xml`：同时声明普通 Launcher 与 Leanback Launcher，横屏运行且不要求触摸屏。
- `src/main/java/com/kanayama/sudokuassistant/MainActivity.kt`：应用和遥控器入口。
- `src/main/java/com/kanayama/sudokuassistant/SudokuGameView.kt`：页面状态机、绘制、计时和输入核心。
- `src/test/java/com/kanayama/sudokuassistant/model/SudokuGeneratorTest.kt`：生成器回归测试。

## 3. 设计约定

- 生产 UI 使用原生 View，不引入 Compose；这是小米电视首键延迟的兼容性约束。
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

