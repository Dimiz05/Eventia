package com.example.eventia

import com.example.eventia.api.models.EventDto
import com.example.eventia.repositories.toDomain
import com.google.gson.Gson
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMappingTest {
    @Test
    fun eventPreservesOffsetNullCoordinatesAndAttendance() {
        val guestJson = """
            {
              "id": 14, "title": "Compose", "summary": "Android",
              "startsAt": "2026-11-22T18:00:00-04:00",
              "category": {"id": 7, "name": "Tecnología"},
              "imageUrl": null,
              "venue": {"id": 10, "name": "Espacio Maker", "address": "Calle Tadeo", "lat": -17.38, "lng": null},
              "capacity": 35, "availableSpots": 0
            }
        """.trimIndent()
        val gson = Gson()
        val guest = gson.fromJson(guestJson, EventDto::class.java).toDomain()
        val authenticated = gson.fromJson(
            guestJson.dropLast(1) + """, "isAttending": false}""",
            EventDto::class.java
        ).toDomain()

        assertEquals(ZoneOffset.ofHours(-4), guest.startsAt.offset)
        assertEquals(18, guest.startsAt.hour)
        assertEquals(35, guest.capacity)
        assertEquals(0, guest.availableSpots)
        assertNull(guest.imageUrl)
        assertNull(guest.venue.longitude)
        assertNull(guest.isAttending)
        assertEquals(false, authenticated.isAttending)
    }
}
