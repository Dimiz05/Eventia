package com.example.eventia.api.models

data class VenueDto(
    val id: Long,
    val name: String,
    val address: String,
    val description: String?,
    val openingHours: String?,
    val imageUrl: String?,
    val lat: Double?,
    val lng: Double?,
    val upcomingEventsCount: Int?
)
