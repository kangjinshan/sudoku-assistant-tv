# 数独生成器测试指南

> 位置：`app/src/test/java/com/kanayama/sudokuassistant/model/`

`SudokuGeneratorTest` 是数独规则的核心回归保护。`everySizeAndDifficultyProducesValidSolvablePuzzle` 用固定种子覆盖全部九种组合；`invalidCompletedBoardIsRejected` 验证重复数字被拒绝；`validAlternativeCompletionIsAccepted` 保证多解题不依赖生成参考解；`completionMustKeepGivensAndFollowSudokuRules` 验证题面约束和冲突定位。修改 `BoardSize`、提示数、生成公式、`hasSolution`、`isValidSolution` 或完成校验时必须同步扩展断言，并保持测试确定性。
