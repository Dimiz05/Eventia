package com.example.eventia.api

import com.example.eventia.api.models.CategoryDto
import com.example.eventia.api.models.EventDto
import com.example.eventia.api.models.LoginRequestDto
import com.example.eventia.api.models.LoginResponseDto
import com.example.eventia.api.models.AccountDto
import com.example.eventia.api.models.RegisterRequestDto
import com.example.eventia.api.models.VenueDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface EventiaApi {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("events")
    suspend fun getEvents(): List<EventDto>

    @GET("venues")
    suspend fun getVenues(): List<VenueDto>

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequestDto): LoginResponseDto

    @POST("auth/register")
    suspend fun register(@Body account: RegisterRequestDto): LoginResponseDto

    @GET("me")
    suspend fun getMe(@Header("Authorization") authorization: String): AccountDto
}
