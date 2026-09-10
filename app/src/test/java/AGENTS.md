# JVM 测试包导航

> 位置：`app/src/test/java/`

本目录只包含本地 JVM 测试包层级。实际测试位于 `com/kanayama/sudokuassistant/model/`，不依赖 Android 设备。新增测试应镜像生产源码包名，并保持可由 `testDebugUnitTest` 独立运行。


`GameProgressTest` 使用 Robolectric API 33 验证原生 View 的数独返回/重建、暂停计时、预选与填写恢复、成功清档，以及 24 点触摸合并后的自动选择、切换、待执行运算与菜单提示恢复、失败终局重置。`model/ProgressCodecTest` 验证全部宫格和难度的快照往返及损坏/完成快照拒绝。Robolectric 测试使用独立测试存储，不能替代设备截图验收。
