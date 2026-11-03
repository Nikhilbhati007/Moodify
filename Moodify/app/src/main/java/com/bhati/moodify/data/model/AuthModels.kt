package com.bhati.moodify.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    val message: String? = null,
    @SerializedName("access_token") val token: String? = null,
    val userId: String? = null,
    val error: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)
