# 测试目录指南

> 最后更新：2026-08-28  
> 位置：`app/src/test/`

## 1. 概述

该目录包含不依赖设备的 JVM 测试，重点防止随机题目生成产生非法、无解或提示数不符合难度配置的题目，并保护多解题的规则校验行为。

## 2. 核心组件

- `SudokuGeneratorTest.everySizeAndDifficultyProducesValidSolvablePuzzle`：对 3 种棋盘 × 3 种难度 × 10 个种子验证。
- `SudokuGeneratorTest.invalidCompletedBoardIsRejected`：确认重复数字会被拒绝。
- `SudokuGeneratorTest.validAlternativeCompletionIsAccepted`：确认不同于生成参考解的合法答案可以通关。
- `SudokuGeneratorTest.completionMustKeepGivensAndFollowSudokuRules`：确认提交必须保留题面并满足数独规则，同时验证冲突定位。
- `SudokuGenerator.generate`：被测生成入口。
- `SudokuGenerator.isValidSolution`：完整盘合法性断言。
- `SudokuGenerator.hasSolution`：题面可解性断言。

## 3. 设计约定

- 测试必须使用固定 `Random(seed)`，避免随机失败不可复现。
- 新增棋盘或难度时必须自动进入枚举遍历，不应只测试九宫。
- UI 遥控器行为必须在 TV 真机或模拟器验证，不能用 JVM 测试替代。

## 4. 典型调用路径

运行 `./gradlew :app:testDebugUnitTest`，测试报告位于 `app/build/reports/tests/testDebugUnitTest/`。
