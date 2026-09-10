# 组织命名空间导航

> 位置：`app/src/main/java/com/kanayama/`

本目录隔离 `kanayama` 组织下的应用包。当前仅包含 `sudokuassistant/`；新模块不得直接放在此层。包名调整会影响安装升级兼容性和 SharedPreferences 数据继承，必须谨慎。


两种玩法通过 `ProgressRepository` 保存未完成进度；`MainActivity.onPause/onResume` 管理数独暂停计时，返回首页及重新进入不换题。24 点合并后自动选择结果，支持点击其他数字切换。详见应用交互及 data/model 指南。
