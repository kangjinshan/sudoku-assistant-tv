package com.kanayama.sudokuassistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGeneratorTest {
    @Test
    fun pickerDefaultsMatchBoardSizeUx() {
        assertEquals(2, BoardSize.FOUR.defaultPickerValue)
        assertEquals(2, BoardSize.SIX.defaultPickerValue)
        assertEquals(5, BoardSize.NINE.defaultPickerValue)
    }

    @Test
    fun expertHasFewerCluesThanHardForEveryBoardSize() {
        val expectedClues = mapOf(BoardSize.FOUR to 6, BoardSize.SIX to 14, BoardSize.NINE to 24)
        expectedClues.forEach { (size, count) ->
            assertEquals(count, SudokuGenerator.clueCount(size, Difficulty.EXPERT))
            val counts = Difficulty.entries.map { SudokuGenerator.clueCount(size, it) }
            assertTrue(counts.zipWithNext().all { (easier, harder) -> easier > harder })
        }
    }

    @Test
    fun everySizeAndDifficultyProducesValidSolvablePuzzle() {
        BoardSize.entries.forEach { size ->
            Difficulty.entries.forEach { difficulty ->
                repeat(if (difficulty == Difficulty.EXPERT) 100 else 10) { seed ->
                    val puzzle = SudokuGenerator.generate(size, difficulty, Random(seed))
                    assertTrue("${size.name} ${difficulty.name}", SudokuGenerator.isValidSolution(puzzle.solution, size))
                    val originalGivens = puzzle.givens.copyOf()
                    assertTrue("${size.name} ${difficulty.name} should be solvable", SudokuGenerator.hasSolution(puzzle.givens, size))
                    assertArrayEquals(originalGivens, puzzle.givens)
                    assertEquals(SudokuGenerator.clueCount(size, difficulty), puzzle.givens.count { it != 0 })
                    puzzle.givens.forEachIndexed { index, value ->
                        if (value != 0) assertEquals(puzzle.solution[index], value)
                    }
                }
            }
        }
    }

    @Test
    fun solverRejectsUnsolvablePartialBoardWithoutChangingGivens() {
        val givens = intArrayOf(
            1, 2, 3, 0,
            3, 4, 1, 0,
            2, 1, 4, 0,
            4, 3, 0, 2,
        )
        val original = givens.copyOf()
        assertFalse(SudokuGenerator.hasSolution(givens, BoardSize.FOUR))
        assertArrayEquals(original, givens)
    }

    @Test
    fun invalidCompletedBoardIsRejected() {
        BoardSize.entries.forEach { size ->
            val solution = SudokuGenerator.generateSolution(size, Random(7))
            solution[0] = solution[1]
            assertFalse(SudokuGenerator.isValidSolution(solution, size))
        }
    }

    @Test
    fun validAlternativeCompletionIsAccepted() {
        val generatedSolution = intArrayOf(
            1, 2, 3, 4,
            3, 4, 1, 2,
            2, 1, 4, 3,
            4, 3, 2, 1,
        )
        val givens = intArrayOf(
            1, 2, 3, 4,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val alternativeSolution = intArrayOf(
            1, 2, 3, 4,
            3, 4, 1, 2,
            4, 3, 2, 1,
            2, 1, 4, 3,
        )
        val puzzle = Puzzle(BoardSize.FOUR, Difficulty.HARD, generatedSolution, givens)

        assertFalse(alternativeSolution.contentEquals(generatedSolution))
        assertTrue(puzzle.isValidCompletion(alternativeSolution))
    }

    @Test
    fun completionMustKeepGivensAndFollowSudokuRules() {
        val solution = intArrayOf(
            1, 2, 3, 4,
            3, 4, 1, 2,
            2, 1, 4, 3,
            4, 3, 2, 1,
        )
        val givens = IntArray(16).also { it[0] = 1 }
        val puzzle = Puzzle(BoardSize.FOUR, Difficulty.HARD, solution, givens)
        val changedGiven = solution.map { value ->
            when (value) {
                1 -> 2
                2 -> 1
                else -> value
            }
        }.toIntArray()
        val duplicate = solution.copyOf().also { it[1] = 1 }

        assertTrue(SudokuGenerator.isValidSolution(changedGiven, BoardSize.FOUR))
        assertFalse(puzzle.isValidCompletion(changedGiven))
        assertFalse(puzzle.isValidCompletion(duplicate))
        assertEquals(setOf(0, 1, 9), SudokuGenerator.conflictingCells(duplicate, BoardSize.FOUR))
    }
}
