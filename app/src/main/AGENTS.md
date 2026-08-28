# 主源集指南

> 位置：`app/src/main/`

`AndroidManifest.xml` 声明普通与 Leanback 启动入口、横屏和无触摸要求；`java/` 保存 Kotlin 运行逻辑；`res/` 保存主题与图标。当前 Manifest 使用 `mipmap-nodpi/ic_launcher_hd.png` 和 `drawable-nodpi/tv_banner_hd.png`，以避免电视桌面对低分辨率资源二次放大。修改包名时必须同步 Manifest、Gradle namespace/applicationId 和源码包路径。不得增加网络权限或后台服务。Android 的 `res/` 子目录只允许合法资源文件，因此资源维护说明统一记录在 `app/AGENTS.md`，不得在 `res/` 内放 Markdown。
