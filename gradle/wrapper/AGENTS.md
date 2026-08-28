# Gradle Wrapper 文件指南

> 最后更新：2026-08-28  
> 位置：`gradle/wrapper/`

## 1. 概述

本目录保存可复现构建所需的 Gradle Wrapper 元数据，不承载应用业务逻辑。

## 2. 核心组件

- `gradle-wrapper.properties`：指定 Gradle 8.9；必须与 Android Gradle Plugin 8.7.3 兼容。
- `gradle-wrapper.jar`：由 Gradle 官方 wrapper 任务生成，禁止手工编辑。
- 根目录 `gradlew`：读取本目录配置并启动构建。
- 根目录 `build.gradle.kts`：声明需要由 Wrapper 执行的插件。
- `app/build.gradle.kts`：最终消费构建运行时的 Android 模块。

## 3. 设计约定

- 版本升级必须通过 Wrapper 任务完成，并提交 properties 与 jar 的配套变化。
- 不得把本机 SDK 路径写入本目录。
- 修改后必须执行一次干净的 Release 构建。

## 4. 典型调用路径

`./gradlew :app:assembleRelease` → Wrapper 下载/选择 Gradle → 加载插件 → 输出签名 Release APK。

