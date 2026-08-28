# Gradle Wrapper 指南

> 最后更新：2026-08-28  
> 位置：`gradle/`

## 1. 概述

该目录固定项目构建工具版本，确保本地与 CI 使用一致的 Gradle 运行时；不包含应用业务逻辑。

## 2. 核心组件

- `wrapper/gradle-wrapper.properties`：固定 Gradle 8.9 下载地址。
- `wrapper/gradle-wrapper.jar`：Wrapper 启动实现。
- 根目录 `gradlew`：macOS/Linux 构建入口。
- 根目录 `gradlew.bat`：Windows 构建入口。
- 根目录 `build.gradle.kts`：声明 Android Gradle Plugin 与 Kotlin 插件版本。

## 3. 设计约定

- 升级 Gradle 时同时验证 Android Gradle Plugin 兼容矩阵。
- 禁止手工修改 `gradle-wrapper.jar`。
- Wrapper 升级后至少运行单元测试、Release Lint 和 Release 构建。
- 不提交 `.gradle/` 缓存或本机 `local.properties`。

## 4. 典型调用路径

```text
./gradlew
  → gradle-wrapper.properties 选择 Gradle 8.9
  → 根 build.gradle.kts 加载插件
  → app/build.gradle.kts 构建 APK
```
