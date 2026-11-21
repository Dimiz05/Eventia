package com.example.eventia.domain.model

import java.time.OffsetDateTime

data class Event(
    val id: Long,
    val title: String,
    val summary: String,
    val description: String?,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime?,
    val category: Category,
    val imageUrl: String?,
    val venue: Venue,
    val capacity: Int,
    val availableSpots: Int,
    val isAttending: Boolean?
)
