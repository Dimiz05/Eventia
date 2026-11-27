package com.example.eventia

import com.example.eventia.ui.screens.map.formatMapDistance
import org.junit.Assert.assertEquals
import org.junit.Test

class MapDistanceTest {
    @Test
    fun formatsMetersBelowOneKilometer() {
        assertEquals("850 m", formatMapDistance(850f))
        assertEquals("999 m", formatMapDistance(999.9f))
    }

    @Test
    fun formatsKilometersWithOneDecimalAndAComma() {
        assertEquals("1,0 km", formatMapDistance(1000f))
        assertEquals("2,4 km", formatMapDistance(2400f))
    }
}
