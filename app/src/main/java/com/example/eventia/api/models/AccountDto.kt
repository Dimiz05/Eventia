package com.example.eventia.api.models

import com.google.gson.annotations.SerializedName

data class RegisterRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

class AccountDto(
    @SerializedName("email") val email: String?,
    @SerializedName("name") val name: String? = null
)
