package com.example.eventia

import com.example.eventia.api.EventiaApi
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class EventiaApiTest {
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

        assertEquals(setOf("categories", "events", "venues"), routes)
    }
}
