# 数独助手（电视与触摸屏版）：Agent 协作指南

> 最后更新：2026-08-30

## 1. 系统概述

本项目是面向 Android TV、小米电视、横屏 Android 手机、Android 平板及小学生家庭练习场景的离线数独应用。业务边界包括题目生成、遥控器与触摸交互、计时、答案校验、通关反馈及本地最好成绩；不包含账号、联网、广告、云同步或中途续玩。

技术栈为 Kotlin、Android 原生 `View`/`Canvas`、`SharedPreferences` 和 Gradle。界面刻意不使用 Compose：目标小米电视曾出现 Compose 首次焦点响应延迟，原生 View 是必须保留的性能约束。

## 2. 目录导航

| 目录 | 职责 | 关键说明 | 文档 |
|---|---|---|---|
| `app/` | Android 应用模块与打包配置 | Release 开启 R8、资源压缩并使用本地调试签名 | [app/AGENTS.md](app/AGENTS.md) |
| `app/src/main/java/com/kanayama/sudokuassistant/` | Activity、状态机、绘制、遥控器与触摸事件 | 遥控器进入 `handleKey`，触摸经等比坐标反算进入同一状态机 | [应用层指南](app/src/main/java/com/kanayama/sudokuassistant/AGENTS.md) |
| `app/src/main/java/com/kanayama/sudokuassistant/model/` | 数独规格、生成、校验与求解 | 支持 4×4、6×6、9×9；至少保证有解 | [模型指南](app/src/main/java/com/kanayama/sudokuassistant/model/AGENTS.md) |
| `app/src/main/java/com/kanayama/sudokuassistant/data/` | 本地成绩持久化 | 每个宫格与难度组合保留最快 10 次 | [数据指南](app/src/main/java/com/kanayama/sudokuassistant/data/AGENTS.md) |
| `app/src/main/res/` | Manifest、主题、高清图标和 TV 横幅 | Android 资源目录禁止存放 Markdown，维护规则统一见应用模块文档 | [应用模块指南](app/AGENTS.md) |
| `app/src/test/` | JVM 单元测试 | 穷举三种宫格与三档难度的生成校验 | [测试指南](app/src/test/AGENTS.md) |
| `gradle/` | Gradle Wrapper | 固定 Gradle 8.9 | [构建工具指南](gradle/AGENTS.md) |

## 3. 核心业务场景索引

- **应用启动与首键响应**
  - 入口：`MainActivity.onCreate`、`MainActivity.dispatchKeyEvent`
  - 核心逻辑：`SudokuGameView.requestFocus`、`SudokuGameView.handleKey`
  - 副作用：创建 250ms 计时刷新任务；无网络访问
- **选择宫格和难度**
  - 入口：`SudokuGameView.handleHomeKey`
  - 核心逻辑：`SudokuGameView.activateHome`、`BoardSize`、`Difficulty`
  - 副作用：只修改内存状态，开始游戏前不写磁盘
- **生成一局数独**
  - 入口：`SudokuGameView.startGame`
  - 核心逻辑：`SudokuGenerator.generate` → `generateSolution` → `isValidSolution` → `hasSolution`
  - 副作用：启动单调时钟计时；不持久化未完成棋局
- **遥控器填写数字**
  - 入口：`MainActivity.dispatchKeyEvent`
  - 核心逻辑：`SudokuGameView.handleGameKey` → `enterValue`
  - 副作用：最后一格填满后自动校验；错误格只在整盘提交后标记
- **触摸屏填写与预选数字**
  - 入口：`SudokuGameView.onTouchEvent`
  - 核心逻辑：点按可填写格打开普通数字面板；长按空格打开预选面板；`ViewportTransform` 负责反算设计坐标
  - 副作用：与遥控器路径共用 `enterValue`、候选掩码和自动提交逻辑
- **空格预选数字**
  - 入口：游戏中空格按菜单键
  - 核心逻辑：`SudokuGameView.openCandidatePicker` → `togglePickerCandidate`
  - 副作用：每格最多保留 4 个仅存内存的预选；预选不计完成度、不触发校验，正式填数后自动清除
- **正确通关与奖励**
  - 入口：`SudokuGameView.enterValue`
  - 核心逻辑：`Puzzle.isValidCompletion` 按行、列、宫和原始题面校验 → `ScoreRepository.record` → `Page.REWARD`
  - 副作用：更新 `sudoku_scores` SharedPreferences
- **查看最好成绩**
  - 入口：`SudokuGameView.handleScoresKey`
  - 核心逻辑：`ScoreRepository.scores`、`SudokuGameView.drawScores`
  - 副作用：只读本地存储
- **首页退出应用**
  - 入口：`SudokuGameView.handleHomeKey`
  - 核心逻辑：`exitOpen` / `exitSelected` 状态 → `MainActivity.finishAffinity`
  - 副作用：结束当前 Activity 任务，不清除成绩
- **电视部署**
  - 入口：`:app:assembleRelease`
  - 核心逻辑：R8/资源压缩 → `adb install -r`
  - 副作用：覆盖安装会终止当前游戏；真机部署后执行 `cmd package compile -m speed -f`

## 4. 全局设计约束

- 输入：所有 DPAD/确定/返回键必须经过原生 `View` 路径，首页首帧后立即响应；禁止重新引入 Compose 焦点链路。
- 数字面板：四宫和六宫默认聚焦数字 2，九宫默认聚焦数字 5；无既有候选数字时，预选面板沿用同一默认焦点。
- 预选：遥控器在空格按菜单键进入预选窗口；触摸屏长按空格进入。每格最多 4 个，按升序绘制到四角。
- 布局：`SudokuGameView` 以 1920×1080 为设计坐标等比居中缩放，禁止分别拉伸横纵轴；棋盘保持 984×984 设计像素，数字面板必须位于右侧且不得覆盖棋盘。
- 规则：六宫采用 2 行×3 列分宫；生成题可多解，但必须经 `hasSolution` 确认至少有解。
- 校验：填写过程不即时判错；仅在最后一个空格填满后统一校验并自动提交。不得逐格对比生成答案，多解题中任何满足原始题面及行、列、宫规则的完整答案都必须判对。
- 计时：以 `SystemClock.elapsedRealtime()` 计算真实用时，刷新任务只负责重绘，不能用累计 tick 代替真实时钟。
- 存储：只保存每个 `BoardSize × Difficulty` 的最快 10 次秒数，不保存未完成棋局。
- 发布：电视安装使用 Release APK 和覆盖安装；禁止为部署执行卸载或清除应用数据。
- 依赖：应用运行时不得增加网络、账号、音频或广告依赖。

## 5. AGENTS 维护规则

- 修改代码、配置、脚本、测试或资源后，任务完成前必须同步更新对应目录的 `AGENTS.md`。
- 当目录职责、关键入口、业务流程或约束变化时，必须同步更新父目录及本文件。
- 新增有业务逻辑、外部依赖或关键配置的目录时必须补建 `AGENTS.md`；删除或重命名文件后必须清除全部失效引用。
- 除非用户明确要求跳过，否则不得把文档维护留到后续任务。
- 发布前至少运行 `./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease`；真机改动还需检查启动、DPAD、截图和崩溃日志。
