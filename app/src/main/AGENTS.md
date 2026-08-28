# 主源集指南

> 位置：`app/src/main/`

`AndroidManifest.xml` 声明普通与 Leanback 启动入口、横屏和无触摸要求；`java/` 保存 Kotlin 运行逻辑；`res/` 保存主题与图标。修改包名时必须同步 Manifest、Gradle namespace/applicationId 和源码包路径。不得增加网络权限或后台服务。

