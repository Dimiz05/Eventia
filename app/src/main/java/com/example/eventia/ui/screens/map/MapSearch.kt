package com.example.eventia.ui.screens.map

import com.example.eventia.domain.model.Event

internal fun filterMapMarkers(markers: Map<MapPoint, List<Event>>, query: String): Map<MapPoint, List<Event>> {
    val search = query.trim()
    if (search.isEmpty()) return markers

    return buildMap {
        markers.forEach { (point, events) ->
            val matches = events.filter { event ->
                event.title.contains(search, ignoreCase = true) ||
                    event.venue.name.contains(search, ignoreCase = true)
            }
            if (matches.isNotEmpty()) put(point, matches)
        }
    }
}
