package com.kanayama.sudokuassistant

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.kanayama.sudokuassistant.data.ScoreRepository
import com.kanayama.sudokuassistant.model.ArithmeticOperation
import com.kanayama.sudokuassistant.model.BoardSize
import com.kanayama.sudokuassistant.model.Difficulty
import com.kanayama.sudokuassistant.model.Puzzle
import com.kanayama.sudokuassistant.model.SudokuGenerator
import com.kanayama.sudokuassistant.model.TwentyFourGenerator
import com.kanayama.sudokuassistant.model.TwentyFourMoveStatus
import com.kanayama.sudokuassistant.model.TwentyFourPuzzle
import com.kanayama.sudokuassistant.model.TwentyFourRound
import kotlin.math.min

private enum class Page { HOME, GAME, TWENTY_FOUR, REWARD, SCORES }
private enum class PickerMode { VALUE, CANDIDATES }

private data class PickerLayout(
    val columns: Int,
    val rows: Int,
    val key: Float,
    val gap: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val gridLeft: Float,
    val gridTop: Float
)

class SudokuGameView(context: Context, private val exitApp: () -> Unit) : View(context) {
    private val ink = Color.rgb(8, 19, 29)
    private val panel = Color.rgb(16, 37, 53)
    private val panelLight = Color.rgb(24, 54, 74)
    private val mint = Color.rgb(86, 214, 201)
    private val cream = Color.rgb(244, 240, 232)
    private val muted = Color.rgb(157, 176, 188)
    private val coral = Color.rgb(255, 139, 123)
    private val gold = Color.rgb(255, 208, 106)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scores = ScoreRepository(context)

    private var page = Page.HOME
    private var boardSize = BoardSize.NINE
    private var difficulty = Difficulty.EASY
    private var homeFocus = 8
    private var scoreFocus = 6
    private var rewardFocus = 1
    private var exitOpen = false
    private var exitSelected = false
    private var pickerOpen = false
    private var pickerSelection = 4
    private var pickerMode = PickerMode.VALUE
    private var pickerDraftMask = 0
    private var pickerLimitReached = false
    private var puzzle: Puzzle? = null
    private var entries = IntArray(0)
    private var candidateMasks = IntArray(0)
    private var selectedCell = 0
    private var incorrectCells = emptySet<Int>()
    private var statusMessage: String? = null
    private var startedAt = 0L
    private var elapsedSeconds = 0L
    private var newRecord = false
    private var twentyFourPuzzle: TwentyFourPuzzle? = null
    private var twentyFourRound: TwentyFourRound? = null
    private var twentyFourFocus = 0
    private var twentyFourSource: Int? = null
    private var twentyFourOperation: ArithmeticOperation? = null
    private var twentyFourMessage = "先选择一个数字"

