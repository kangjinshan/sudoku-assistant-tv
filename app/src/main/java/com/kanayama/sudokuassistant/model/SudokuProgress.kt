package com.kanayama.sudokuassistant.model

/** A versioned, device-local snapshot; elapsed time excludes time away from the game. */
data class SudokuProgress(
    val puzzle: Puzzle,
    val entries: IntArray,
    val candidateMasks: IntArray,
    val selectedCell: Int,
    val elapsedMillis: Long,
) {
    fun encode(): String = listOf(
        "1", puzzle.size.name, puzzle.difficulty.name,
        puzzle.solution.joinToString(","), puzzle.givens.joinToString(","),
        entries.joinToString(","), candidateMasks.joinToString(","),
        selectedCell.toString(), elapsedMillis.toString(),
    ).joinToString("|")

    companion object {
        fun decode(encoded: String): SudokuProgress? = runCatching {
            val fields = encoded.split('|')
            require(fields.size == 9 && fields[0] == "1")
            val size = BoardSize.valueOf(fields[1])
            val difficulty = Difficulty.valueOf(fields[2])
            fun numbers(index: Int) = fields[index].split(',').map { it.toInt() }.toIntArray()
            val puzzle = Puzzle(size, difficulty, numbers(3), numbers(4))
            val entries = numbers(5)
            val candidates = numbers(6)
            val selectedCell = fields[7].toInt()
            val elapsedMillis = fields[8].toLong()
            require(SudokuGenerator.isValidSolution(puzzle.solution, size))
            require(puzzle.givens.indices.all { puzzle.givens[it] == 0 || puzzle.givens[it] == puzzle.solution[it] })
            require(entries.size == puzzle.givens.size && candidates.size == entries.size)
            require(entries.all { it in 0..size.side })
            require(entries.indices.all { !puzzle.isGiven(it) || entries[it] == puzzle.givens[it] })
            require(candidates.indices.all {
                candidates[it] in 0 until (1 shl size.side) &&
                    Integer.bitCount(candidates[it]) <= 4 && (entries[it] == 0 || candidates[it] == 0)
            })
            require(selectedCell in entries.indices && elapsedMillis >= 0)
            require(!puzzle.isValidCompletion(entries))
            SudokuProgress(puzzle, entries, candidates, selectedCell, elapsedMillis)
        }.getOrNull()
    }
}
