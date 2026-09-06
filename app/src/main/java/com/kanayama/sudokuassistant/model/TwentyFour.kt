package com.kanayama.sudokuassistant.model

import kotlin.random.Random

enum class ArithmeticOperation(val symbol: String) {
    ADD("＋"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷");

    fun apply(left: Int, right: Int): Int? {
        val result = when (this) {
            ADD -> left.toLong() + right
            SUBTRACT -> left.toLong() - right
            MULTIPLY -> left.toLong() * right
            DIVIDE -> {
                if (right == 0 || left % right != 0) return null
                left.toLong() / right
            }
        }
        return result.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
    }
}

data class TwentyFourPuzzle(
    val numbers: List<Int>,
    val solutionExpression: String,
) {
    init {
        require(numbers.size == 4)
        require(numbers.all { it in 1..10 })
    }
}

enum class TwentyFourMoveStatus {
    APPLIED,
    SOLVED,
    NOT_TWENTY_FOUR,
    INVALID_SELECTION,
    INVALID_OPERATION,
}

data class TwentyFourMove(
    val status: TwentyFourMoveStatus,
    val result: Int? = null,
)

class TwentyFourRound(initialNumbers: List<Int>) {
    val initialNumbers = initialNumbers.toList()
    private val currentValues = arrayOfNulls<Int>(4)

    init {
        require(initialNumbers.size == 4)
        require(initialNumbers.all { it in 1..10 })
        reset()
    }

    val values: List<Int?>
        get() = currentValues.toList()

    val remainingCount: Int
        get() = currentValues.count { it != null }

    val isSolved: Boolean
        get() = remainingCount == 1 && currentValues.singleOrNull { it != null } == 24

    fun reset() {
        initialNumbers.forEachIndexed { index, value -> currentValues[index] = value }
    }

    fun combine(sourceIndex: Int, targetIndex: Int, operation: ArithmeticOperation): TwentyFourMove {
        if (sourceIndex !in currentValues.indices || targetIndex !in currentValues.indices || sourceIndex == targetIndex) {
            return TwentyFourMove(TwentyFourMoveStatus.INVALID_SELECTION)
        }
        val left = currentValues[sourceIndex] ?: return TwentyFourMove(TwentyFourMoveStatus.INVALID_SELECTION)
        val right = currentValues[targetIndex] ?: return TwentyFourMove(TwentyFourMoveStatus.INVALID_SELECTION)
        val result = operation.apply(left, right) ?: return TwentyFourMove(TwentyFourMoveStatus.INVALID_OPERATION)

        currentValues[sourceIndex] = null
        currentValues[targetIndex] = result
        val status = when {
            remainingCount > 1 -> TwentyFourMoveStatus.APPLIED
            result == 24 -> TwentyFourMoveStatus.SOLVED
            else -> TwentyFourMoveStatus.NOT_TWENTY_FOUR
        }
        return TwentyFourMove(status, result)
    }
}

object TwentyFourGenerator {
    private data class Term(val value: Int, val expression: String)

    fun generate(random: Random = Random.Default): TwentyFourPuzzle {
        repeat(1_000) {
            val numbers = List(4) { random.nextInt(1, 11) }
            val solution = findSolution(numbers)
            if (solution != null) return TwentyFourPuzzle(numbers, solution)
        }

        val fallback = listOf(3, 5, 1, 3)
        return TwentyFourPuzzle(fallback, requireNotNull(findSolution(fallback)))
    }

    fun findSolution(numbers: List<Int>): String? {
        if (numbers.size != 4 || numbers.any { it !in 1..10 }) return null
        val visited = mutableSetOf<String>()
        return solve(numbers.map { Term(it, it.toString()) }, visited)
    }

    private fun solve(terms: List<Term>, visited: MutableSet<String>): String? {
        if (terms.size == 1) return terms.single().expression.takeIf { terms.single().value == 24 }
        val state = terms.map { it.value }.sorted().joinToString(",")
        if (!visited.add(state)) return null

        for (firstIndex in 0 until terms.lastIndex) {
            for (secondIndex in firstIndex + 1 until terms.size) {
                val first = terms[firstIndex]
                val second = terms[secondIndex]
                val remaining = terms.filterIndexed { index, _ -> index != firstIndex && index != secondIndex }
                val candidates = buildList {
                    add(combine(first, second, ArithmeticOperation.ADD))
                    add(combine(first, second, ArithmeticOperation.MULTIPLY))
                    add(combine(first, second, ArithmeticOperation.SUBTRACT))
                    add(combine(second, first, ArithmeticOperation.SUBTRACT))
                    combine(first, second, ArithmeticOperation.DIVIDE)?.let(::add)
                    combine(second, first, ArithmeticOperation.DIVIDE)?.let(::add)
                }.filterNotNull().distinctBy { it.value }

                for (candidate in candidates) {
                    solve(remaining + candidate, visited)?.let { return it }
                }
            }
        }
        return null
    }

    private fun combine(first: Term, second: Term, operation: ArithmeticOperation): Term? {
        val result = operation.apply(first.value, second.value) ?: return null
        return Term(result, "(${first.expression} ${operation.symbol} ${second.expression})")
    }
}
