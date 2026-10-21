package com.kanayama.sudokuassistant

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.kanayama.sudokuassistant.model.ArithmeticOperation
import com.kanayama.sudokuassistant.model.TwentyFourPuzzle
import com.kanayama.sudokuassistant.model.TwentyFourRound
import kotlin.math.min

/** Draws the 24-point workshop; game state and input remain owned by SudokuGameView. */
internal class TwentyFourRenderer(resources: Resources) {
    private val paper = Color.rgb(246, 243, 234)
    private val white = Color.rgb(255, 253, 247)
    private val forest = Color.rgb(24, 62, 54)
    private val muted = Color.rgb(105, 117, 106)
    private val line = Color.rgb(218, 222, 208)
    private val sage = Color.rgb(222, 233, 215)
    private val faint = Color.rgb(236, 238, 226)
    private val gold = Color.rgb(240, 196, 106)
    private val terracotta = Color.rgb(158, 72, 45)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val bold = Typeface.create("sans-serif", Typeface.BOLD)
    private val numerals = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val dash = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    // This renderer is created lazily on entering 24 points, never during the home first frame.
    private val artwork = BitmapFactory.decodeResource(resources, R.drawable.twenty_four_workshop)
    private val hero = RectF(64f, 64f, 416f, 1008f)
    private val heroClip = Path().apply { addRoundRect(hero, 32f, 32f, Path.Direction.CW) }

    fun draw(
        canvas: Canvas,
        puzzle: TwentyFourPuzzle,
        round: TwentyFourRound,
        focus: Int,
        source: Int?,
        operation: ArithmeticOperation?,
        message: String,
    ) {
        canvas.drawColor(paper)
        drawHero(canvas, round.isSolved)
        text(canvas, if (round.isSolved) "太棒了，正好 24！" else "用四个数字，算出 24", 488f, 112f, 44f, forest, true)
        val stage = when {
            round.remainingCount == 1 -> 3
            source == null -> 0
            operation == null -> 1
            else -> 2
        }
        drawStages(canvas, stage)
        round.values.forEachIndexed { index, value ->
            drawNumber(canvas, index, value, focus == index, source == index, round.isSolved)
        }
        text(canvas, "选择运算符", 488f, 682f, 25f, muted, true)
        text(canvas, "＋  −  ×  ÷", 1440f, 682f, 24f, muted, align = Paint.Align.RIGHT)
        ArithmeticOperation.entries.forEachIndexed { index, item ->
            drawOperation(canvas, index, item, focus == index + 4, operation == item)
        }
        val statusColor = if (round.remainingCount == 1 && !round.isSolved) terracotta else forest
        val statusFill = when {
            round.remainingCount == 1 && !round.isSolved -> Color.rgb(247, 229, 216)
            message.startsWith("原题最后一步提示") -> Color.rgb(250, 235, 196)
            else -> sage
        }
        box(canvas, RectF(488f, 846f, 1440f, 898f), 16f, statusFill)
        fittedText(canvas, message, 964f, 881f, 28f, statusColor, 904f, true, Paint.Align.CENTER)
        listOf("重置", "换一题", "返回首页").forEachIndexed { index, label ->
            val rect = utilityRect(index)
            val focused = focus == index + 8
            val primary = index == 1
            box(canvas, rect, 22f, if (primary || focused) forest else white)
            outline(canvas, rect, 22f, if (focused) forest else line, if (focused) 5f else 2f, if (focused) 6f else 0f)
            text(canvas, label, rect.centerX(), rect.centerY() + 12f, 32f, if (primary || focused) white else forest, true, Paint.Align.CENTER)
        }
        drawNotes(canvas, puzzle, round)
    }

