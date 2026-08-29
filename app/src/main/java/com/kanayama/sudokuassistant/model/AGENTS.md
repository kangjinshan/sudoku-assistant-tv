# 数独模型模块开发指南

> 最后更新：2026-08-28  
> 位置：`app/src/main/java/com/kanayama/sudokuassistant/model/`

## 1. 模块概述

该模块生成适合四宫、六宫和九宫练习的有效题目，并按原始题面及数独规则校验最终提交。业务要求是每题至少有解，不强制唯一解；任一合法完整答案都应通过。

## 2. 核心代码结构

| 文件 | 职责 | 关键类 / 方法 |
|---|---|---|
| `Sudoku.kt` | 全部领域模型与算法及棋盘交互规格 | `BoardSize`、`defaultPickerValue`、`Difficulty`、`Puzzle.isValidCompletion`、`SudokuGenerator.generate`、`isValidSolution`、`conflictingCells`、`hasSolution` |

## 3. 核心业务流程

- **生成完整盘**：`generate` → `generateSolution` → 行带/列栈/数字随机置换 → `isValidSolution`。
- **生成题面**：按 `clueCount` 随机清零 → `hasSolution` 回溯验证 → 创建 `Puzzle`。
- **最终校验**：`Puzzle.isValidCompletion` 先确认原始已知数未变，再以 `isValidSolution` 验证每行、每列、每宫；不与 `Puzzle.solution` 逐格比较。
- **冲突定位**：非法完整盘由 `conflictingCells` 找出行、列或宫内的重复数字，供 UI 标出需要检查的玩家填写格。

## 4. 关键资源与副作用

- 无外部存储、网络或异步副作用。
- `Puzzle.solution` 是生成阶段保留的参考解，不得用于最终通关判定；`solution` 和 `givens` 均为可变数组，调用方不得修改。
- 已知数：四宫 12/10/8，六宫 27/22/18，九宫 54/43/32。
- 数字面板默认值：四宫 2、六宫 2、九宫 5；UI 的普通面板和空白预选面板统一读取 `BoardSize.defaultPickerValue`。

## 5. 常见修改场景与切入点

- 改难度数量：修改 `SudokuGenerator.clueCounts`，同步 README 和测试。
- 改六宫分宫：修改 `BoardSize.SIX`，同步校验、绘制和说明文档。
- 要求唯一解：扩展 `hasSolution` 为计数求解器，生成阶段在移除数字后限制解数为 1。
- 新增宫格：更新 `BoardSize`、生成模式、UI picker 列数和全部参数化测试。
- 改数字面板默认焦点：修改 `BoardSize.defaultPickerValue`，同步 README、UI 指南和模型测试。
- 修复非法题：从 `isValidSolution`、`canPlace` 和 `hasSolution` 依次排查。

## 6. 维护与风险说明

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| 分宫尺寸与边长不整除 | 低 | 生成错误盘 | 新规格必须满足 `blockRows × blockColumns == side` |
| 提示数过少导致回溯变慢 | 中 | 开局按键卡顿 | 压测生成时间，必要时后台预生成 |
| 修改 pattern 后出现非法解 | 中 | 所有题不可用 | 运行全部 `SudokuGeneratorTest` |
