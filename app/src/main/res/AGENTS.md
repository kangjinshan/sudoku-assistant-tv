# Android 资源层指南

> 最后更新：2026-08-28  
> 位置：`app/src/main/res/`

## 1. 概述

资源层定义应用名称、启动主题、图标和 TV Launcher 横幅。游戏主体由 `SudokuGameView` Canvas 绘制，不在 XML 中维护页面布局。

## 2. 核心组件

- `values/strings.xml`：应用名“数独助手”。
- `values/colors.xml`：窗口启动背景 `app_background`。
- `values/themes.xml`：API 24+ 基础主题和首帧深色背景。
- `values-v31/themes.xml`：Android 12+ 启动画面图标与背景。
- `mipmap-*/ic_launcher.png`：按 mdpi 到 xxxhdpi 提供的应用图标。
- `drawable-nodpi/app_icon_source.png`：用户提供的高分辨率图标源图，也供 README 展示。
- `drawable/tv_banner.xml`：Leanback/电视桌面横幅。

## 3. 设计约定

- 图标源图更新后必须重新生成全部五档 mipmap，禁止只替换单一密度。
- 启动背景必须保持 `#08131D`，避免首帧前出现突兀白屏。
- TV 横幅与应用图标用途不同；修改 `android:banner` 前必须在小米桌面验证裁切。
- 资源文件不得包含密钥、用户信息或网络地址。

## 4. 典型调用路径

```text
AndroidManifest.xml
  → Theme.SudokuAssistant
  → values[/v31]/themes.xml
  → 启动背景与图标
```

