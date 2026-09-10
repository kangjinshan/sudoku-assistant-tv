# 数独助手测试包指南

> 位置：`app/src/test/java/com/kanayama/sudokuassistant/`

测试按生产模块继续分包。当前 `model/` 通过 `SudokuGeneratorTest` 验证数独规则，并通过 `TwentyFourGeneratorTest` 验证 24 点生成、整数运算、合并、重置及最后一步提示；`ViewportTransformTest` 验证跨屏幕比例坐标映射。Android 存储与 View 交互测试使用明确标注的 Robolectric 测试，或放到 instrumentation 测试；不得当作无 Android 依赖的模型测试。运行入口是 `./gradlew :app:testDebugUnitTest`。

`GameProgressTest` 使用 Robolectric API 33 验证原生 View 的数独返回/重建、暂停计时、预选与填写恢复、成功清档，以及 24 点触摸合并后的自动选择、切换、待执行运算与菜单提示恢复、失败终局重置。`model/ProgressCodecTest` 验证全部宫格和难度的快照往返及损坏/完成快照拒绝。Robolectric 测试使用独立测试存储，不能替代设备截图验收。
