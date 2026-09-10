package com.kanayama.sudokuassistant

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import com.kanayama.sudokuassistant.data.ProgressRepository
import com.kanayama.sudokuassistant.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameProgressTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val repository get() = ProgressRepository(context)

    @Before fun clearTestStorage() {
        context.getSharedPreferences("sudoku_progress", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun view() = SudokuGameView(context) {}.apply { layout(0, 0, 1920, 1080) }
    private fun SudokuGameView.key(code: Int) { assertTrue(handleKey(code)) }
    private fun SudokuGameView.tap(x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(now, now + action * 30L, action, x, y, 0)
            onTouchEvent(event)
            event.recycle()
        }
    }
    private fun SudokuGameView.startTwentyFour() {
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
    }

    @Test fun sudokuSurvivesHomeOtherModeAndNewViewWithoutCountingTimeAway() {
        val game = view()
        game.key(KeyEvent.KEYCODE_DPAD_CENTER)
        game.key(KeyEvent.KEYCODE_MENU)
        game.key(KeyEvent.KEYCODE_MENU)
        game.key(KeyEvent.KEYCODE_DPAD_CENTER)
        ShadowSystemClock.advanceBy(Duration.ofMillis(2450))
        game.key(KeyEvent.KEYCODE_BACK)
        val saved = requireNotNull(repository.latest())
        assertTrue(saved.candidateMasks.any { it != 0 })
        assertTrue(saved.elapsedMillis >= 2450)
        game.startTwentyFour()
        game.key(KeyEvent.KEYCODE_BACK)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(80))
        val reopened = view()
        reopened.key(KeyEvent.KEYCODE_DPAD_CENTER)
        val restored = requireNotNull(repository.latest())
        assertArrayEquals(saved.puzzle.givens, restored.puzzle.givens)
        assertArrayEquals(saved.candidateMasks, restored.candidateMasks)
        assertEquals(saved.selectedCell, restored.selectedCell)
        assertEquals(saved.elapsedMillis, restored.elapsedMillis)
        reopened.key(KeyEvent.KEYCODE_DPAD_CENTER)
        reopened.key(KeyEvent.KEYCODE_DPAD_CENTER)
        reopened.key(KeyEvent.KEYCODE_BACK)
        val filled = requireNotNull(repository.latest())
        assertEquals(5, filled.entries[saved.selectedCell])
        assertEquals(0, filled.candidateMasks[saved.selectedCell])
        reopened.key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertArrayEquals(filled.entries, requireNotNull(repository.latest()).entries)
    }

    @Test fun sudokuPauseIsIdempotentAndCompletionRemovesOnlyItsOwnSave() {
        val puzzle = SudokuGenerator.generate(BoardSize.FOUR, Difficulty.EASY, kotlin.random.Random(4))
        val empty = puzzle.givens.indexOfFirst { it == 0 }
        val entries = puzzle.solution.copyOf().apply { this[empty] = 0 }
        repository.save(SudokuProgress(puzzle, entries, IntArray(16), empty, 1800))
        val game = view()
        game.key(KeyEvent.KEYCODE_DPAD_CENTER)
        ShadowSystemClock.advanceBy(Duration.ofMillis(400))
        game.pauseGame()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20))
        game.pauseGame()
        assertEquals(2200, requireNotNull(repository.latest()).elapsedMillis)
        game.resumeGame()
        game.key(KeyEvent.KEYCODE_DPAD_CENTER)
        val value = puzzle.solution[empty]
        // Four-cell picker uses two columns, centered at these design coordinates.
        val picker = SudokuGameView::class.java.getDeclaredField("pickerSelection").apply { isAccessible = true }
        picker.setInt(game, value - 1)
        game.key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertNull(repository.load(BoardSize.FOUR, Difficulty.EASY))
        game.key(KeyEvent.KEYCODE_BACK)
        assertNull(repository.load(BoardSize.FOUR, Difficulty.EASY))
    }

    @Test fun twentyFourKeepsResultSelectedAndRestoresPendingOperation() {
        repository.saveTwentyFour(TwentyFourProgress(TwentyFourRound(listOf(1, 2, 3, 4)), 0, null, null, "先选择一个数字"))
        val game = view()
        game.startTwentyFour()
        game.tap(710f, 245f)
        game.tap(535f, 745f)
        game.tap(1210f, 245f)
        val combined = requireNotNull(repository.loadTwentyFour())
        assertEquals(listOf(null, 3, 3, 4), combined.round.values)
        assertEquals(1, combined.source)
        assertEquals(1, combined.focus)
        game.tap(710f, 485f)
        assertEquals(2, requireNotNull(repository.loadTwentyFour()).source)
        game.tap(1095f, 745f)
        val pending = requireNotNull(repository.loadTwentyFour())
        game.key(KeyEvent.KEYCODE_BACK)
        val reopened = view()
        reopened.startTwentyFour()
        val restored = requireNotNull(repository.loadTwentyFour())
        assertEquals(pending.encode(), restored.encode())
        reopened.tap(1210f, 485f)
        assertEquals(listOf(null, 3, null, 12), requireNotNull(repository.loadTwentyFour()).round.values)
        reopened.tap(710f, 245f) // A disappeared number must not steal focus or selection.
        assertEquals(3, requireNotNull(repository.loadTwentyFour()).focus)
        reopened.key(KeyEvent.KEYCODE_MENU)
        reopened.key(KeyEvent.KEYCODE_BACK)
        reopened.startTwentyFour()
        assertTrue(requireNotNull(repository.loadTwentyFour()).message.startsWith("原题最后一步提示"))
    }

    @Test fun twentyFourSolvedSaveIsClearedButFailedResultRemainsResettable() {
        val round = TwentyFourRound(listOf(1, 2, 3, 4))
        round.combine(0, 1, ArithmeticOperation.MULTIPLY)
        round.combine(1, 2, ArithmeticOperation.MULTIPLY)
        round.combine(2, 3, ArithmeticOperation.MULTIPLY)
        repository.saveTwentyFour(TwentyFourProgress(round, 3, null, null, "完成"))
        assertNull(repository.loadTwentyFour())
        round.reset()
        round.combine(0, 1, ArithmeticOperation.ADD)
        round.combine(1, 2, ArithmeticOperation.ADD)
        round.combine(2, 3, ArithmeticOperation.ADD)
        repository.saveTwentyFour(TwentyFourProgress(round, 3, null, null, "请重置"))
        val restored = requireNotNull(repository.loadTwentyFour()).round
        assertEquals(listOf(null, null, null, 10), restored.values)
        restored.reset()
        assertEquals(listOf(1, 2, 3, 4), restored.values)
        assertTrue(restored.history.isEmpty())
    }
}
