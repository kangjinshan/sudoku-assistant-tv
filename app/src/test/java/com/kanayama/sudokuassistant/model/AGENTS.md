# 数独生成器测试指南

> 位置：`app/src/test/java/com/kanayama/sudokuassistant/model/`

`SudokuGeneratorTest` 是数独规则的核心回归保护。`everySizeAndDifficultyProducesValidSolvablePuzzle` 用固定种子覆盖全部九种组合；`invalidCompletedBoardIsRejected` 验证重复数字被拒绝。修改 `BoardSize`、提示数、生成公式、`hasSolution` 或 `isValidSolution` 时必须同步扩展断言，并保持测试确定性。
