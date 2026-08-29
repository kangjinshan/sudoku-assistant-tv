package com.kanayama.sudokuassistant.model

enum class BoardSize(
    val side: Int,
    val blockRows: Int,
    val blockColumns: Int,
    val label: String,
    val defaultPickerValue: Int,
) {
    FOUR(4, 2, 2, "四宫", 2),
    SIX(6, 2, 3, "六宫", 2),
    NINE(9, 3, 3, "九宫", 5),
}

enum class Difficulty(val label: String) {
    EASY("简单"), MEDIUM("中等"), HARD("困难")
}

data class Puzzle(
    val size: BoardSize,
    val difficulty: Difficulty,
    val solution: IntArray,
    val givens: IntArray,
) {
    init {
        require(solution.size == size.side * size.side)
        require(givens.size == solution.size)
    }

    fun isGiven(index: Int): Boolean = givens[index] != 0

    fun isValidCompletion(values: IntArray): Boolean =
        values.size == givens.size &&
            values.indices.all { index -> givens[index] == 0 || values[index] == givens[index] } &&
            SudokuGenerator.isValidSolution(values, size)
}

object SudokuGenerator {
    private val clueCounts = mapOf(
        BoardSize.FOUR to mapOf(Difficulty.EASY to 12, Difficulty.MEDIUM to 10, Difficulty.HARD to 8),
        BoardSize.SIX to mapOf(Difficulty.EASY to 27, Difficulty.MEDIUM to 22, Difficulty.HARD to 18),
        BoardSize.NINE to mapOf(Difficulty.EASY to 54, Difficulty.MEDIUM to 43, Difficulty.HARD to 32),
    )

    fun clueCount(size: BoardSize, difficulty: Difficulty): Int =
        requireNotNull(clueCounts[size]?.get(difficulty))

    fun generate(
        size: BoardSize,
        difficulty: Difficulty,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): Puzzle {
        repeat(20) {
            val solution = generateSolution(size, random)
            if (isValidSolution(solution, size)) {
                val givens = solution.copyOf()
                val removeCount = givens.size - clueCount(size, difficulty)
                givens.indices.shuffled(random).take(removeCount).forEach { givens[it] = 0 }
                if (hasSolution(givens, size)) return Puzzle(size, difficulty, solution, givens)
            }
        }
        error("无法生成有效数独")
    }

    internal fun generateSolution(size: BoardSize, random: kotlin.random.Random): IntArray {
        val side = size.side
        val symbols = (1..side).shuffled(random)
        val rowBands = (0 until side / size.blockRows).shuffled(random)
        val columnStacks = (0 until side / size.blockColumns).shuffled(random)
        val rows = rowBands.flatMap { band ->
            (0 until size.blockRows).shuffled(random).map { band * size.blockRows + it }
        }
        val columns = columnStacks.flatMap { stack ->
            (0 until size.blockColumns).shuffled(random).map { stack * size.blockColumns + it }
        }
        return IntArray(side * side) { index ->
            val row = rows[index / side]
            val column = columns[index % side]
            val pattern = (column + size.blockColumns * (row % size.blockRows) + row / size.blockRows) % side
            symbols[pattern]
        }
    }

    fun isValidSolution(values: IntArray, size: BoardSize): Boolean {
        val side = size.side
        if (values.size != side * side || values.any { it !in 1..side }) return false
        val expected = (1..side).toSet()
        for (row in 0 until side) {
            if ((0 until side).map { values[row * side + it] }.toSet() != expected) return false
        }
        for (column in 0 until side) {
            if ((0 until side).map { values[it * side + column] }.toSet() != expected) return false
        }
        for (blockRow in 0 until side step size.blockRows) {
            for (blockColumn in 0 until side step size.blockColumns) {
                val block = buildSet {
                    for (row in blockRow until blockRow + size.blockRows) {
                        for (column in blockColumn until blockColumn + size.blockColumns) {
                            add(values[row * side + column])
                        }
                    }
                }
                if (block != expected) return false
            }
        }
        return true
    }

    fun conflictingCells(values: IntArray, size: BoardSize): Set<Int> {
        val side = size.side
        if (values.size != side * side) return values.indices.toSet()
        val conflicts = mutableSetOf<Int>()

        values.forEachIndexed { index, value ->
            if (value !in 1..side) conflicts += index
        }

        fun collectDuplicates(indices: Iterable<Int>) {
            indices
                .filter { values[it] in 1..side }
                .groupBy { values[it] }
                .values
                .filter { it.size > 1 }
                .forEach { conflicts.addAll(it) }
        }

        for (row in 0 until side) {
            collectDuplicates((0 until side).map { column -> row * side + column })
        }
        for (column in 0 until side) {
            collectDuplicates((0 until side).map { row -> row * side + column })
        }
        for (blockRow in 0 until side step size.blockRows) {
            for (blockColumn in 0 until side step size.blockColumns) {
                collectDuplicates(buildList {
                    for (row in blockRow until blockRow + size.blockRows) {
                        for (column in blockColumn until blockColumn + size.blockColumns) {
                            add(row * side + column)
                        }
                    }
                })
            }
        }
        return conflicts
    }

    fun hasSolution(givens: IntArray, size: BoardSize): Boolean {
        val board = givens.copyOf()
        val side = size.side
        fun solve(): Boolean {
            val empty = board.indexOfFirst { it == 0 }
            if (empty < 0) return isValidSolution(board, size)
            val row = empty / side
            val column = empty % side
            for (value in 1..side) {
                if (canPlace(board, size, row, column, value)) {
                    board[empty] = value
                    if (solve()) return true
                    board[empty] = 0
                }
            }
            return false
        }
        return solve()
    }

    private fun canPlace(board: IntArray, size: BoardSize, row: Int, column: Int, value: Int): Boolean {
        val side = size.side
        if ((0 until side).any { board[row * side + it] == value }) return false
        if ((0 until side).any { board[it * side + column] == value }) return false
        val startRow = row / size.blockRows * size.blockRows
        val startColumn = column / size.blockColumns * size.blockColumns
        for (r in startRow until startRow + size.blockRows) {
            for (c in startColumn until startColumn + size.blockColumns) {
                if (board[r * side + c] == value) return false
            }
        }
        return true
    }
}
