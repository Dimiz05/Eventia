package com.example.eventia.api.models

data class EventDto(
    val id: Long,
    val title: String,
    val summary: String,
    val description: String?,
    val startsAt: String,
    val endsAt: String?,
    val category: CategoryDto,
    val imageUrl: String?,
    val venue: VenueDto,
    val capacity: Int,
    val availableSpots: Int,
    val isAttending: Boolean?
)
