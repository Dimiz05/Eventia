package com.example.eventia.api.models

import com.google.gson.annotations.SerializedName

data class CategoryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)
