package com.kanayama.sudokuassistant

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTransformTest {
    @Test
    fun exactDesignSizeUsesIdentityTransform() {
        val viewport = ViewportTransform.fit(1920, 1080)

        assertEquals(1f, viewport.scale, 0.0001f)
        assertEquals(0f, viewport.offsetX, 0.0001f)
        assertEquals(0f, viewport.offsetY, 0.0001f)
    }

    @Test
    fun widePhoneCentersDesignWithoutStretching() {
        val viewport = ViewportTransform.fit(2400, 1080)
        val point = viewport.toDesignPoint(240f, 0f)

        assertEquals(1f, viewport.scale, 0.0001f)
        assertEquals(240f, viewport.offsetX, 0.0001f)
        assertEquals(0f, point.x, 0.0001f)
        assertEquals(0f, point.y, 0.0001f)
    }

    @Test
    fun fourByThreeTabletCentersDesignVertically() {
        val viewport = ViewportTransform.fit(1024, 768)
        val center = viewport.toDesignPoint(512f, 384f)

        assertEquals(1024f / 1920f, viewport.scale, 0.0001f)
        assertEquals(0f, viewport.offsetX, 0.0001f)
        assertEquals(96f, viewport.offsetY, 0.0001f)
        assertEquals(960f, center.x, 0.0001f)
        assertEquals(540f, center.y, 0.0001f)
    }
}
