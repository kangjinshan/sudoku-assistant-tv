package com.kanayama.sudokuassistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGeneratorTest {
    @Test
    fun everySizeAndDifficultyProducesValidSolvablePuzzle() {
        BoardSize.entries.forEach { size ->
            Difficulty.entries.forEach { difficulty ->
                repeat(10) { seed ->
                    val puzzle = SudokuGenerator.generate(size, difficulty, Random(seed))
                    assertTrue("${size.name} ${difficulty.name}", SudokuGenerator.isValidSolution(puzzle.solution, size))
                    assertTrue("${size.name} ${difficulty.name} should be solvable", SudokuGenerator.hasSolution(puzzle.givens, size))
                    assertEquals(SudokuGenerator.clueCount(size, difficulty), puzzle.givens.count { it != 0 })
                    puzzle.givens.forEachIndexed { index, value ->
                        if (value != 0) assertEquals(puzzle.solution[index], value)
                    }
                }
            }
        }
    }

    @Test
    fun invalidCompletedBoardIsRejected() {
        BoardSize.entries.forEach { size ->
            val solution = SudokuGenerator.generateSolution(size, Random(7))
            solution[0] = solution[1]
            assertFalse(SudokuGenerator.isValidSolution(solution, size))
        }
    }
}
