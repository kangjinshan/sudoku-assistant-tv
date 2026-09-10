package com.kanayama.sudokuassistant.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ProgressCodecTest {
    @Test fun sudokuSnapshotsRoundTripEverySizeAndDifficultyIncludingIncorrectFullBoards() {
        for (size in BoardSize.entries) for (difficulty in Difficulty.entries) {
            val puzzle = SudokuGenerator.generate(size, difficulty, Random(10))
            val entries = puzzle.givens.copyOf()
            val empty = entries.indexOfFirst { it == 0 }
            val masks = IntArray(entries.size).apply { this[empty] = 3 }
            val original = SudokuProgress(puzzle, entries, masks, empty, 123456)
            assertEquals(original.encode(), requireNotNull(SudokuProgress.decode(original.encode())).encode())
            for (index in entries.indices) if (entries[index] == 0) entries[index] = 1
            assertNotNull(SudokuProgress.decode(SudokuProgress(puzzle, entries, IntArray(entries.size), empty, 23).encode()))
        }
    }

    @Test fun malformedOrCompletedSnapshotsAreNotResumed() {
        assertNull(SudokuProgress.decode("bad"))
        assertNull(TwentyFourProgress.decode("1|1,2,3,4|0,1,DIVIDE|1|||"))
        assertNull(TwentyFourProgress.decode("1|1,2,3,4|0,1,ADD|0|||"))
        val puzzle = SudokuGenerator.generate(BoardSize.FOUR, Difficulty.EASY, Random(2))
        assertNull(SudokuProgress.decode(SudokuProgress(puzzle, puzzle.solution, IntArray(16), 0, 0).encode()))
        assertNull(SudokuProgress.decode(SudokuProgress(puzzle, puzzle.givens, IntArray(16), 16, 0).encode()))
        assertNull(SudokuProgress.decode(SudokuProgress(puzzle, puzzle.givens, IntArray(16), 0, -1).encode()))
    }
}
