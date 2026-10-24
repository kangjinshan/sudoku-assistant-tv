# 主源集指南

> 位置：`app/src/main/`

`AndroidManifest.xml` 声明普通与 Leanback 启动入口、横屏和无触摸要求；`java/` 保存 Kotlin 运行逻辑；`res/` 保存主题与图标。当前 Manifest 使用 `mipmap-nodpi/ic_launcher_hd.png` 和 `drawable-nodpi/tv_banner_hd.png`，以避免电视桌面对低分辨率资源二次放大。修改包名时必须同步 Manifest、Gradle namespace/applicationId 和源码包路径。不得增加网络权限或后台服务。Android 的 `res/` 子目录只允许合法资源文件，因此资源维护说明统一记录在 `app/AGENTS.md`，不得在 `res/` 内放 Markdown。

两种玩法通过 `ProgressRepository` 保存未完成进度；`MainActivity.onPause/onResume` 管理数独暂停计时，返回首页及重新进入不换题。24 点合并后自动选择结果，支持点击其他数字切换。详见应用交互及 data/model 指南。

24 点的视觉层由 `TwentyFourRenderer` 单独承担，`SudokuGameView` 保留输入与游戏状态；绘制和触摸使用同一组布局矩形。本地纸艺 WebP 的资源与来源规则见 `app/AGENTS.md` 和项目根 `output/imagegen/AGENTS.md`。


## 电视启动图标与横幅（2026-10-24）

1.8.1（versionCode 15）使用 `res/drawable-nodpi/tv_brand_banner.png` 的 1280×720 横幅，图案与“数独助手”分开放置，电视距离下清楚可辨。Manifest 的 TV 横幅独立于方形启动图标；保留原包名、应用名称、业务逻辑与用户数据。图案来源和哈希见本模块 `tv-brand-assets.json`。本次资源变更按发布构建、签名兼容、电视启动和桌面截图验收。
