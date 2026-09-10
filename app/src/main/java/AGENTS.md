# 生产代码命名空间导航

> 位置：`app/src/main/java/`

生产 Kotlin 代码位于 `com/kanayama/sudokuassistant/`。`MainActivity` 接收系统按键与返回操作，`SudokuGameView` 执行数独与 24 点页面状态机和触摸路由，`ViewportTransform` 负责跨屏幕比例坐标映射，`model` 负责两种玩法的生成与规则，`data` 负责数独成绩与两种玩法的未完成进度存储。禁止在上层包路径新增无命名空间源码。

两种玩法通过 `ProgressRepository` 保存未完成进度；`MainActivity.onPause/onResume` 管理数独暂停计时，返回首页及重新进入不换题。24 点合并后自动选择结果，支持点击其他数字切换。详见应用交互及 data/model 指南。
