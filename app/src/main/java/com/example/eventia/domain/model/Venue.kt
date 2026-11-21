package com.example.eventia.domain.model

data class Venue(
    val id: Long,
    val name: String,
    val address: String,
    val description: String? = null,
    val openingHours: String? = null,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val upcomingEventsCount: Int? = null
)
