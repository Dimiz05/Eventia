package com.example.eventia

import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.map.MapPoint
import com.example.eventia.ui.screens.map.filterMapMarkers
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapMarkersTest {
    @Test
    fun mapSearchFiltersEventsWithinSharedMarkers() {
        val venue = Venue(1, "Casa de la Cultura", "Av. Principal 123")
        val first = Event(
            id = 10,
            title = "Noche de jazz",
            summary = "",
            description = null,
            startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-04:00"),
            endsAt = null,
            category = Category(1, "Música"),
            imageUrl = null,
            venue = venue,
            capacity = 50,
            availableSpots = 36,
            isAttending = null
        )
        val second = first.copy(id = 11, title = "Encuentro de cuerdas")
        val third = first.copy(
            id = 12,
            title = "Feria del Libro",
            venue = Venue(2, "Biblioteca Municipal", "Calle Sucre")
        )
        val venuePoint = MapPoint(-16.5, -68.15)
        val libraryPoint = MapPoint(-16.51, -68.16)
        val markers = linkedMapOf(venuePoint to listOf(first, second), libraryPoint to listOf(third))

        assertEquals(markers, filterMapMarkers(markers, "   "))
        assertEquals(mapOf(venuePoint to listOf(first)), filterMapMarkers(markers, " JaZz "))
        assertEquals(mapOf(venuePoint to listOf(first, second)), filterMapMarkers(markers, "cultura"))
        assertEquals(emptyMap<MapPoint, List<Event>>(), filterMapMarkers(markers, "robótica"))
    }

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
