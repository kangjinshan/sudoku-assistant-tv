# Android 值资源指南

> 最后更新：2026-08-28  
> 位置：`app/src/main/res/values/`

## 1. 概述

本目录维护应用名称、启动背景色和基础主题。页面配色由 `SudokuGameView` 的绘制常量定义，这里只处理窗口创建前后的系统资源。

## 2. 核心组件

- `strings.xml`：声明 `app_name`。
- `colors.xml`：声明与游戏背景一致的 `app_background`。
- `themes.xml`：设置无标题栏、横屏全屏背景和导航栏颜色。
- `../values-v31/themes.xml`：为 Android 12+ 补充系统启动画面。
- `AndroidManifest.xml`：通过 `Theme.SudokuAssistant` 使用这些资源。

## 3. 设计约定

- 基础主题中的属性必须兼容 API 24；API 31+ 属性只能放在 `values-v31`。
- `windowBackground` 必须与 `SudokuGameView` 的 `ink` 色一致，避免启动闪屏。
- 修改主题后必须在小米电视冷启动并检查窗口焦点、DPAD 首键和屏幕边缘。

## 4. 典型调用路径

系统创建窗口 → 加载 `Theme.SudokuAssistant` → 显示 `app_background` → `MainActivity` 安装 `SudokuGameView`。
