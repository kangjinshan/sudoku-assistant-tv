package com.kanayama.sudokuassistant.data

import android.content.Context
import com.kanayama.sudokuassistant.model.BoardSize
import com.kanayama.sudokuassistant.model.Difficulty

class ScoreRepository(context: Context) {
    private val preferences = context.getSharedPreferences("sudoku_scores", Context.MODE_PRIVATE)

    fun scores(size: BoardSize, difficulty: Difficulty): List<Long> =
        preferences.getString(key(size, difficulty), null)
            ?.split(',')
            ?.mapNotNull(String::toLongOrNull)
            ?.sorted()
            ?.take(MAX_SCORES)
            .orEmpty()

    fun record(size: BoardSize, difficulty: Difficulty, elapsedSeconds: Long): Boolean {
        val oldScores = scores(size, difficulty)
        val updated = (oldScores + elapsedSeconds).sorted().take(MAX_SCORES)
        preferences.edit().putString(key(size, difficulty), updated.joinToString(",")).apply()
        return oldScores.isEmpty() || elapsedSeconds < oldScores.first()
    }

    private fun key(size: BoardSize, difficulty: Difficulty) = "${size.name}_${difficulty.name}"

    private companion object { const val MAX_SCORES = 10 }
}
