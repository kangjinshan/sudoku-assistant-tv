# Android 源集导航

> 位置：`app/src/`

本目录分为 `main/` 生产代码与资源、`test/` JVM 测试。生产变更需同时检查对应测试；测试代码不得进入主 APK。入口依次为 `main/AndroidManifest.xml`、`MainActivity`、`SudokuGameView`，规则测试入口为 `SudokuGeneratorTest`。

