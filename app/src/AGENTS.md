# Android 源集导航

> 位置：`app/src/`

本目录分为 `main/` 生产代码与资源、`test/` JVM 测试。生产变更需同时检查对应测试；测试代码不得进入主 APK。入口依次为 `main/AndroidManifest.xml`、`MainActivity`、`SudokuGameView`；数独与 24 点规则测试入口分别为 `SudokuGeneratorTest` 和 `TwentyFourGeneratorTest`。

两种玩法通过 `ProgressRepository` 保存未完成进度；`MainActivity.onPause/onResume` 管理数独暂停计时，返回首页及重新进入不换题。24 点合并后自动选择结果，支持点击其他数字切换。详见应用交互及 data/model 指南。
