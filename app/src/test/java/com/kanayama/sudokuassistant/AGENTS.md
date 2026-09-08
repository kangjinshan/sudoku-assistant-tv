# 数独助手测试包指南

> 位置：`app/src/test/java/com/kanayama/sudokuassistant/`

测试按生产模块继续分包。当前 `model/` 通过 `SudokuGeneratorTest` 验证数独规则，并通过 `TwentyFourGeneratorTest` 验证 24 点生成、整数运算、合并、重置及最后一步提示；`ViewportTransformTest` 验证跨屏幕比例坐标映射。若为 `ScoreRepository` 增加 Android 存储测试，应放到 instrumentation 测试而非伪装为纯 JVM 测试。运行入口是 `./gradlew :app:testDebugUnitTest`。
