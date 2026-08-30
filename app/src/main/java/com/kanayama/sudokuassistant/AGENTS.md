# 应用交互模块开发指南

> 最后更新：2026-08-30
> 位置：`app/src/main/java/com/kanayama/sudokuassistant/`

## 1. 模块概述

本模块解决电视遥控器与横屏触摸设备从启动到通关的完整交互问题。它用单一原生自绘 View 保证低性能电视上的首键响应，并以统一状态机提供触摸输入、稳定计时和可预测焦点，不承担题目算法与永久成绩格式定义。

## 2. 核心代码结构

| 文件/目录 | 职责 | 关键类 / 方法 |
|---|---|---|
| `MainActivity.kt` | 创建唯一游戏 View，最早接收系统按键和系统返回 | `MainActivity.onCreate`、`dispatchKeyEvent`、`onBackPressed` |
| `SudokuGameView.kt` | 页面状态、Canvas 绘制、遥控器与触摸路由、计时和通关编排 | `SudokuGameView`、`handleKey`、`onTouchEvent`、`startGame`、`enterValue` |
| `ViewportTransform.kt` | 将手机、平板和电视视图坐标等比映射到设计坐标 | `ViewportTransform.fit`、`toDesignPoint` |
| `model/` | 棋盘规格和生成/求解算法 | `BoardSize`、`Difficulty`、`SudokuGenerator` |
| `data/` | 最好成绩存储 | `ScoreRepository` |
| `Page` | 首页、游戏、奖励、成绩四态枚举 | `HOME`、`GAME`、`REWARD`、`SCORES` |

## 3. 核心业务流程

- **首键响应**：Android `KeyEvent` → `MainActivity.dispatchKeyEvent` → `SudokuGameView.handleKey` → 对应页面 handler → `invalidate`。
- **触摸输入**：Android `MotionEvent` → `GestureDetector` → `ViewportTransform.toDesignPoint` → 页面 tap/long-press handler → 与遥控器共用状态变更方法。
- **开始游戏**：`handleHomeKey` → `activateHome` → `startGame` → `SudokuGenerator.generate` → 初始化题盘与 `startedAt`。
- **填写数字**：`handleGameKey` → 确定键打开普通 picker → 按 `BoardSize.defaultPickerValue` 初始化焦点（四宫/六宫为 2，九宫为 5）→ 改变 `pickerSelection` → `enterValue` → 清除该格预选 → 必要时自动提交。
- **预选数字**：空格按菜单键 → `openCandidatePicker` → 菜单键通过 `togglePickerCandidate` 切换草稿（最多 4 个）→ 确定键保存；返回键放弃本次草稿。
- **触摸填数**：点按可填写格打开普通 picker，点按数字立即填入；长按空格打开预选 picker，点按数字切换草稿，通过“保存预选”提交。
- **计时刷新**：`onAttachedToWindow` → `ticker` 每 250ms 触发 → 用 `SystemClock.elapsedRealtime` 重算秒数 → `invalidate`。
- **通关记录**：`enterValue` 调用 `Puzzle.isValidCompletion` 校验题面约束及行、列、宫规则 → `ScoreRepository.record` → `Page.REWARD`；多解题的任一合法答案均可通关。
- **退出应用**：首页返回键打开确认状态 → 左右切换 `exitSelected` → 确定后调用 Activity 提供的 `exitApp`。

## 4. 关键资源与副作用

- 本地存储：`ScoreRepository` 使用名为 `sudoku_scores` 的 SharedPreferences。
- 定时任务：`ticker` 在 View attach 时启动、detach 时移除；禁止产生多个重复 callback。
- 进程状态：未完成棋局仅存在内存，覆盖安装、进程终止或重新启动不会恢复。
- 外部依赖：无网络、数据库、消息队列或后台服务。

## 5. 常见修改场景与切入点

- 调整首页焦点路径：修改 `handleHomeKey`，并同步检查 `drawHome` 中的 `homeFocus` 索引。
- 调整数独盘尺寸：修改 `drawGame` 的 `boardPixels`；同时验证 4/6/9 三种字号和粗分隔线。
- 调整设计画布或宽高比适配：同步修改 `ViewportTransform` 常量、Canvas 变换、触摸反算测试，禁止横纵轴独立缩放。
- 调整数字浮层：修改 `drawPicker` 的 `panelWidth`、`key` 和右边界，确保左边界大于棋盘右边界 1080。
- 调整数字浮层默认焦点：修改 `BoardSize.defaultPickerValue`，并同步 README、模型测试和空白预选面板行为。
- 调整预选交互：同步检查 `pickerMode`、`pickerDraftMask`、`candidateMasks`、`togglePickerCandidate` 与 `drawGame` 的四角绘制。
- 新增页面：扩展 `Page`、`handleKey` 和 `onDraw` 三处分支。
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
