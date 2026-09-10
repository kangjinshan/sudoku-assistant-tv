# Java 包根导航

> 位置：`app/src/main/java/com/`

本目录仅承载反向域名包结构。当前唯一应用命名空间是 `com.kanayama.sudokuassistant`，必须与 `app/build.gradle.kts` 的 namespace 和 Manifest 包组件一致。业务入口见 `kanayama/sudokuassistant/AGENTS.md`。


两种玩法通过 `ProgressRepository` 保存未完成进度；`MainActivity.onPause/onResume` 管理数独暂停计时，返回首页及重新进入不换题。24 点合并后自动选择结果，支持点击其他数字切换。详见应用交互及 data/model 指南。
