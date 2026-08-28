package com.kanayama.sudokuassistant

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent

class MainActivity : Activity() {
    private lateinit var gameView: SudokuGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = SudokuGameView(this) { finishAffinity() }
        setContentView(gameView)
        gameView.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && gameView.handleKey(event.keyCode)) return true
        return super.dispatchKeyEvent(event)
    }
}
