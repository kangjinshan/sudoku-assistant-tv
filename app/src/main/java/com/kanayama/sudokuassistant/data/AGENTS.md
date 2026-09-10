# 本地存储层开发指南

> 最后更新：2026-09-10
> 位置：`app/src/main/java/com/kanayama/sudokuassistant/data/`

## 1. 概述

本层负责离线最好成绩与两种玩法未完成进度的读写，不处理计时、通关判断或 UI。数据使用 SharedPreferences，适合当前单用户、低数据量电视应用。

## 2. 核心组件

- `ScoreRepository`：成绩存储入口。
- `ScoreRepository.scores`：解析、排序并最多返回 10 条秒数。
- `ScoreRepository.record`：加入新成绩、重新排序、截取最快 10 条并返回是否刷新第一名。
- `ScoreRepository.key`：以 `BoardSize.name` 与 `Difficulty.name` 组成稳定键。
- `ProgressRepository`：使用独立的 `sudoku_progress` 保存各宫格 × 难度的数独快照，以及单局 24 点快照；`latest` 保存最近数独组合以恢复首页选择。`load/loadTwentyFour` 使用模型校验快照，损坏数据不恢复。
- `saveTwentyFour` 在成功时删除进度；失败终局可保留并重置。`clear` 仅删除对应数独组合，不影响其他组合与成绩。
- `MAX_SCORES`：每个组合固定为 10。

## 3. 设计约定

- 成绩文件名固定为 `sudoku_scores`，进度文件名为 `sudoku_progress`。
- 成绩时间单位为秒，使用逗号分隔的 Long 列表；进度的累计时间单位为毫秒，模型负责带版本号的编解码。
- 写入使用 `apply()` 异步提交；UI 不应等待磁盘。
- 禁止在此层引用 View、Activity 或题目生成器。

## 4. 典型调用路径

```text
SudokuGameView.enterValue
  → 全盘正确
  → ScoreRepository.record(size, difficulty, elapsedSeconds)
  → 返回是否新纪录
  → SudokuGameView 绘制奖励页
```

