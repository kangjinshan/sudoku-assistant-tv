# 数独与 24 点模型模块开发指南

> 最后更新：2026-09-08
> 位置：`app/src/main/java/com/kanayama/sudokuassistant/model/`

## 1. 模块概述

该模块提供两套离线数字游戏规则：生成适合四宫、六宫和九宫练习的有效数独，并生成由四个 1–10 数字组成、存在整数四则运算解的 24 点题目。数独不强制唯一解；24 点每一步除法都必须整除。

## 2. 核心代码结构

| 文件 | 职责 | 关键类 / 方法 |
|---|---|---|
| `Sudoku.kt` | 全部领域模型与算法及棋盘交互规格 | `BoardSize`、`defaultPickerValue`、`Difficulty`、`Puzzle.isValidCompletion`、`SudokuGenerator.generate`、`isValidSolution`、`conflictingCells`、`hasSolution` |
| `TwentyFour.kt` | 24 点题目生成、整数解搜索、单步运算与局面状态 | `ArithmeticOperation.apply`、`TwentyFourGenerator.generate`、`findSolution`、`findFinalStep`、`TwentyFourFinalStep.expression`、`TwentyFourRound.combine`、`TwentyFourMoveStatus` |

## 3. 核心业务流程

- **生成完整盘**：`generate` → `generateSolution` → 行带/列栈/数字随机置换 → `isValidSolution`。
- **生成题面**：按 `clueCount` 随机清零 → `hasSolution` 回溯验证 → 创建 `Puzzle`。
- **最终校验**：`Puzzle.isValidCompletion` 先确认原始已知数未变，再以 `isValidSolution` 验证每行、每列、每宫；不与 `Puzzle.solution` 逐格比较。
- **冲突定位**：非法完整盘由 `conflictingCells` 找出行、列或宫内的重复数字，供 UI 标出需要检查的玩家填写格。
- **生成 24 点题目**：`TwentyFourGenerator.generate` 随机取 4 个 1–10 数字 → `findSolutionTerm` 递归组合两项 → 仅保留 `ArithmeticOperation.apply` 接受的整数结果 → 找到 24 后返回完整表达式及同一解法的最后一步；`findSolution` 返回完整表达式，`findFinalStep` 返回最后一步两个整数和运算符。
- **执行 24 点运算**：`TwentyFourRound.combine(sourceIndex, targetIndex, operation)` → 按顺序计算 `source operation target` → 清空 source → 把结果写入 target → 根据剩余数量返回 `APPLIED`、`SOLVED` 或 `NOT_TWENTY_FOUR`。
- **重置 24 点题目**：`TwentyFourRound.reset` 把四个位置恢复为不可变的 `initialNumbers`；不重新随机生成。

## 4. 关键资源与副作用

- 无外部存储、网络或异步副作用。
- `Puzzle.solution` 是生成阶段保留的参考解，不得用于最终通关判定；`solution` 和 `givens` 均为可变数组，调用方不得修改。
- 已知数：四宫 12/10/8，六宫 27/22/18，九宫 54/43/32。
- 数字面板默认值：四宫 2、六宫 2、九宫 5；UI 的普通面板和空白预选面板统一读取 `BoardSize.defaultPickerValue`。
- 24 点初始数字限定为 1–10；中间结果可为 0 或负数。`DIVIDE` 在除数为 0 或不能整除时返回 `null`，UI 必须保留原局面并提示用户。
- `TwentyFourPuzzle.solutionExpression` 保存完整参考解，不直接显示给玩家；`finalStep` 保存同一解法的最终两个整数和运算符，供菜单键提示。`TwentyFourFinalStep.expression` 不含 `= 24`，负数加括号。加法和乘法按数值升序展示操作数，减法与除法保持顺序；`1 × 24` 等合法最后一步不筛除。24 点过程、提示、结果和成功状态不持久化。

## 5. 常见修改场景与切入点

- 改难度数量：修改 `SudokuGenerator.clueCounts`，同步 README 和测试。
- 改六宫分宫：修改 `BoardSize.SIX`，同步校验、绘制和说明文档。
- 要求唯一解：扩展 `hasSolution` 为计数求解器，生成阶段在移除数字后限制解数为 1。
- 新增宫格：更新 `BoardSize`、生成模式、UI picker 列数和全部参数化测试。
- 改数字面板默认焦点：修改 `BoardSize.defaultPickerValue`，同步 README、UI 指南和模型测试。
- 修复非法题：从 `isValidSolution`、`canPlace` 和 `hasSolution` 依次排查。
- 修改 24 点数字范围或目标：同步修改 `TwentyFourPuzzle` 校验、`TwentyFourGenerator.generate/findSolution`、`TwentyFourRound.isSolved` 和测试，不能只改 UI 文案。
- 修改运算规则：从 `ArithmeticOperation.apply` 入手，并回归求解器与 `TwentyFourRound.combine`；生成器和玩家操作必须使用完全相同的整除语义。
- 修改数字合并方向：从 `TwentyFourRound.combine` 入手；当前业务约束是 source 消失、结果写入 target。

## 6. 维护与风险说明

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| 分宫尺寸与边长不整除 | 低 | 生成错误盘 | 新规格必须满足 `blockRows × blockColumns == side` |
| 提示数过少导致回溯变慢 | 中 | 开局按键卡顿 | 压测生成时间，必要时后台预生成 |
| 修改 pattern 后出现非法解 | 中 | 所有题不可用 | 运行全部 `SudokuGeneratorTest` |
| 生成器与玩家运算规则不一致 | 中 | 题目理论可解但界面无法完成 | 两条路径统一调用 `ArithmeticOperation.apply`，运行 `TwentyFourGeneratorTest` |
| 允许非整除除法 | 中 | 出现分数并破坏整数玩法 | `DIVIDE` 必须同时检查除数非零和余数为零 |
