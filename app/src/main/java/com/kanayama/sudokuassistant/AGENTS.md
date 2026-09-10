# 应用交互模块开发指南

> 最后更新：2026-09-10
> 位置：`app/src/main/java/com/kanayama/sudokuassistant/`

## 1. 模块概述

本模块解决电视遥控器与横屏触摸设备在数独和 24 点两种玩法中的完整交互问题。它用单一原生自绘 View 保证低性能电视上的首键响应，并以统一状态机提供触摸输入、稳定计时和可预测焦点，不承担题目算法与永久成绩格式定义。

## 2. 核心代码结构

| 文件/目录 | 职责 | 关键类 / 方法 |
|---|---|---|
| `MainActivity.kt` | 创建唯一游戏 View，最早接收系统按键和系统返回 | `MainActivity.onCreate`、`dispatchKeyEvent`、`onBackPressed`、`onPause/onResume` |
| `SudokuGameView.kt` | 数独与 24 点页面状态、Canvas 绘制、遥控器与触摸路由、计时和通关编排 | `SudokuGameView`、`handleKey`、`onTouchEvent`、`startGame`、`startTwentyFourGame`、`selectTwentyFourNumber` |
| `ViewportTransform.kt` | 将手机、平板和电视视图坐标等比映射到设计坐标 | `ViewportTransform.fit`、`toDesignPoint` |
| `model/` | 数独与 24 点的规格、生成、求解和运算状态 | `BoardSize`、`Difficulty`、`SudokuGenerator`、`TwentyFourGenerator`、`TwentyFourRound` |
| `data/` | 最好成绩与未完成进度存储 | `ScoreRepository`、`ProgressRepository` |
| `Page` | 首页、数独、24 点、奖励、成绩五态枚举 | `HOME`、`GAME`、`TWENTY_FOUR`、`REWARD`、`SCORES` |

## 3. 核心业务流程

- **首键响应**：Android `KeyEvent` → `MainActivity.dispatchKeyEvent` → `SudokuGameView.handleKey` → 对应页面 handler → `invalidate`。
- **触摸输入**：Android `MotionEvent` → `GestureDetector` → `ViewportTransform.toDesignPoint` → 页面 tap/long-press handler → 与遥控器共用状态变更方法。
- **开始游戏**：`handleHomeKey` → `activateHome` → `startGame` → 优先读取对应宫格与难度的 `SudokuProgress`，无存档才生成 → 恢复题盘、预选、位置与累计用时。
- **填写数字**：`handleGameKey` → 确定键打开普通 picker → 按 `BoardSize.defaultPickerValue` 初始化焦点（四宫/六宫为 2，九宫为 5）→ 改变 `pickerSelection` → `enterValue` → 清除该格预选 → 必要时自动提交。
- **预选数字**：空格按菜单键 → `openCandidatePicker` → 菜单键通过 `togglePickerCandidate` 切换草稿（最多 4 个）→ 确定键保存；返回键放弃本次草稿。
- **触摸填数**：点按可填写格打开普通 picker，点按数字立即填入；长按空格打开预选 picker，点按数字切换草稿，通过“保存预选”提交。
- **清除数字**：普通 picker 底部提供“清除”，绘制和触摸共用 `PickerLayout.clearButton`；遥控器从数字最底行按下设置 `pickerClearFocused`，按上恢复原数字焦点。点按清除或聚焦后确定均关闭面板并调用 `enterValue(0)`，清空当前可填写格及预选、重置错误提示，不触发自动提交，保持计时和棋盘焦点。每次打开普通或预选 picker 都重置清除焦点。
- **开始 24 点**：首页 `homeFocus == 7` → `resumeTwentyFourGame` → 恢复 `TwentyFourProgress` 的题目、合并历史、焦点、选择和提示；无存档才调用 `startTwentyFourGame`。
- **24 点遥控器输入**：`handleTwentyFourKey` → `moveTwentyFourFocus` 在 2×2 数字、四个运算符和三个操作按钮间移动 → `activateTwentyFourFocus` 执行选择。
- **24 点触摸输入**：`handleTwentyFourTap` 使用 `twentyFourNumberRect`、`twentyFourOperationRect`、`twentyFourUtilityRect` 命中同一组状态变更方法。
- **24 点数字合并**：`selectTwentyFourNumber` 记录第一个数字 → `selectTwentyFourOperation` 记录运算符 → 再次 `selectTwentyFourNumber` 调用 `TwentyFourRound.combine`；第一个位置清空，结果留在第二个位置；未到终局时 `twentyFourSource` 与 `twentyFourFocus` 均指向结果，可直接选运算符或点其他数字切换。空数字卡不能获得触摸焦点。
- **24 点重置与换题**：`resetTwentyFourGame` 恢复当前 `TwentyFourRound.initialNumbers`；`startTwentyFourGame` 重新生成题目。重置通过页面“重置”按钮执行。
- **24 点提示**：遥控器菜单键 → `showTwentyFourHint` → 读取原题 `TwentyFourPuzzle.finalStep.expression`，提示区只显示最后一步两个整数和运算符（含 `1 × 24`），不显示完整表达式或等号结果；不修改局面、选择和焦点。继续操作会替换提示文案，再按菜单键可重看。
- **计时刷新**：`onAttachedToWindow` → `ticker` 每 250ms 触发 → 用累计毫秒加本次 `SystemClock.elapsedRealtime` 差值重算秒数 → `invalidate`。
- **通关记录**：`enterValue` 调用 `Puzzle.isValidCompletion` 校验题面约束及行、列、宫规则 → `ScoreRepository.record` → 删除该组合进度 → `Page.REWARD`；多解题的任一合法答案均可通关。
- **暂停与保存**：`handleKey` / 触摸完成后 `saveProgress`；`showHome` 和 `MainActivity.onPause` 调用 `pauseGame`，累计计时并保存，`onResume` 通过 `resumeGame` 接续。首页“继续数独”根据所选组合是否有进度显示。
- **退出应用**：首页返回键打开确认状态 → 左右切换 `exitSelected` → 确定后调用 Activity 提供的 `exitApp`。

