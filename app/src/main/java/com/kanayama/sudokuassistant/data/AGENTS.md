# 成绩存储层开发指南

> 最后更新：2026-08-28  
> 位置：`app/src/main/java/com/kanayama/sudokuassistant/data/`

## 1. 概述

本层只负责离线最好成绩的读写，不处理计时、通关判断或 UI。数据使用 SharedPreferences，适合当前单用户、低数据量电视应用。

## 2. 核心组件

- `ScoreRepository`：唯一存储入口。
- `ScoreRepository.scores`：解析、排序并最多返回 10 条秒数。
- `ScoreRepository.record`：加入新成绩、重新排序、截取最快 10 条并返回是否刷新第一名。
- `ScoreRepository.key`：以 `BoardSize.name` 与 `Difficulty.name` 组成稳定键。
- `MAX_SCORES`：每个组合固定为 10。

## 3. 设计约定

- 存储文件名固定为 `sudoku_scores`。
- 时间单位固定为秒，使用逗号分隔的 Long 列表。
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

