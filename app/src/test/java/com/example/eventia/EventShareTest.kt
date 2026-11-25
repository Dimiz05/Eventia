package com.example.eventia

import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.eventdetail.venueMapUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class EventShareTest {
    private val venue = Venue(7, "Café & Arte", "Av. 6 de Agosto #123")

    @Test
    fun usesCoordinatesWhenBothAreValid() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=-16.5%2C-68.15",
            venueMapUrl(venue.copy(latitude = -16.5, longitude = -68.15))
        )
    }

    @Test
    fun encodesTheVenueNameAndAddressWhenCoordinatesAreMissingOrInvalid() {
        val expected = "https://www.google.com/maps/search/?api=1&query=Caf%C3%A9+%26+Arte%2C+Av.+6+de+Agosto+%23123"
        assertEquals(expected, venueMapUrl(venue.copy(latitude = -16.5)))
        assertEquals(expected, venueMapUrl(venue.copy(latitude = -16.5, longitude = 181.0)))
    }

    @Test
    fun fallsBackToTheVenueNameWhenThereIsNoAddress() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=Caf%C3%A9+%26+Arte",
            venueMapUrl(venue.copy(address = ""))
        )
    }
}
