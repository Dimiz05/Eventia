package com.example.eventia.repositories

import com.example.eventia.api.EventiaApi
import com.example.eventia.api.mappers.toDomain
import com.example.eventia.api.models.EventDto
import com.example.eventia.data.local.SessionStore
import com.example.eventia.domain.model.Event
import javax.inject.Inject

internal class AttendanceSessionUnavailable : Exception()

interface AttendanceRepository {
    suspend fun getMyAttendances(): List<Event>
}

class RemoteAttendanceRepository @Inject constructor(
    private val api: EventiaApi,
    private val store: SessionStore
) : AttendanceRepository {
    override suspend fun getMyAttendances(): List<Event> {
        val token = store.readToken()?.takeIf(String::isNotBlank)
            ?: throw AttendanceSessionUnavailable()
        return api.getMyAttendances("Bearer $token").map(EventDto::toDomain)
    }
}
