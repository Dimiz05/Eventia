package com.example.eventia.api

import com.example.eventia.api.models.CategoryDto
import retrofit2.http.GET

interface EventiaApi {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>
}
