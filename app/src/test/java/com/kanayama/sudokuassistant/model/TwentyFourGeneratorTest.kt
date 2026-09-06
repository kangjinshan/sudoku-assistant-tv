package com.kanayama.sudokuassistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TwentyFourGeneratorTest {
    @Test
    fun generatedPuzzlesUseFourNumbersFromOneToTenAndHaveIntegerSolution() {
        repeat(100) { seed ->
            val puzzle = TwentyFourGenerator.generate(Random(seed))

            assertEquals(4, puzzle.numbers.size)
            assertTrue(puzzle.numbers.all { it in 1..10 })
            assertNotNull(TwentyFourGenerator.findSolution(puzzle.numbers))
        }
    }

    @Test
    fun solverRejectsPuzzleWithoutAPathToTwentyFour() {
        assertNull(TwentyFourGenerator.findSolution(listOf(1, 1, 1, 1)))
        assertNotNull(TwentyFourGenerator.findSolution(listOf(3, 5, 1, 3)))
    }

    @Test
    fun divisionOnlyAcceptsIntegerResults() {
        assertNull(ArithmeticOperation.DIVIDE.apply(5, 2))
        assertNull(ArithmeticOperation.DIVIDE.apply(5, 0))
        assertEquals(2, ArithmeticOperation.DIVIDE.apply(8, 4))
    }

    @Test
    fun combiningRemovesTheSourceAndKeepsTheResultAtTheTarget() {
        val round = TwentyFourRound(listOf(3, 5, 1, 3))

        assertEquals(TwentyFourMoveStatus.APPLIED, round.combine(0, 1, ArithmeticOperation.ADD).status)
        assertEquals(listOf(null, 8, 1, 3), round.values)
        assertEquals(TwentyFourMoveStatus.APPLIED, round.combine(1, 3, ArithmeticOperation.MULTIPLY).status)
        assertEquals(listOf(null, null, 1, 24), round.values)
        assertEquals(TwentyFourMoveStatus.SOLVED, round.combine(2, 3, ArithmeticOperation.MULTIPLY).status)
        assertEquals(listOf(null, null, null, 24), round.values)
        assertTrue(round.isSolved)

        round.reset()
        assertEquals(listOf(3, 5, 1, 3), round.values)
        assertFalse(round.isSolved)
    }

    @Test
    fun invalidMoveDoesNotChangeTheRound() {
        val round = TwentyFourRound(listOf(5, 2, 3, 4))

        assertEquals(TwentyFourMoveStatus.INVALID_OPERATION, round.combine(0, 1, ArithmeticOperation.DIVIDE).status)
        assertEquals(listOf(5, 2, 3, 4), round.values)
    }
}
