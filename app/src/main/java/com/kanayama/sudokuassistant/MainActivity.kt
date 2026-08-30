package com.kanayama.sudokuassistant

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.view.KeyEvent
import android.window.OnBackInvokedDispatcher

class MainActivity : Activity() {
    private lateinit var gameView: SudokuGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = SudokuGameView(this) { finishAffinity() }
        setContentView(gameView)
        gameView.requestFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (!gameView.handleKey(KeyEvent.KEYCODE_BACK)) finish()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && gameView.handleKey(event.keyCode)) return true
        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (!gameView.handleKey(KeyEvent.KEYCODE_BACK)) super.onBackPressed()
    }
}
