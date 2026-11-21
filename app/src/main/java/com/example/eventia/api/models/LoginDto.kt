package com.example.eventia.api.models

import com.google.gson.annotations.SerializedName

class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

class LoginResponseDto(
    @SerializedName("token") val token: String
)
