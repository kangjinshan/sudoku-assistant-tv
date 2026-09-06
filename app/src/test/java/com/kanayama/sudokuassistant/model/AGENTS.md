# 数独与 24 点模型测试指南

> 位置：`app/src/test/java/com/kanayama/sudokuassistant/model/`

`SudokuGeneratorTest` 是数独规则与棋盘规格的核心回归保护。`pickerDefaultsMatchBoardSizeUx` 锁定四宫/六宫默认数字 2、九宫默认数字 5；`everySizeAndDifficultyProducesValidSolvablePuzzle` 用固定种子覆盖全部九种组合；`invalidCompletedBoardIsRejected` 验证重复数字被拒绝；`validAlternativeCompletionIsAccepted` 保证多解题不依赖生成参考解；`completionMustKeepGivensAndFollowSudokuRules` 验证题面约束和冲突定位。修改 `BoardSize`、提示数、生成公式、`hasSolution`、`isValidSolution` 或完成校验时必须同步扩展断言，并保持测试确定性。

`TwentyFourGeneratorTest` 保护 24 点模型。`generatedPuzzlesUseFourNumbersFromOneToTenAndHaveIntegerSolution` 用 100 个固定种子验证每题恰有四个 1–10 数字并可解；`solverRejectsPuzzleWithoutAPathToTwentyFour` 覆盖已知可解与不可解输入；`divisionOnlyAcceptsIntegerResults` 锁定整除规则；`combiningRemovesTheSourceAndKeepsTheResultAtTheTarget` 验证 source 消失、结果落入 target、通关和重置；`invalidMoveDoesNotChangeTheRound` 防止非法操作污染状态。修改 `ArithmeticOperation`、`TwentyFourGenerator`、`TwentyFourRound` 或目标值时必须同步扩展断言。