    private fun drawHero(canvas: Canvas, solved: Boolean) {
        canvas.save()
        canvas.clipPath(heroClip)
        paint.color = Color.WHITE
        canvas.drawBitmap(artwork, null, hero, paint)
        canvas.restore()
        text(canvas, "数 字 工 坊", 104f, 130f, 25f, Color.rgb(203, 221, 197), true)
        text(canvas, "24", 95f, 304f, 174f, gold, face = numerals)
        text(canvas, "点挑战", 104f, 368f, 48f, white, true)
        text(canvas, "四个数字，三步巧思。", 104f, 427f, 25f, Color.rgb(218, 230, 210))
        text(canvas, if (solved) "每一次尝试，都有收获。" else "让每一步，都恰到好处。", 104f, 914f, 24f, white)
        text(canvas, "动动脑筋  ·  慢慢来", 104f, 958f, 22f, Color.rgb(203, 221, 197))
    }

    private fun drawStages(canvas: Canvas, active: Int) {
        listOf("选一个数字", "选运算符", "再选一个数字").forEachIndexed { index, label ->
            val x = 505f + index * 326f
            paint.color = if (index == active) forest else line
            canvas.drawCircle(x, 164f, 17f, paint)
            text(canvas, (index + 1).toString(), x, 172f, 22f, if (index == active) white else muted, true, Paint.Align.CENTER)
            text(canvas, label, x + 30f, 173f, 26f, if (index == active) forest else muted, index == active)
            if (index < 2) text(canvas, "→", x + 268f, 173f, 26f, muted)
        }
    }

    private fun drawNumber(canvas: Canvas, index: Int, value: Int?, focused: Boolean, selected: Boolean, solved: Boolean) {
        val rect = numberRect(index)
        if (value == null) {
            box(canvas, rect, 26f, faint)
            paint.pathEffect = dash
            outline(canvas, rect, 26f, line, 2f)
            paint.pathEffect = null
            text(canvas, "已合并", rect.centerX(), rect.centerY() + 12f, 29f, muted, align = Paint.Align.CENTER)
            return
        }
        val fill = when {
            solved -> forest
            selected -> sage
            else -> white
        }
        box(canvas, RectF(rect.left, rect.top + 7f, rect.right, rect.bottom + 7f), 26f, Color.rgb(224, 224, 211))
        box(canvas, rect, 26f, fill)
        outline(canvas, rect, 26f, if (focused || selected) forest else line, if (focused) 6f else 2f)
        if (focused) outline(canvas, rect, 31f, Color.rgb(202, 213, 189), 3f, 8f)
        if (selected || solved) {
            box(canvas, RectF(rect.left + 22f, rect.top + 18f, rect.left + 112f, rect.top + 52f), 12f, if (solved) gold else forest)
            text(canvas, if (solved) "完成" else "已选", rect.left + 67f, rect.top + 43f, 22f, if (solved) forest else white, true, Paint.Align.CENTER)
        } else {
            text(canvas, "0${index + 1}", rect.left + 25f, rect.top + 44f, 22f, muted)
        }
        fittedText(canvas, value.toString(), rect.centerX(), rect.top + 151f, 104f, if (solved) white else forest, rect.width() - 48f, face = numerals, align = Paint.Align.CENTER)
    }

    private fun drawOperation(canvas: Canvas, index: Int, operation: ArithmeticOperation, focused: Boolean, selected: Boolean) {
        val rect = operationRect(index)
        val fills = intArrayOf(sage, Color.rgb(246, 227, 211), Color.rgb(248, 234, 189), Color.rgb(222, 233, 232))
        box(canvas, rect, 23f, if (selected) forest else fills[index])
        if (focused) outline(canvas, rect, 23f, forest, 5f, 5f)
        text(canvas, operation.symbol, rect.centerX(), rect.top + 74f, 66f, if (selected) white else forest, true, Paint.Align.CENTER)
        val labels = listOf("加", "减", "乘", "除")
        text(canvas, labels[index], rect.centerX(), rect.bottom - 14f, 22f, if (selected) white else muted, align = Paint.Align.CENTER)
    }

