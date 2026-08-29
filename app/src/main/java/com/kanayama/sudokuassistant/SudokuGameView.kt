package com.kanayama.sudokuassistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import com.kanayama.sudokuassistant.data.ScoreRepository
import com.kanayama.sudokuassistant.model.BoardSize
import com.kanayama.sudokuassistant.model.Difficulty
import com.kanayama.sudokuassistant.model.Puzzle
import com.kanayama.sudokuassistant.model.SudokuGenerator
import kotlin.math.min

private enum class Page { HOME, GAME, REWARD, SCORES }
private enum class PickerMode { VALUE, CANDIDATES }

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
    private var homeFocus = 7
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
        val sx = width / 1920f
        val sy = height / 1080f
        canvas.save()
        canvas.scale(sx, sy)
        drawBackground(canvas)
        when (page) {
            Page.HOME -> drawHome(canvas)
            Page.GAME -> drawGame(canvas)
            Page.REWARD -> drawReward(canvas)
            Page.SCORES -> drawScores(canvas)
        }
        canvas.restore()
    }

    fun handleKey(keyCode: Int): Boolean {
        val handled = when (page) {
            Page.HOME -> handleHomeKey(keyCode)
            Page.GAME -> handleGameKey(keyCode)
            Page.REWARD -> handleRewardKey(keyCode)
            Page.SCORES -> handleScoresKey(keyCode)
        }
        if (handled) invalidate()
        return handled
    }

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
                1, 2, 4, 5, 7 -> homeFocus - 1
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> homeFocus = when (homeFocus) {
                0, 1, 3, 4, 6 -> homeFocus + 1
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_UP -> homeFocus = when (homeFocus) {
                in 3..5 -> homeFocus - 3
                6 -> 3
                7 -> 5
                else -> homeFocus
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> homeFocus = when (homeFocus) {
                in 0..2 -> homeFocus + 3
                3 -> 6
                4, 5 -> 7
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
            7 -> startGame()
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
        exitOpen = false
        homeFocus = 7
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
        text(canvas, "坐下来，专注一局。", 128f, 700f, 38f, muted)
        paint.color = mint
        canvas.drawCircle(137f, 830f, 10f, paint)
        text(canvas, "遥控器方向键移动 · 确定键选择", 172f, 844f, 28f, cream)

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
        actionButton(canvas, 802f, 800f, 1155f, 920f, "最好成绩", homeFocus == 6, false)
        actionButton(canvas, 1187f, 800f, 1716f, 920f, "开始游戏", homeFocus == 7, true)
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
        text(canvas, "方向键", 1165f, 430f, 32f, cream, true)
        text(canvas, "移动蓝绿色光标", 1165f, 474f, 25f, muted)
        text(canvas, "确定键", 1165f, 540f, 32f, cream, true)
        text(canvas, "打开数字面板，选择并正式填入", 1165f, 584f, 25f, muted)
        text(canvas, "菜单键", 1165f, 650f, 32f, cream, true)
        text(canvas, "为空格添加最多 4 个预选数字", 1165f, 694f, 25f, muted)
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
        val columns = if (size == BoardSize.FOUR) 2 else 3
        val rows = (size.side + columns - 1) / columns
        val panelWidth = if (size == BoardSize.FOUR) 500f else 620f
        val key = if (size == BoardSize.FOUR) 144f else 128f
        val gap = 16f
        val panelHeight = 125f + rows * (key + gap) + 75f
        val right = 1848f
        val left = right - panelWidth
        val top = (1080f - panelHeight) / 2f
        rounded(canvas, left, top, right, top + panelHeight, 28f, panel)
        strokeRound(canvas, left, top, right, top + panelHeight, 28f, panelLight, 3f)
        textCenter(canvas, if (pickerMode == PickerMode.VALUE) "选择数字" else "预选数字", (left + right) / 2f, top + 76f, 38f, cream, true)
        val gridWidth = columns * key + (columns - 1) * gap
        val gridLeft = (left + right - gridWidth) / 2f
        repeat(rows) { row ->
            repeat(columns) { column ->
                val index = row * columns + column
                if (index < size.side) {
                    val x = gridLeft + column * (key + gap)
                    val y = top + 105f + row * (key + gap)
                    val focused = index == pickerSelection
                    val chosen = pickerMode == PickerMode.CANDIDATES && pickerDraftMask and (1 shl index) != 0
                    rounded(canvas, x, y, x + key, y + key, 18f, when {
                        focused -> mint
                        chosen -> Color.rgb(31, 74, 84)
                        else -> ink
                    })
                    strokeRound(canvas, x, y, x + key, y + key, 18f, when {
                        focused -> cream
                        chosen -> gold
                        else -> panelLight
                    }, if (focused) 5f else if (chosen) 4f else 2f)
                    textCenter(canvas, "${index + 1}", x + key / 2f, y + key * .7f, 58f, when {
                        focused -> ink
                        chosen -> gold
                        else -> cream
                    }, true)
                    if (chosen) {
                        paint.color = gold
                        canvas.drawCircle(x + key - 18f, y + 18f, 8f, paint)
                    }
                }
            }
        }
        val footer = if (pickerMode == PickerMode.VALUE) {
            "方向键选择 · 确定键填入"
        } else if (pickerLimitReached) {
            "每格最多预选 4 个数字"
        } else {
            "菜单键选中/取消 · 确定键完成"
        }
        textCenter(canvas, footer, (left + right) / 2f, top + panelHeight - 28f, 22f, if (pickerLimitReached) coral else muted)
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
