package com.example.eventia.api

import com.example.eventia.api.models.AccountDto
import com.example.eventia.api.models.CategoryDto
import com.example.eventia.api.models.EventDto
import com.example.eventia.api.models.LoginRequestDto
import com.example.eventia.api.models.LoginResponseDto
import com.example.eventia.api.models.RegisterRequestDto
import com.example.eventia.api.models.VenueDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EventiaApi {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("events")
    suspend fun getEvents(
        @Query("q") query: String? = null,
        @Query("category") categoryId: Long? = null
    ): List<EventDto>

    @GET("events/{id}")
    suspend fun getEvent(
        @Path("id") id: Long,
        @Header("Authorization") authorization: String? = null
    ): EventDto

    @POST("events/{id}/attendance")
    suspend fun attendEvent(
        @Path("id") id: Long,
        @Header("Authorization") authorization: String
    ): Response<Void>

    @DELETE("events/{id}/attendance")
    suspend fun cancelAttendance(
        @Path("id") id: Long,
        @Header("Authorization") authorization: String
    ): Response<Void>

    @GET("venues")
    suspend fun getVenues(): List<VenueDto>

    @GET("venues/{id}")
    suspend fun getVenue(@Path("id") id: Long): VenueDto

    @GET("venues/{id}/events")
    suspend fun getVenueEvents(@Path("id") id: Long): List<EventDto>

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequestDto): LoginResponseDto

    @POST("auth/register")
    suspend fun register(@Body account: RegisterRequestDto): LoginResponseDto

    @GET("me")
    suspend fun getMe(@Header("Authorization") authorization: String): AccountDto

    @GET("me/attendances")
    suspend fun getMyAttendances(@Header("Authorization") authorization: String): List<EventDto>
}