## 4. 关键资源与副作用

- 本地存储：`ScoreRepository` 使用名为 `sudoku_scores` 的 SharedPreferences。
- 定时任务：`ticker` 在 View attach 时启动、detach 时移除；禁止产生多个重复 callback。
- 进程状态：未完成数独保存到 `sudoku_progress`，包含已确认填写、预选、格子位置及累计毫秒；同宫格和难度恢复原题。数字面板草稿不自动确认。
- 24 点状态：`TwentyFourProgress` 保存原题和最多三步合法合并历史、焦点、已选数字、运算符与提示；恢复后仍可重置原题。失败终局保留，成功时删除进度。
- 外部依赖：无网络、数据库、消息队列或后台服务。

## 5. 常见修改场景与切入点

- 调整首页焦点路径：修改 `handleHomeKey`，并同步检查 `drawHome` 与 `handleHomeTap` 中的 `homeFocus` 索引；当前 6/7/8 分别为成绩、24 点、开始数独。
- 调整数独盘尺寸：修改 `drawGame` 的 `boardPixels`；同时验证 4/6/9 三种字号和粗分隔线。
- 调整设计画布或宽高比适配：同步修改 `ViewportTransform` 常量、Canvas 变换、触摸反算测试，禁止横纵轴独立缩放。
- 调整数字浮层：修改 `drawPicker` 的 `panelWidth`、`key` 和右边界，确保左边界大于棋盘右边界 1080。
- 调整清除按钮：同步检查 `PickerLayout.clearButton`、`pickerClearFocused`、`handlePickerTap`、`handleGameKey` 和 `drawPicker`；两种面板底部预留 170 设计像素，四宫/六宫/九宫均须可达且不覆盖棋盘。
- 调整数字浮层默认焦点：修改 `BoardSize.defaultPickerValue`，并同步 README、模型测试和空白预选面板行为。
- 调整预选交互：同步检查 `pickerMode`、`pickerDraftMask`、`candidateMasks`、`togglePickerCandidate` 与 `drawGame` 的四角绘制。
- 新增页面：扩展 `Page`、`handleKey` 和 `onDraw` 三处分支。
- 调整 24 点布局：同步修改 `twentyFourNumberRect`、`twentyFourOperationRect`、`twentyFourUtilityRect`、`drawTwentyFour` 与 `moveTwentyFourFocus`，保证绘制、触摸命中和遥控器路径一致。
- 调整 24 点操作：从 `selectTwentyFourNumber`、`selectTwentyFourOperation` 和 `TwentyFourRound.combine` 入手；必须回归数字消失位置、结果落点、整除限制、重置和最后结果判断。
- 修改计时：从 `ticker` 与 `startedAt` 入手，不能靠 tick 次数累加。
- 修改通关逻辑：从 `enterValue` 与 `Puzzle.isValidCompletion` 入手，并回归多解题、`ScoreRepository.record` 与奖励页。

## 6. 维护与风险说明

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| 按键被 Compose/焦点框架延迟 | 高（已发生） | 首次 10 秒不可操作 | 坚持原生 View；真机冷启动后 1 秒内发送 DPAD 验证 |
| 设计坐标修改导致过扫描裁切 | 中 | 边缘按钮不可见 | 在 1920×1080 真机截图检查四边至少 48px 安全区 |
| 触摸坐标与绘制缩放不一致 | 中 | 手机或平板点错格子 | 绘制和点击统一使用 `ViewportTransform`，覆盖宽屏和 4:3 单元测试 |
| ticker 未移除造成泄漏 | 低 | 后台耗电和重复刷新 | 保持 `onDetachedFromWindow` 调用 `removeCallbacks` |
| 自动提交误触发 | 中 | 未填完或错误答案进入奖励页 | 保持 `entries.all` 后再校验题面约束及完整数独规则 |
| 24 点焦点落到已消失数字 | 中 | 遥控器无法继续选择第二个数 | `moveTwentyFourFocus` 必须跳过 `TwentyFourRound.values` 中的 `null`，真机逐步合并验证 |
| 运算顺序颠倒 | 中 | 减法、除法得到错误结果 | 始终把第一个数字作为 `left`、第二个数字作为 `right`，并由 `TwentyFourRound.combine` 统一处理 |
