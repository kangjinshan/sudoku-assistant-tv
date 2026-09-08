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
            assertEquals(24, puzzle.finalStep.operation.apply(puzzle.finalStep.left, puzzle.finalStep.right))
            assertEquals(puzzle.finalStep, TwentyFourGenerator.findFinalStep(puzzle.numbers))
            assertSolutionMatchesHint(puzzle)
        }
    }

    @Test
    fun solverRejectsPuzzleWithoutAPathToTwentyFour() {
        assertNull(TwentyFourGenerator.findSolution(listOf(1, 1, 1, 1)))
        assertNull(TwentyFourGenerator.findFinalStep(listOf(1, 1, 1, 1)))
        assertNull(TwentyFourGenerator.findFinalStep(listOf(2, 3, 8)))
        assertNull(TwentyFourGenerator.findFinalStep(listOf(0, 2, 3, 8)))
        assertNotNull(TwentyFourGenerator.findSolution(listOf(3, 5, 1, 3)))
    }

    @Test
    fun finalStepHintOnlyRevealsTheLastOperation() {
        val step = requireNotNull(TwentyFourGenerator.findFinalStep(listOf(2, 1, 3, 8)))
        assertEquals("1 × 24", step.expression)
        assertEquals(24, step.operation.apply(step.left, step.right))
        assertEquals("3 × 8", TwentyFourFinalStep(3, ArithmeticOperation.MULTIPLY, 8).expression)
        assertEquals("(-3) × (-8)", TwentyFourFinalStep(-3, ArithmeticOperation.MULTIPLY, -8).expression)
        assertEquals("30 − 6", TwentyFourFinalStep(30, ArithmeticOperation.SUBTRACT, 6).expression)
        assertEquals("48 ÷ 2", TwentyFourFinalStep(48, ArithmeticOperation.DIVIDE, 2).expression)
    }

    // Independently evaluate the saved expression to verify every input is used and
    // the hint is the actual root operation of a valid integer-only solution.
    private fun assertSolutionMatchesHint(puzzle: TwentyFourPuzzle) {
        val tokens = Regex("[0-9]+|[()＋−×÷]").findAll(puzzle.solutionExpression).map { it.value }.toList()
        val leaves = mutableListOf<Int>()
        var position = 0
        var lastStep: TwentyFourFinalStep? = null
        fun evaluate(): Int {
            val token = tokens[position++]
            if (token != "(") return token.toInt().also { leaves.add(it) }
            val left = evaluate()
            val symbol = tokens[position++]
            val operation = ArithmeticOperation.entries.single { it.symbol == symbol }
            val right = evaluate()
            assertEquals(")", tokens[position++])
            lastStep = TwentyFourFinalStep(left, operation, right)
            return when (operation) {
                ArithmeticOperation.ADD -> left + right
                ArithmeticOperation.SUBTRACT -> left - right
                ArithmeticOperation.MULTIPLY -> left * right
                ArithmeticOperation.DIVIDE -> {
                    assertTrue(right != 0)
                    assertEquals(0, left % right)
                    left / right
                }
            }
        }
        assertEquals(24, evaluate())
        assertEquals(tokens.size, position)
        assertEquals(puzzle.numbers.sorted(), leaves.sorted())
        assertEquals(puzzle.finalStep, lastStep)
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