    private fun drawNotes(canvas: Canvas, puzzle: TwentyFourPuzzle, round: TwentyFourRound) {
        val history = round.history
        box(canvas, RectF(1504f, 64f, 1856f, 1008f), 28f, Color.rgb(237, 239, 227))
        text(canvas, "计算记录", 1536f, 121f, 31f, forest, true)
        text(canvas, "留住每一步巧思", 1536f, 162f, 24f, muted)
        repeat(3) { index ->
            box(canvas, RectF(1536f + index * 100f, 192f, 1624f + index * 100f, 198f), 3f, if (index < history.size) forest else line)
        }
        text(canvas, "已完成 ${history.size} / 3 步", 1536f, 237f, 23f, muted)

        // Replay only the player's completed moves; this never exposes the puzzle's solution.
        val replay = puzzle.numbers.map<Int, Int?> { it }.toMutableList()
        repeat(3) { index ->
            val top = 267f + index * 104f
            text(canvas, "0${index + 1}", 1536f, top + 29f, 21f, muted)
            val move = history.getOrNull(index)
            if (move != null) {
                val left = requireNotNull(replay[move.source])
                val right = requireNotNull(replay[move.target])
                val result = requireNotNull(move.operation.apply(left, right))
                val expression = "${operand(left)} ${move.operation.symbol} ${operand(right)} = $result"
                fittedText(canvas, expression, 1536f, top + 70f, 29f, forest, 288f, true)
                replay[move.source] = null
                replay[move.target] = result
            } else {
                text(canvas, if (index == history.size) "等待下一步巧思" else "·  ·  ·", 1536f, top + 70f, 24f, muted)
            }
            rule(canvas, 1536f, top + 88f, 1824f)
        }
        text(canvas, "原题数字", 1536f, 631f, 25f, forest, true)
        puzzle.numbers.forEachIndexed { index, value ->
            val left = 1536f + index * 74f
            box(canvas, RectF(left, 652f, left + 64f, 712f), 14f, white)
            text(canvas, value.toString(), left + 32f, 693f, 29f, forest, true, Paint.Align.CENTER)
        }
        text(canvas, "全部用上，每个只用一次。", 1536f, 756f, 23f, muted)
        rule(canvas, 1536f, 796f, 1824f)
        text(canvas, "玩法小贴士", 1536f, 840f, 25f, forest, true)
        text(canvas, "除法只允许整除", 1536f, 881f, 24f, muted)
        box(canvas, RectF(1536f, 923f, 1612f, 970f), 10f, white)
        text(canvas, "菜单", 1574f, 955f, 23f, forest, true, Paint.Align.CENTER)
        text(canvas, "提示最后一步", 1627f, 955f, 23f, forest)
    }

    private fun operand(value: Int): String = if (value < 0) "($value)" else value.toString()

    private fun rule(canvas: Canvas, left: Float, y: Float, right: Float) {
        paint.color = line
        paint.strokeWidth = 2f
        canvas.drawLine(left, y, right, y, paint)
    }

    private fun box(canvas: Canvas, rect: RectF, radius: Float, color: Int) {
        paint.color = color
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun outline(canvas: Canvas, rect: RectF, radius: Float, color: Int, width: Float, outset: Float = 0f) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = color
        canvas.drawRoundRect(rect.left - outset, rect.top - outset, rect.right + outset, rect.bottom + outset, radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun text(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, strong: Boolean = false, align: Paint.Align = Paint.Align.LEFT, face: Typeface? = null) {
        paint.color = color
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = face ?: if (strong) bold else regular
        canvas.drawText(value, x, baseline, paint)
    }

    private fun fittedText(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, width: Float, strong: Boolean = false, align: Paint.Align = Paint.Align.LEFT, face: Typeface? = null) {
        paint.textSize = size
        paint.typeface = face ?: if (strong) bold else regular
        val fitted = size * min(1f, width / paint.measureText(value).coerceAtLeast(1f))
        text(canvas, value, x, baseline, fitted, color, strong, align, face)
    }

    companion object {
        fun numberRect(index: Int): RectF {
            val left = if (index % 2 == 0) 488f else 988f
            val top = if (index < 2) 208f else 442f
            return RectF(left, top, left + 452f, top + 198f)
        }

        fun operationRect(index: Int): RectF {
            val left = 488f + index * 248f
            return RectF(left, 702f, left + 208f, 822f)
        }

        fun utilityRect(index: Int): RectF {
            val left = 488f + index * 328f
            return RectF(left, 924f, left + 296f, 1008f)
        }
    }
}
