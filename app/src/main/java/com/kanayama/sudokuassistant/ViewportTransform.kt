package com.kanayama.sudokuassistant

internal data class DesignPoint(val x: Float, val y: Float)

internal data class ViewportTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
) {
    fun toDesignPoint(viewX: Float, viewY: Float): DesignPoint =
        DesignPoint((viewX - offsetX) / scale, (viewY - offsetY) / scale)

    companion object {
        const val DESIGN_WIDTH = 1920f
        const val DESIGN_HEIGHT = 1080f

        fun fit(viewWidth: Int, viewHeight: Int): ViewportTransform {
            if (viewWidth <= 0 || viewHeight <= 0) return ViewportTransform(1f, 0f, 0f)
            val scale = minOf(viewWidth / DESIGN_WIDTH, viewHeight / DESIGN_HEIGHT)
            return ViewportTransform(
                scale = scale,
                offsetX = (viewWidth - DESIGN_WIDTH * scale) / 2f,
                offsetY = (viewHeight - DESIGN_HEIGHT * scale) / 2f
            )
        }
    }
}
