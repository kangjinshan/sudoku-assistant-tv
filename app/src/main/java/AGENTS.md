# 生产代码命名空间导航

> 位置：`app/src/main/java/`

生产 Kotlin 代码位于 `com/kanayama/sudokuassistant/`。`MainActivity` 接收系统按键与返回操作，`SudokuGameView` 执行页面状态机和触摸路由，`ViewportTransform` 负责跨屏幕比例坐标映射，`model` 负责数独生成，`data` 负责成绩存储。禁止在上层包路径新增无命名空间源码。