    private val touchDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean = true

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            performClick()
            val point = ViewportTransform.fit(width, height).toDesignPoint(event.x, event.y)
            if (handleTap(point.x, point.y)) invalidate()
            return true
        }

        override fun onLongPress(event: MotionEvent) {
            val point = ViewportTransform.fit(width, height).toDesignPoint(event.x, event.y)
            if (handleLongPress(point.x, point.y)) invalidate()
        }
    })

    private val ticker = object : Runnable {
        override fun run() {
            if (page == Page.GAME && startedAt > 0L) {
                elapsedSeconds = (SystemClock.elapsedRealtime() - startedAt) / 1000L
                invalidate()
            }
            postDelayed(this, 250L)
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(ink)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
        removeCallbacks(ticker)
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewport = ViewportTransform.fit(width, height)
        canvas.save()
        canvas.translate(viewport.offsetX, viewport.offsetY)
        canvas.scale(viewport.scale, viewport.scale)
        drawBackground(canvas)
        when (page) {
            Page.HOME -> drawHome(canvas)
            Page.GAME -> drawGame(canvas)
            Page.TWENTY_FOUR -> drawTwentyFour(canvas)
            Page.REWARD -> drawReward(canvas)
            Page.SCORES -> drawScores(canvas)
        }
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean =
        touchDetector.onTouchEvent(event) || super.onTouchEvent(event)

    override fun performClick(): Boolean = super.performClick()

    fun handleKey(keyCode: Int): Boolean {
        val handled = when (page) {
            Page.HOME -> handleHomeKey(keyCode)
            Page.GAME -> handleGameKey(keyCode)
            Page.TWENTY_FOUR -> handleTwentyFourKey(keyCode)
            Page.REWARD -> handleRewardKey(keyCode)
            Page.SCORES -> handleScoresKey(keyCode)
        }
        if (handled) invalidate()
        return handled
    }

    private fun handleTap(x: Float, y: Float): Boolean = when (page) {
        Page.HOME -> handleHomeTap(x, y)
        Page.GAME -> handleGameTap(x, y)
        Page.TWENTY_FOUR -> handleTwentyFourTap(x, y)
        Page.REWARD -> handleRewardTap(x, y)
        Page.SCORES -> handleScoresTap(x, y)
    }

    private fun handleLongPress(x: Float, y: Float): Boolean {
        if (page != Page.GAME || pickerOpen) return false
        val current = puzzle ?: return false
        val cellIndex = boardCellAt(x, y, current.size.side) ?: return false
        selectedCell = cellIndex
        if (!current.isGiven(cellIndex) && entries[cellIndex] == 0) {
            openCandidatePicker(current.size)
        }
        return true
    }

    private fun handleHomeTap(x: Float, y: Float): Boolean {
        if (exitOpen) {
            when {
                contains(x, y, 485f, 588f, 904f, 710f) -> {
                    exitSelected = false
                    exitOpen = false
                }
                contains(x, y, 936f, 588f, 1435f, 710f) -> {
                    exitSelected = true
                    exitApp()
                }
                else -> return false
            }
            return true
        }
        BoardSize.entries.forEachIndexed { index, size ->
            val left = 802f + index * 314f
            if (contains(x, y, left, 264f, left + 286f, 452f)) {
                homeFocus = index
                boardSize = size
                return true
            }
        }
        Difficulty.entries.forEachIndexed { index, item ->
            val left = 802f + index * 314f
            if (contains(x, y, left, 604f, left + 286f, 790f)) {
                homeFocus = index + 3
                difficulty = item
                return true
            }
        }
        return when {
            contains(x, y, 802f, 800f, 1076f, 920f) -> {
                homeFocus = 6
                activateHome()
                true
            }
            contains(x, y, 1092f, 800f, 1366f, 920f) -> {
                homeFocus = 7
                activateHome()
                true
            }
            contains(x, y, 1382f, 800f, 1716f, 920f) -> {
                homeFocus = 8
                activateHome()
                true
            }
            else -> false
        }
    }

    private fun handleGameTap(x: Float, y: Float): Boolean {
        val current = puzzle ?: return false
        if (pickerOpen) return handlePickerTap(x, y, current.size)
        val cellIndex = boardCellAt(x, y, current.size.side) ?: return false
        selectedCell = cellIndex
        if (!current.isGiven(cellIndex)) openValuePicker(current.size)
        return true
    }

    private fun handlePickerTap(x: Float, y: Float, size: BoardSize): Boolean {
        val layout = pickerLayout(size)
        repeat(size.side) { index ->
            val column = index % layout.columns
            val row = index / layout.columns
            val left = layout.gridLeft + column * (layout.key + layout.gap)
            val top = layout.gridTop + row * (layout.key + layout.gap)
            if (contains(x, y, left, top, left + layout.key, top + layout.key)) {
                pickerSelection = index
                pickerLimitReached = false
                if (pickerMode == PickerMode.VALUE) {
                    pickerOpen = false
                    enterValue(index + 1)
                } else {
                    togglePickerCandidate()
                }
                return true
            }
        }
        if (pickerMode == PickerMode.CANDIDATES) {
            val buttonTop = layout.bottom - 92f
            val buttonMiddle = (layout.left + layout.right) / 2f
            when {
                contains(x, y, layout.left + 34f, buttonTop, buttonMiddle - 8f, layout.bottom - 24f) -> {
                    pickerOpen = false
                    pickerLimitReached = false
                    return true
                }
                contains(x, y, buttonMiddle + 8f, buttonTop, layout.right - 34f, layout.bottom - 24f) -> {
                    candidateMasks[selectedCell] = pickerDraftMask
                    pickerOpen = false
                    pickerLimitReached = false
                    return true
                }
            }
        }
        if (!contains(x, y, layout.left, layout.top, layout.right, layout.bottom)) {
            pickerOpen = false
            pickerLimitReached = false
            return true
        }
        return false
    }

    private fun handleTwentyFourTap(x: Float, y: Float): Boolean {
        repeat(4) { index ->
            if (twentyFourNumberRect(index).contains(x, y)) {
                twentyFourFocus = index
                selectTwentyFourNumber(index)
                return true
            }
        }
        ArithmeticOperation.entries.forEachIndexed { index, operation ->
            if (twentyFourOperationRect(index).contains(x, y)) {
                twentyFourFocus = index + 4
                selectTwentyFourOperation(operation)
                return true
            }
        }
        repeat(3) { index ->
            if (twentyFourUtilityRect(index).contains(x, y)) {
                twentyFourFocus = index + 8
                activateTwentyFourFocus()
                return true
            }
        }
        return false
    }

    private fun startTwentyFourGame() {
        val generated = TwentyFourGenerator.generate()
        twentyFourPuzzle = generated
        twentyFourRound = TwentyFourRound(generated.numbers)
        twentyFourFocus = 0
        twentyFourSource = null
        twentyFourOperation = null
        twentyFourMessage = "先选择一个数字"
        page = Page.TWENTY_FOUR
    }

    private fun resetTwentyFourGame() {
        twentyFourRound?.reset()
        twentyFourFocus = 0
        twentyFourSource = null
        twentyFourOperation = null
        twentyFourMessage = "已恢复原题，请重新尝试"
    }

    private fun showTwentyFourHint() {
        val puzzle = twentyFourPuzzle ?: return
        twentyFourMessage = "原题最后一步提示：${puzzle.finalStep.expression}"
    }

    private fun handleTwentyFourKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> moveTwentyFourFocus(keyCode)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> activateTwentyFourFocus()
            KeyEvent.KEYCODE_MENU -> showTwentyFourHint()
            KeyEvent.KEYCODE_BACK -> showHome()
            else -> return false
        }
        return true
    }

    private fun moveTwentyFourFocus(keyCode: Int) {
        val candidates = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> when (twentyFourFocus) {
                1 -> intArrayOf(0, 1)
                3 -> intArrayOf(2, 3)
                5 -> intArrayOf(4)
                6 -> intArrayOf(5)
                7 -> intArrayOf(6)
                9 -> intArrayOf(8)
                10 -> intArrayOf(9)
                else -> intArrayOf(twentyFourFocus)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> when (twentyFourFocus) {
                0 -> intArrayOf(1, 0)
                2 -> intArrayOf(3, 2)
                4 -> intArrayOf(5)
                5 -> intArrayOf(6)
                6 -> intArrayOf(7)
                8 -> intArrayOf(9)
                9 -> intArrayOf(10)
                else -> intArrayOf(twentyFourFocus)
            }
            KeyEvent.KEYCODE_DPAD_UP -> when (twentyFourFocus) {
                2 -> intArrayOf(0, 1, 2)
                3 -> intArrayOf(1, 0, 3)
                4, 5 -> intArrayOf(2, 3, 0, 1, twentyFourFocus)
                6, 7 -> intArrayOf(3, 2, 1, 0, twentyFourFocus)
                8 -> intArrayOf(4)
                9 -> intArrayOf(5)
                10 -> intArrayOf(7)
                else -> intArrayOf(twentyFourFocus)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> when (twentyFourFocus) {
                0 -> intArrayOf(2, 3, 4)
                1 -> intArrayOf(3, 2, 6)
                2 -> intArrayOf(4)
                3 -> intArrayOf(6)
                4 -> intArrayOf(8)
                5, 6 -> intArrayOf(9)
                7 -> intArrayOf(10)
                else -> intArrayOf(twentyFourFocus)
            }
            else -> intArrayOf(twentyFourFocus)
        }
        val values = twentyFourRound?.values.orEmpty()
        candidates.firstOrNull { candidate ->
            candidate !in 0..3 ||
                (values.getOrNull(candidate) != null && (twentyFourOperation == null || candidate != twentyFourSource))
        }
            ?.let { twentyFourFocus = it }
    }

    private fun activateTwentyFourFocus() {
        when (twentyFourFocus) {
            in 0..3 -> selectTwentyFourNumber(twentyFourFocus)
            in 4..7 -> selectTwentyFourOperation(ArithmeticOperation.entries[twentyFourFocus - 4])
            8 -> resetTwentyFourGame()
            9 -> startTwentyFourGame()
            10 -> showHome()
        }
    }

    private fun selectTwentyFourNumber(index: Int) {
        val round = twentyFourRound ?: return
        val value = round.values.getOrNull(index) ?: return
        if (round.remainingCount == 1) {
            twentyFourMessage = if (round.isSolved) "太棒了，正好 24！" else "结果是 $value，按“重置”再试一次"
            return
        }

        val sourceIndex = twentyFourSource
        val operation = twentyFourOperation
        if (sourceIndex == null || operation == null) {
            twentyFourSource = index
            twentyFourOperation = null
            twentyFourMessage = "已选择 $value，请选择运算符"
            return
        }
        if (sourceIndex == index) {
            twentyFourMessage = "请选择另一个数字"
            return
        }

        val move = round.combine(sourceIndex, index, operation)
        when (move.status) {
            TwentyFourMoveStatus.APPLIED -> twentyFourMessage = "得到 ${move.result}，继续计算"
            TwentyFourMoveStatus.SOLVED -> twentyFourMessage = "太棒了，正好 24！"
            TwentyFourMoveStatus.NOT_TWENTY_FOUR -> twentyFourMessage = "结果是 ${move.result}，按“重置”再试一次"
            TwentyFourMoveStatus.INVALID_OPERATION -> {
                twentyFourMessage = if (operation == ArithmeticOperation.DIVIDE) {
                    "除法必须整除，换一种算法试试"
                } else {
                    "这一步无法计算，请换一种算法"
                }
                return
            }
            TwentyFourMoveStatus.INVALID_SELECTION -> {
                twentyFourMessage = "请选择两个不同的数字"
                return
            }
        }
        twentyFourSource = null
        twentyFourOperation = null
        twentyFourFocus = index
    }

    private fun selectTwentyFourOperation(operation: ArithmeticOperation) {
        val sourceIndex = twentyFourSource
        val sourceValue = sourceIndex?.let { twentyFourRound?.values?.getOrNull(it) }
        if (sourceValue == null) {
            twentyFourMessage = "请先选择一个数字"
            return
        }
        twentyFourOperation = operation
        twentyFourMessage = "$sourceValue ${operation.symbol} … 请选择第二个数字"
    }

    private fun twentyFourNumberRect(index: Int): RectF {
        val left = if (index % 2 == 0) 520f else 1020f
        val top = if (index < 2) 150f else 390f
        return RectF(left, top, left + 380f, top + 190f)
    }

    private fun twentyFourOperationRect(index: Int): RectF {
        val left = 410f + index * 280f
        return RectF(left, 680f, left + 250f, 810f)
    }

    private fun twentyFourUtilityRect(index: Int): RectF {
        val left = 660f + index * 270f
        return RectF(left, 920f, left + 240f, 1005f)
    }

    private fun handleRewardTap(x: Float, y: Float): Boolean = when {
        contains(x, y, 650f, 680f, 925f, 800f) -> {
            rewardFocus = 0
            page = Page.SCORES
            true
        }
        contains(x, y, 955f, 680f, 1290f, 800f) -> {
            rewardFocus = 1
            showHome()
            true
        }
        else -> false
    }

    private fun handleScoresTap(x: Float, y: Float): Boolean {
        repeat(6) { index ->
            val left = 64f + index * 132f
            if (contains(x, y, left, 190f, left + 116f, 246f)) {
                scoreFocus = index
                if (index < 3) boardSize = BoardSize.entries[index]
                else difficulty = Difficulty.entries[index - 3]
                return true
            }
        }
        if (contains(x, y, 1635f, 65f, 1855f, 130f)) {
            scoreFocus = 6
            showHome()
            return true
        }
        return false
    }

    private fun boardCellAt(x: Float, y: Float, side: Int): Int? {
        val left = 96f
        val top = 48f
        val boardPixels = 984f
        if (!contains(x, y, left, top, left + boardPixels, top + boardPixels)) return null
        val cell = boardPixels / side
        val column = ((x - left) / cell).toInt().coerceIn(0, side - 1)
        val row = ((y - top) / cell).toInt().coerceIn(0, side - 1)
        return row * side + column
    }

    private fun contains(x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean =
        x >= left && x < right && y >= top && y < bottom

    private fun handleHomeKey(keyCode: Int): Boolean {
        if (exitOpen) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> exitSelected = false
                KeyEvent.KEYCODE_DPAD_RIGHT -> exitSelected = true
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (exitSelected) exitApp() else exitOpen = false
                }
                KeyEvent.KEYCODE_BACK -> exitOpen = false
                else -> return true
            }
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> homeFocus = when (homeFocus) {
                1, 2, 4, 5, 7, 8 -> homeFocus - 1
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> homeFocus = when (homeFocus) {
                0, 1, 3, 4, 6, 7 -> homeFocus + 1
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_UP -> homeFocus = when (homeFocus) {
                in 3..5 -> homeFocus - 3
                6 -> 3
                7 -> 4
                8 -> 5
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> homeFocus = when (homeFocus) {
                in 0..2 -> homeFocus + 3
                3 -> 6
                4 -> 7
                5 -> 8
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> activateHome()
            KeyEvent.KEYCODE_BACK -> { exitSelected = false; exitOpen = true }
            else -> return false
        }
        return true
    }

    private fun activateHome() {
        when (homeFocus) {
            0 -> boardSize = BoardSize.FOUR
            1 -> boardSize = BoardSize.SIX
            2 -> boardSize = BoardSize.NINE
            3 -> difficulty = Difficulty.EASY
            4 -> difficulty = Difficulty.MEDIUM
            5 -> difficulty = Difficulty.HARD
            6 -> { scoreFocus = 6; page = Page.SCORES }
            7 -> startTwentyFourGame()
            8 -> startGame()
        }
    }

    private fun startGame() {
        val generated = SudokuGenerator.generate(boardSize, difficulty)
        puzzle = generated
        entries = generated.givens.copyOf()
        candidateMasks = IntArray(entries.size)
        selectedCell = generated.givens.indexOfFirst { it == 0 }.coerceAtLeast(0)
        incorrectCells = emptySet()
        statusMessage = null
        pickerOpen = false
        elapsedSeconds = 0L
        startedAt = SystemClock.elapsedRealtime()
        page = Page.GAME
    }

    private fun handleGameKey(keyCode: Int): Boolean {
        val current = puzzle ?: return false
        if (pickerOpen) {
            val columns = if (current.size == BoardSize.FOUR) 2 else 3
            val row = pickerSelection / columns
            val column = pickerSelection % columns
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> if (column > 0) {
                    pickerSelection--
                    pickerLimitReached = false
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (column < columns - 1 && pickerSelection + 1 < current.size.side) {
                    pickerSelection++
                    pickerLimitReached = false
                }
                KeyEvent.KEYCODE_DPAD_UP -> if (row > 0) {
                    pickerSelection -= columns
                    pickerLimitReached = false
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> if (pickerSelection + columns < current.size.side) {
                    pickerSelection += columns
                    pickerLimitReached = false
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    pickerOpen = false
                    if (pickerMode == PickerMode.VALUE) {
                        enterValue(pickerSelection + 1)
                    } else {
                        candidateMasks[selectedCell] = pickerDraftMask
                    }
                }
                KeyEvent.KEYCODE_MENU -> {
                    if (pickerMode == PickerMode.CANDIDATES) togglePickerCandidate()
                }
                KeyEvent.KEYCODE_BACK -> {
                    pickerOpen = false
                    pickerLimitReached = false
                }
                else -> return false
            }
            return true
        }
        val side = current.size.side
        val row = selectedCell / side
        val column = selectedCell % side
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> selectedCell = (row - 1).coerceAtLeast(0) * side + column
            KeyEvent.KEYCODE_DPAD_DOWN -> selectedCell = (row + 1).coerceAtMost(side - 1) * side + column
            KeyEvent.KEYCODE_DPAD_LEFT -> selectedCell = row * side + (column - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> selectedCell = row * side + (column + 1).coerceAtMost(side - 1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (!current.isGiven(selectedCell)) {
                    openValuePicker(current.size)
                }
            }
            KeyEvent.KEYCODE_MENU -> {
                if (!current.isGiven(selectedCell) && entries[selectedCell] == 0) {
                    openCandidatePicker(current.size)
                }
            }
            KeyEvent.KEYCODE_BACK -> showHome()
            else -> return false
        }
        return true
    }

    private fun enterValue(value: Int) {
        val current = puzzle ?: return
        if (current.isGiven(selectedCell)) return
        entries[selectedCell] = value
        candidateMasks[selectedCell] = 0
        incorrectCells = emptySet()
        statusMessage = null
        if (entries.all { it != 0 }) {
            if (current.isValidCompletion(entries)) {
                elapsedSeconds = (SystemClock.elapsedRealtime() - startedAt) / 1000L
                newRecord = scores.record(current.size, current.difficulty, elapsedSeconds)
                rewardFocus = 1
                page = Page.REWARD
            } else {
                incorrectCells = SudokuGenerator.conflictingCells(entries, current.size)
                    .filterTo(mutableSetOf()) { !current.isGiven(it) }
                statusMessage = "还有数字不正确，请继续检查"
            }
        }
    }

    private fun openValuePicker(size: BoardSize) {
        pickerMode = PickerMode.VALUE
        pickerSelection = size.defaultPickerValue - 1
        pickerDraftMask = 0
        pickerLimitReached = false
        pickerOpen = true
    }

    private fun openCandidatePicker(size: BoardSize) {
        pickerMode = PickerMode.CANDIDATES
        pickerDraftMask = candidateMasks[selectedCell]
        pickerSelection = (0 until size.side).firstOrNull { pickerDraftMask and (1 shl it) != 0 }
            ?: (size.defaultPickerValue - 1)
        pickerLimitReached = false
        pickerOpen = true
    }

    private fun togglePickerCandidate() {
        val bit = 1 shl pickerSelection
        if (pickerDraftMask and bit != 0) {
            pickerDraftMask = pickerDraftMask and bit.inv()
            pickerLimitReached = false
        } else if (Integer.bitCount(pickerDraftMask) < 4) {
            pickerDraftMask = pickerDraftMask or bit
            pickerLimitReached = false
        } else {
            pickerLimitReached = true
        }
    }

    private fun handleRewardKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> rewardFocus = 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> rewardFocus = 1
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (rewardFocus == 0) page = Page.SCORES else showHome()
            }
            KeyEvent.KEYCODE_BACK -> showHome()
            else -> return false
        }
        return true
    }

    private fun handleScoresKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (scoreFocus in 1..5) scoreFocus--
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (scoreFocus in 0..4) scoreFocus++
            KeyEvent.KEYCODE_DPAD_UP -> if (scoreFocus == 6) scoreFocus = 4
            KeyEvent.KEYCODE_DPAD_DOWN -> if (scoreFocus in 0..5) scoreFocus = 6
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> when (scoreFocus) {
                0 -> boardSize = BoardSize.FOUR
                1 -> boardSize = BoardSize.SIX
                2 -> boardSize = BoardSize.NINE
                3 -> difficulty = Difficulty.EASY
                4 -> difficulty = Difficulty.MEDIUM
                5 -> difficulty = Difficulty.HARD
                6 -> showHome()
            }
            KeyEvent.KEYCODE_BACK -> showHome()
            else -> return false
        }
        return true
    }

    private fun showHome() {
        puzzle = null
        entries = IntArray(0)
        candidateMasks = IntArray(0)
        pickerOpen = false
        twentyFourPuzzle = null
        twentyFourRound = null
        twentyFourSource = null
        twentyFourOperation = null
        exitOpen = false
        homeFocus = 8
        page = Page.HOME
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(ink)
        paint.color = panel
        canvas.drawCircle(110f, 25f, 390f, paint)
        paint.color = Color.argb(15, 86, 214, 201)
        canvas.drawCircle(1760f, 970f, 500f, paint)
    }

    private fun drawHome(canvas: Canvas) {
        text(canvas, "数独", 128f, 350f, 126f, cream, true)
        text(canvas, "助手", 128f, 555f, 126f, mint, true)
        text(canvas, "数独与 24 点，专注一局。", 128f, 700f, 38f, muted)
        paint.color = mint
        canvas.drawCircle(137f, 830f, 10f, paint)
        text(canvas, "遥控器与触摸屏均可操作", 172f, 844f, 28f, cream)

        rounded(canvas, 726f, 84f, 1792f, 996f, 32f, panel)
        text(canvas, "选择宫格", 803f, 215f, 46f, cream, true)
        text(canvas, "每一局都会生成新题", 1715f, 216f, 30f, muted, align = Paint.Align.RIGHT)
        BoardSize.entries.forEachIndexed { index, size ->
            val left = 802f + index * 314f
            optionCard(canvas, left, 264f, left + 286f, 452f, size.label, "${size.side} × ${size.side}", boardSize == size, homeFocus == index)
        }
        text(canvas, "选择难度", 803f, 557f, 46f, cream, true)
        text(canvas, "由题面已知数字数量决定", 1715f, 558f, 30f, muted, align = Paint.Align.RIGHT)
        Difficulty.entries.forEachIndexed { index, item ->
            val left = 802f + index * 314f
            optionCard(canvas, left, 604f, left + 286f, 790f, item.label, "已知 ${SudokuGenerator.clueCount(boardSize, item)} 格", difficulty == item, homeFocus == index + 3)
        }
        actionButton(canvas, 802f, 800f, 1076f, 920f, "最好成绩", homeFocus == 6, false)
        actionButton(canvas, 1092f, 800f, 1366f, 920f, "24 点", homeFocus == 7, false)
        actionButton(canvas, 1382f, 800f, 1716f, 920f, "开始数独", homeFocus == 8, true)
        if (exitOpen) drawExitDialog(canvas)
    }

    private fun drawExitDialog(canvas: Canvas) {
        paint.color = Color.argb(215, 8, 19, 29)
        canvas.drawRect(0f, 0f, 1920f, 1080f, paint)
        rounded(canvas, 390f, 300f, 1530f, 782f, 32f, panel)
        strokeRound(canvas, 390f, 300f, 1530f, 782f, 32f, panelLight, 4f)
        textCenter(canvas, "退出数独助手？", 960f, 430f, 52f, cream, true)
        textCenter(canvas, "当前没有进行中的游戏，可以安全退出。", 960f, 510f, 30f, muted)
        actionButton(canvas, 485f, 588f, 904f, 710f, "取消", !exitSelected, !exitSelected)
        actionButton(canvas, 936f, 588f, 1435f, 710f, "退出应用", exitSelected, exitSelected)
    }

    private fun drawGame(canvas: Canvas) {
        val current = puzzle ?: return
        val left = 96f
        val top = 48f
        val boardPixels = 984f
        val side = current.size.side
        val cell = boardPixels / side
        paint.style = Paint.Style.FILL
        paint.color = cream
        canvas.drawRect(left, top, left + boardPixels, top + boardPixels, paint)
        val selectedRow = selectedCell / side
        val selectedColumn = selectedCell % side
        entries.indices.forEach { index ->
            val row = index / side
            val column = index % side
            val sameGroup = row == selectedRow || column == selectedColumn ||
                (row / current.size.blockRows == selectedRow / current.size.blockRows && column / current.size.blockColumns == selectedColumn / current.size.blockColumns)
            paint.color = when {
                index == selectedCell -> mint
                sameGroup -> Color.rgb(225, 234, 231)
                current.isGiven(index) -> Color.rgb(241, 238, 231)
                else -> Color.WHITE
            }
            canvas.drawRect(left + column * cell, top + row * cell, left + (column + 1) * cell, top + (row + 1) * cell, paint)
        }
        for (line in 0..side) {
            paint.color = if (line % current.size.blockColumns == 0) panel else Color.rgb(154, 173, 181)
            paint.strokeWidth = if (line % current.size.blockColumns == 0) 5f else 1.5f
            canvas.drawLine(left + line * cell, top, left + line * cell, top + boardPixels, paint)
            paint.color = if (line % current.size.blockRows == 0) panel else Color.rgb(154, 173, 181)
            paint.strokeWidth = if (line % current.size.blockRows == 0) 5f else 1.5f
            canvas.drawLine(left, top + line * cell, left + boardPixels, top + line * cell, paint)
        }
        entries.forEachIndexed { index, value ->
            if (value != 0) {
                val color = when {
                    index in incorrectCells -> coral
                    index == selectedCell -> ink
                    current.isGiven(index) -> Color.rgb(20, 38, 50)
                    else -> Color.rgb(19, 121, 119)
                }
                val row = index / side
                val column = index % side
                textCenter(canvas, value.toString(), left + column * cell + cell / 2f, top + row * cell + cell * .72f, cell * .66f, color, true)
            }
        }
        candidateMasks.forEachIndexed { index, mask ->
            if (entries[index] == 0 && mask != 0) {
                val row = index / side
                val column = index % side
                val cellLeft = left + column * cell
                val cellTop = top + row * cell
                var slot = 0
                repeat(side) { numberIndex ->
                    if (mask and (1 shl numberIndex) != 0 && slot < 4) {
                        val xFactor = if (slot % 2 == 0) .23f else .77f
                        val yFactor = if (slot < 2) .32f else .86f
                        textCenter(
                            canvas,
                            (numberIndex + 1).toString(),
                            cellLeft + cell * xFactor,
                            cellTop + cell * yFactor,
                            min(cell * .28f, 34f),
                            if (index == selectedCell) ink else Color.rgb(19, 121, 119),
                            true
                        )
                        slot++
                    }
                }
            }
        }
        strokeRect(canvas, left + selectedColumn * cell + 3f, top + selectedRow * cell + 3f, left + (selectedColumn + 1) * cell - 3f, top + (selectedRow + 1) * cell - 3f, mint, 7f)

        text(canvas, "${current.size.label} · ${current.difficulty.label}", 1165f, 135f, 40f, mint, true)
        text(canvas, formatTime(elapsedSeconds), 1165f, 280f, 74f, cream)
        text(canvas, "本局用时", 1165f, 340f, 28f, muted)
        text(canvas, "触摸操作", 1165f, 430f, 32f, cream, true)
        text(canvas, "点按可填写格：打开数字面板", 1165f, 474f, 25f, muted)
        text(canvas, "长按空格：添加最多 4 个预选数字", 1165f, 535f, 25f, muted)
        text(canvas, "遥控器操作", 1165f, 610f, 32f, cream, true)
        text(canvas, "方向键移动 · 确定键填数", 1165f, 654f, 25f, muted)
        text(canvas, "菜单键为空格添加预选数字", 1165f, 698f, 25f, muted)
        text(canvas, "自动提交", 1165f, 760f, 32f, cream, true)
        text(canvas, "最后一个空格填满后自动校验", 1165f, 804f, 25f, muted)
        statusMessage?.let { text(canvas, it, 1165f, 850f, 27f, coral, true) }
        val filled = entries.count { it != 0 }
        text(canvas, "完成度  $filled / ${entries.size}", 1165f, 885f, 27f, cream)
        rounded(canvas, 1165f, 910f, 1824f, 924f, 7f, panelLight)
        rounded(canvas, 1165f, 910f, 1165f + 659f * filled / entries.size, 924f, 7f, mint)
        text(canvas, "返回键退出本局", 1165f, 975f, 25f, muted)
        if (pickerOpen) drawPicker(canvas, current.size)
    }

    private fun drawPicker(canvas: Canvas, size: BoardSize) {
        paint.color = Color.argb(145, 8, 19, 29)
        canvas.drawRect(0f, 0f, 1920f, 1080f, paint)
        val layout = pickerLayout(size)
        rounded(canvas, layout.left, layout.top, layout.right, layout.bottom, 28f, panel)
        strokeRound(canvas, layout.left, layout.top, layout.right, layout.bottom, 28f, panelLight, 3f)
        textCenter(canvas, if (pickerMode == PickerMode.VALUE) "选择数字" else "预选数字", (layout.left + layout.right) / 2f, layout.top + 76f, 38f, cream, true)
        repeat(layout.rows) { row ->
            repeat(layout.columns) { column ->
                val index = row * layout.columns + column
                if (index < size.side) {
                    val x = layout.gridLeft + column * (layout.key + layout.gap)
                    val y = layout.gridTop + row * (layout.key + layout.gap)
                    val focused = index == pickerSelection
                    val chosen = pickerMode == PickerMode.CANDIDATES && pickerDraftMask and (1 shl index) != 0
                    rounded(canvas, x, y, x + layout.key, y + layout.key, 18f, when {
                        focused -> mint
                        chosen -> Color.rgb(31, 74, 84)
                        else -> ink
                    })
                    strokeRound(canvas, x, y, x + layout.key, y + layout.key, 18f, when {
                        focused -> cream
                        chosen -> gold
                        else -> panelLight
                    }, if (focused) 5f else if (chosen) 4f else 2f)
                    textCenter(canvas, "${index + 1}", x + layout.key / 2f, y + layout.key * .7f, 58f, when {
                        focused -> ink
                        chosen -> gold
                        else -> cream
                    }, true)
                    if (chosen) {
                        paint.color = gold
                        canvas.drawCircle(x + layout.key - 18f, y + 18f, 8f, paint)
                    }
                }
            }
        }
        if (pickerMode == PickerMode.CANDIDATES) {
            val buttonTop = layout.bottom - 92f
            val middle = (layout.left + layout.right) / 2f
            actionButton(canvas, layout.left + 34f, buttonTop, middle - 8f, layout.bottom - 24f, "取消", false, false)
            actionButton(canvas, middle + 8f, buttonTop, layout.right - 34f, layout.bottom - 24f, "保存预选", false, true)
            val footer = if (pickerLimitReached) "每格最多预选 4 个数字" else "点按数字切换 · 遥控器菜单键切换"
            textCenter(canvas, footer, (layout.left + layout.right) / 2f, layout.bottom - 112f, 22f, if (pickerLimitReached) coral else muted)
        } else {
            textCenter(canvas, "点按数字填入 · 遥控器确定键填入", (layout.left + layout.right) / 2f, layout.bottom - 28f, 22f, muted)
        }
    }

    private fun pickerLayout(size: BoardSize): PickerLayout {
        val columns = if (size == BoardSize.FOUR) 2 else 3
        val rows = (size.side + columns - 1) / columns
        val panelWidth = if (size == BoardSize.FOUR) 500f else 620f
        val key = if (size == BoardSize.FOUR) 144f else 128f
        val gap = 16f
        val footerHeight = if (pickerMode == PickerMode.CANDIDATES) 170f else 75f
        val panelHeight = 125f + rows * (key + gap) + footerHeight
        val right = 1848f
        val left = right - panelWidth
        val top = (1080f - panelHeight) / 2f
        val gridWidth = columns * key + (columns - 1) * gap
        return PickerLayout(
            columns = columns,
            rows = rows,
            key = key,
            gap = gap,
            left = left,
            top = top,
            right = right,
            bottom = top + panelHeight,
            gridLeft = (left + right - gridWidth) / 2f,
            gridTop = top + 105f
        )
    }

    private fun drawTwentyFour(canvas: Canvas) {
        val currentPuzzle = twentyFourPuzzle ?: return
        val round = twentyFourRound ?: return
        val values = round.values

        text(canvas, "24 点挑战", 72f, 98f, 58f, cream, true)
        text(canvas, "用完四个数字，让最后的结果等于 24", 72f, 138f, 26f, muted)
        text(canvas, "目标  24", 1848f, 98f, 40f, gold, true, Paint.Align.RIGHT)
        text(canvas, "初始数字  ${currentPuzzle.numbers.joinToString("  ·  ")}", 1848f, 138f, 24f, muted, align = Paint.Align.RIGHT)

        values.forEachIndexed { index, value ->
            val rect = twentyFourNumberRect(index)
            val isSource = index == twentyFourSource
            val focused = index == twentyFourFocus
            val fill = when {
                value == null -> Color.rgb(11, 27, 39)
                isSource -> Color.rgb(79, 64, 30)
                focused -> panelLight
                else -> panel
            }
            rounded(canvas, rect.left, rect.top, rect.right, rect.bottom, 28f, fill)
            strokeRound(
                canvas,
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                28f,
                when {
                    focused -> cream
                    isSource -> gold
                    else -> panelLight
                },
                if (focused) 7f else if (isSource) 5f else 3f
            )
            if (value == null) {
                textCenter(canvas, "已合并", rect.centerX(), rect.centerY() + 12f, 30f, Color.rgb(77, 99, 112))
            } else {
                if (isSource) textCenter(canvas, "第一个数字", rect.centerX(), rect.top + 38f, 22f, gold, true)
                textCenter(canvas, value.toString(), rect.centerX(), rect.top + 135f, 96f, if (isSource) gold else cream, true)
            }
        }

        textCenter(canvas, "先选数字  →  选运算符  →  选第二个数字", 960f, 635f, 28f, muted)
        ArithmeticOperation.entries.forEachIndexed { index, operation ->
            val rect = twentyFourOperationRect(index)
            val selected = operation == twentyFourOperation
            val focused = twentyFourFocus == index + 4
            rounded(canvas, rect.left, rect.top, rect.right, rect.bottom, 24f, when {
                selected -> gold
                focused -> mint
                else -> panel
            })
            strokeRound(canvas, rect.left, rect.top, rect.right, rect.bottom, 24f, if (focused) cream else panelLight, if (focused) 7f else 3f)
            textCenter(canvas, operation.symbol, rect.centerX(), rect.top + 91f, 70f, if (selected || focused) ink else cream, true)
        }

        val statusColor = when {
            round.isSolved -> mint
            round.remainingCount == 1 -> coral
            else -> cream
        }
        textCenter(canvas, twentyFourMessage, 960f, 872f, 30f, statusColor, true)
        val utilityLabels = listOf("重置", "换一题", "返回首页")
        utilityLabels.forEachIndexed { index, label ->
            val rect = twentyFourUtilityRect(index)
            actionButton(canvas, rect.left, rect.top, rect.right, rect.bottom, label, twentyFourFocus == index + 8, index == 1 && round.remainingCount == 1)
        }
        text(canvas, "遥控器菜单键：提示最后一步", 72f, 1030f, 22f, muted)
        text(canvas, "除法只允许整除", 1848f, 1030f, 22f, muted, align = Paint.Align.RIGHT)
    }

    private fun drawReward(canvas: Canvas) {
        textCenter(canvas, "完成！", 960f, 345f, 108f, mint, true)
        textCenter(canvas, "${boardSize.label} · ${difficulty.label}", 960f, 405f, 30f, muted)
        textCenter(canvas, formatTime(elapsedSeconds), 960f, 535f, 86f, cream)
        if (newRecord) textCenter(canvas, "新的最好成绩", 960f, 590f, 31f, gold, true)
        actionButton(canvas, 650f, 680f, 925f, 800f, "查看成绩", rewardFocus == 0, false)
        actionButton(canvas, 955f, 680f, 1290f, 800f, "选择下一关", rewardFocus == 1, true)
    }

    private fun drawScores(canvas: Canvas) {
        text(canvas, "最好成绩", 64f, 110f, 54f, cream, true)
        text(canvas, "每个组合保留最快的 10 次", 64f, 150f, 25f, muted)
        val labels = listOf("四宫", "六宫", "九宫", "简单", "中等", "困难")
        labels.forEachIndexed { index, label ->
            val x = 64f + index * 132f
            actionButton(canvas, x, 190f, x + 116f, 246f, label, scoreFocus == index, index == boardSize.ordinal || index - 3 == difficulty.ordinal)
        }
        actionButton(canvas, 1635f, 65f, 1855f, 130f, "返回选择", scoreFocus == 6, true)
        val values = scores.scores(boardSize, difficulty)
        if (values.isEmpty()) {
            textCenter(canvas, "还没有通关记录", 960f, 540f, 42f, cream, true)
            textCenter(canvas, "完成一局后，成绩会出现在这里", 960f, 595f, 28f, muted)
        } else {
            values.forEachIndexed { index, seconds ->
                val column = index / 5
                val row = index % 5
                val x = 150f + column * 850f
                val y = 300f + row * 120f
                rounded(canvas, x, y, x + 760f, y + 92f, 16f, panel)
                text(canvas, "%02d".format(index + 1), x + 35f, y + 60f, 28f, if (index < 3) gold else muted, true)
                text(canvas, formatTime(seconds), x + 140f, y + 62f, 36f, cream, true)
            }
        }
    }

    private fun optionCard(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, title: String, subtitle: String, selected: Boolean, focused: Boolean) {
        rounded(canvas, l, t, r, b, 24f, if (selected) Color.rgb(31, 74, 84) else ink)
        if (selected || focused) strokeRound(canvas, l, t, r, b, 24f, if (focused) cream else mint, if (focused) 5f else 4f)
        textCenter(canvas, title, (l + r) / 2f, t + 89f, 42f, if (selected) mint else cream, true)
        textCenter(canvas, subtitle, (l + r) / 2f, t + 145f, 25f, muted)
    }

    private fun actionButton(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, label: String, focused: Boolean, primary: Boolean) {
        rounded(canvas, l, t, r, b, 18f, if (primary || focused) mint else panelLight)
        if (focused) strokeRound(canvas, l, t, r, b, 18f, cream, 7f)
        textCenter(canvas, label, (l + r) / 2f, (t + b) / 2f + 14f, 36f, if (primary || focused) ink else cream, true)
    }

    private fun text(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        canvas.drawText(value, x, baseline, paint)
    }

    private fun textCenter(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false) =
        text(canvas, value, x, baseline, size, color, bold, Paint.Align.CENTER)

    private fun rounded(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, paint)
    }

    private fun strokeRound(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int, width: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = color
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun strokeRect(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, width: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = color
        canvas.drawRect(l, t, r, b, paint)
        paint.style = Paint.Style.FILL
    }

    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }
}
