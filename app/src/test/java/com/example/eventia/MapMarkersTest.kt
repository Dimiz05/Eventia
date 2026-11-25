package com.example.eventia

import com.example.eventia.ui.screens.map.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapMarkersTest {
    @Test
    fun pointRequiresTwoCoordinatesInRange() {
        assertNull(MapPoint.from(null, 0.0))
        assertNull(MapPoint.from(0.0, null))
        assertNull(MapPoint.from(91.0, 0.0))
        assertNull(MapPoint.from(0.0, -181.0))
        assertNull(MapPoint.from(Double.NaN, 0.0))
        assertNull(MapPoint.from(0.0, Double.POSITIVE_INFINITY))
        assertEquals(MapPoint(-90.0, -180.0), MapPoint.from(-90.0, 180.0))
    }
}
