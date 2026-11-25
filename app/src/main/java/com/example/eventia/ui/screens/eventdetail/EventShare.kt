package com.example.eventia.ui.screens.eventdetail

import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.map.MapPoint
import java.net.URLEncoder

internal fun venueMapUrl(venue: Venue): String {
    val point = MapPoint.from(venue.latitude, venue.longitude)
    val query = if (point != null) {
        "${point.latitude},${point.longitude}"
    } else {
        listOf(venue.name, venue.address).filter { it.isNotBlank() }.joinToString(", ")
    }
    return "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(query, Charsets.UTF_8)}"
}
