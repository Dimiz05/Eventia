package com.example.eventia

import com.example.eventia.api.EventiaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

class EventiaApiTest {
    @Test
    fun catalogSupportsBothQueryParametersOnTheEventsRoute() {
        val events = EventiaApi::class.java.declaredMethods.single { it.name == "getEvents" }
        assertEquals("events", events.getAnnotation(GET::class.java)?.value)
        assertEquals(
            listOf("q", "category"),
            events.parameterAnnotations.flatMap { it.toList() }.filterIsInstance<Query>().map { it.value }
        )
    }

    @Test
    fun loginUsesRelativePostRoute() {
        val route = EventiaApi::class.java.declaredMethods
            .singleOrNull { it.name == "login" }
            ?.getAnnotation(POST::class.java)
            ?.value

        assertEquals("auth/login", route)
    }

    @Test
    fun catalogKeepsExistingRelativeGetRoutes() {
        val routes = EventiaApi::class.java.declaredMethods.mapNotNull { method ->
            method.getAnnotation(GET::class.java)?.value
        }.toSet()

        assertEquals(
            setOf("categories", "events", "events/{id}", "venues", "venues/{id}", "venues/{id}/events"),
            routes - setOf("me", "me/attendances")
        )
    }

    @Test
    fun venueDetailAndEventsUseTheVenueIdWithoutAuthorization() {
        for ((methodName, route) in mapOf(
            "getVenue" to "venues/{id}",
            "getVenueEvents" to "venues/{id}/events"
        )) {
            val method = EventiaApi::class.java.declaredMethods.singleOrNull { it.name == methodName }
            assertEquals(route, method?.getAnnotation(GET::class.java)?.value)
            val annotations = method?.parameterAnnotations.orEmpty().flatMap { it.toList() }
            assertEquals(listOf("id"), annotations.filterIsInstance<Path>().map { it.value })
            assertEquals(emptyList<Header>(), annotations.filterIsInstance<Header>())
        }
    }

    @Test
    fun registrationUsesRelativePostRoute() {
        val register = EventiaApi::class.java.declaredMethods.singleOrNull { it.name == "register" }
        assertEquals("auth/register", register?.getAnnotation(POST::class.java)?.value)
    }

    @Test
    fun accountUsesRelativeGetWithAuthorization() {
        val me = EventiaApi::class.java.declaredMethods.singleOrNull { it.name == "getMe" }
        assertEquals("me", me?.getAnnotation(GET::class.java)?.value)
        val header = me?.parameterAnnotations?.flatMap { it.toList() }
            ?.filterIsInstance<Header>()?.singleOrNull()
        assertEquals("Authorization", header?.value)
    }

    @Test
    fun attendancesUseRelativeGetWithAuthorization() {
        val attendances = EventiaApi::class.java.declaredMethods
            .singleOrNull { it.name == "getMyAttendances" }
        assertEquals("me/attendances", attendances?.getAnnotation(GET::class.java)?.value)
        val headers = attendances?.parameterAnnotations.orEmpty().flatMap { it.toList() }
            .filterIsInstance<Header>()
        assertEquals(listOf("Authorization"), headers.map { it.value })
    }

    @Test
    fun eventDetailAcceptsAnAuthorizationHeaderAndKeepsItsIdPath() {
        val detail = EventiaApi::class.java.declaredMethods.single { it.name == "getEvent" }
        val parameters = detail.parameterAnnotations.flatMap { it.toList() }
        assertEquals("events/{id}", detail.getAnnotation(GET::class.java)?.value)
        assertEquals("id", parameters.filterIsInstance<Path>().single().value)
        assertEquals("Authorization", parameters.filterIsInstance<Header>().single().value)
    }

    @Test
    fun attendancePostsTheEventIdWithAuthorizationAndWithoutABody() {
        val attendance = EventiaApi::class.java.declaredMethods.single { it.name == "attendEvent" }
        val parameters = attendance.parameterAnnotations.flatMap { it.toList() }
        assertEquals("events/{id}/attendance", attendance.getAnnotation(POST::class.java)?.value)
        assertEquals("id", parameters.filterIsInstance<Path>().single().value)
        assertEquals("Authorization", parameters.filterIsInstance<Header>().single().value)
        assertFalse(parameters.any { it is Body })
    }

    @Test
    fun cancellingAttendanceDeletesTheEventIdWithAuthorizationAndWithoutABody() {
        val cancellation = EventiaApi::class.java.declaredMethods
            .singleOrNull { it.name == "cancelAttendance" }
        val parameters = cancellation?.parameterAnnotations.orEmpty().flatMap { it.toList() }
        assertEquals("events/{id}/attendance", cancellation?.getAnnotation(DELETE::class.java)?.value)
        assertEquals("id", parameters.filterIsInstance<Path>().single().value)
        assertEquals("Authorization", parameters.filterIsInstance<Header>().single().value)
        assertFalse(parameters.any { it is Body })
    }
}
