package com.example.eventia

import com.example.eventia.api.mappers.toDomain
import com.example.eventia.api.models.EventDto
import com.example.eventia.ui.screens.profile.AttendanceGroups
import com.google.gson.Gson
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceGroupsTest {
    // Event 42 from section 8.2 of Proyecto_Final_Eventia.pdf.
    private val event = Gson().fromJson(
        """
        {
          "id": 42,
          "title": "Noche de jazz en la plaza",
          "summary": "Clásicos del jazz con músicos invitados.",
          "startsAt": "2026-11-14T20:00:00-04:00",
          "category": { "id": 3, "name": "Música" },
          "imageUrl": null,
          "venue": { "id": 7, "name": "Casa de la Cultura",
                     "address": "Av. Principal 123", "lat": -17.3935, "lng": null },
          "capacity": 50,
          "availableSpots": 36
        }
        """.trimIndent(),
        EventDto::class.java
    ).toDomain()

    @Test
    fun usesTheStartInstantAcrossTimeZones() {
        val start = Instant.parse("2026-11-15T00:00:00Z")
        val events = listOf(event)

        assertEquals(AttendanceGroups(events, emptyList()), AttendanceGroups.from(events, start.minusNanos(1)))
        assertEquals(AttendanceGroups(events, emptyList()), AttendanceGroups.from(events, start))
        assertEquals(AttendanceGroups(emptyList(), events), AttendanceGroups.from(events, start.plusNanos(1)))
    }
}
