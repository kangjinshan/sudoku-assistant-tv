# 数独与 24 点模型测试指南

> 位置：`app/src/test/java/com/kanayama/sudokuassistant/model/`

`SudokuGeneratorTest` 是数独规则与棋盘规格的核心回归保护。`pickerDefaultsMatchBoardSizeUx` 锁定四宫/六宫默认数字 2、九宫默认数字 5；`everySizeAndDifficultyProducesValidSolvablePuzzle` 用固定种子覆盖全部九种组合；`invalidCompletedBoardIsRejected` 验证重复数字被拒绝；`validAlternativeCompletionIsAccepted` 保证多解题不依赖生成参考解；`completionMustKeepGivensAndFollowSudokuRules` 验证题面约束和冲突定位。修改 `BoardSize`、提示数、生成公式、`hasSolution`、`isValidSolution` 或完成校验时必须同步扩展断言，并保持测试确定性。

`TwentyFourGeneratorTest` 保护 24 点模型。`generatedPuzzlesUseFourNumbersFromOneToTenAndHaveIntegerSolution` 用 100 个固定种子验证每题恰有四个 1–10 数字并可解；`solverRejectsPuzzleWithoutAPathToTwentyFour` 覆盖已知可解与不可解输入；`divisionOnlyAcceptsIntegerResults` 锁定整除规则；`combiningRemovesTheSourceAndKeepsTheResultAtTheTarget` 验证 source 消失、结果落入 target、通关和重置；`invalidMoveDoesNotChangeTheRound` 防止非法操作污染状态。`finalStepHintOnlyRevealsTheLastOperation` 验证最后一步文案、允许 `1 × 24`、负数括号和减除顺序；生成题测试还独立解析 100 个种子的完整参考解，验证用完全部原始数字、整数中间结果及提示对应最终运算。修改 `ArithmeticOperation`、`TwentyFourGenerator`、`TwentyFourRound` 或目标值时必须同步扩展断言。

`GameProgressTest` 使用 Robolectric API 33 验证原生 View 的数独返回/重建、暂停计时、预选与填写恢复、成功清档，以及 24 点触摸合并后的自动选择、切换、待执行运算与菜单提示恢复、失败终局重置。`model/ProgressCodecTest` 验证全部宫格和难度的快照往返及损坏/完成快照拒绝。Robolectric 测试使用独立测试存储，不能替代设备截图验收。
