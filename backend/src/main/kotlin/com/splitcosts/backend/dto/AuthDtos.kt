package com.splitcosts.backend.dto

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val name: String
)
