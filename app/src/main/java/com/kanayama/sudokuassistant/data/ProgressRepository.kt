package com.kanayama.sudokuassistant.data

import android.content.Context
import com.kanayama.sudokuassistant.model.BoardSize
import com.kanayama.sudokuassistant.model.Difficulty
import com.kanayama.sudokuassistant.model.SudokuProgress
import com.kanayama.sudokuassistant.model.TwentyFourProgress

class ProgressRepository(context: Context) {
    private val preferences = context.getSharedPreferences("sudoku_progress", Context.MODE_PRIVATE)

    fun load(size: BoardSize, difficulty: Difficulty): SudokuProgress? =
        read(key(size, difficulty))?.takeIf { it.puzzle.size == size && it.puzzle.difficulty == difficulty }

    fun hasSudoku(size: BoardSize, difficulty: Difficulty): Boolean = preferences.contains(key(size, difficulty))

    fun latest(): SudokuProgress? = preferences.getString("latest", null)?.let(::read)

    fun loadTwentyFour(): TwentyFourProgress? =
        preferences.getString("twenty_four", null)?.let(TwentyFourProgress::decode)

    fun saveTwentyFour(progress: TwentyFourProgress) {
        val editor = preferences.edit()
        if (progress.round.isSolved) editor.remove("twenty_four")
        else editor.putString("twenty_four", progress.encode())
        editor.apply()
    }

    fun save(progress: SudokuProgress) {
        val key = key(progress.puzzle.size, progress.puzzle.difficulty)
        preferences.edit().putString(key, progress.encode()).putString("latest", key).apply()
    }

    fun clear(size: BoardSize, difficulty: Difficulty) {
        val key = key(size, difficulty)
        val editor = preferences.edit().remove(key)
        if (preferences.getString("latest", null) == key) editor.remove("latest")
        editor.apply()
    }

    private fun read(key: String): SudokuProgress? =
        preferences.getString(key, null)?.let(SudokuProgress::decode)

    private fun key(size: BoardSize, difficulty: Difficulty) = "${size.name}_${difficulty.name}"
}
