# 测试目录指南

> 最后更新：2026-09-10
> 位置：`app/src/test/`

## 1. 概述

该目录包含不依赖设备的 JVM 测试，重点防止数独或 24 点随机题目生成非法或无解题目，保护数独多解规则、24 点整数运算与合并规则，并验证不同手机和平板宽高比的设计坐标映射。

## 2. 核心组件

- `SudokuGeneratorTest.everySizeAndDifficultyProducesValidSolvablePuzzle`：对 3 种棋盘 × 3 种难度 × 10 个种子验证。
- `SudokuGeneratorTest.pickerDefaultsMatchBoardSizeUx`：锁定四宫/六宫默认数字 2、九宫默认数字 5。
- `SudokuGeneratorTest.invalidCompletedBoardIsRejected`：确认重复数字会被拒绝。
- `SudokuGeneratorTest.validAlternativeCompletionIsAccepted`：确认不同于生成参考解的合法答案可以通关。
- `SudokuGeneratorTest.completionMustKeepGivensAndFollowSudokuRules`：确认提交必须保留题面并满足数独规则，同时验证冲突定位。
- `SudokuGenerator.generate`：被测生成入口。
- `SudokuGenerator.isValidSolution`：完整盘合法性断言。
- `SudokuGenerator.hasSolution`：题面可解性断言。
- `TwentyFourGeneratorTest.generatedPuzzlesUseFourNumbersFromOneToTenAndHaveIntegerSolution`：对 100 个固定种子验证数字范围与整数可解性，并独立解析完整参考解，确认用完所有原始数字、每步整除及最终运算与提示一致。
- `TwentyFourGeneratorTest.divisionOnlyAcceptsIntegerResults`：锁定除数非零且必须整除的约束。
- `TwentyFourGeneratorTest.combiningRemovesTheSourceAndKeepsTheResultAtTheTarget`：验证 source 消失、结果落到 target、最终 24 通关及重置。
- `TwentyFourGeneratorTest.invalidMoveDoesNotChangeTheRound`：验证非法除法不会修改局面。
- `TwentyFourGeneratorTest.finalStepHintOnlyRevealsTheLastOperation`：验证 `1 × 24` 合法提示、仅最后一步的文案、负数括号及减除操作数顺序。
- `ViewportTransformTest`：验证 16:9、超宽手机和 4:3 平板的等比居中及触摸坐标反算。

## 3. 设计约定

- 测试必须使用固定 `Random(seed)`，避免随机失败不可复现。
- 新增棋盘或难度时必须自动进入枚举遍历，不应只测试九宫。
- 修改 24 点数字范围、目标值、运算规则或合并方向时必须扩展 `TwentyFourGeneratorTest`；固定随机种子，禁止用偶发随机成功代替断言。
- UI 遥控器和触摸行为必须在对应真机或模拟器验证，坐标数学测试不能替代实际点击验收。

## 4. 典型调用路径

运行 `./gradlew :app:testDebugUnitTest`，测试报告位于 `app/build/reports/tests/testDebugUnitTest/`。

`GameProgressTest` 使用 Robolectric API 33 验证原生 View 的数独返回/重建、暂停计时、预选与填写恢复、成功清档，以及 24 点触摸合并后的自动选择、切换、待执行运算与菜单提示恢复、失败终局重置。`model/ProgressCodecTest` 验证全部宫格和难度的快照往返及损坏/完成快照拒绝。Robolectric 测试使用独立测试存储，不能替代设备截图验收。
