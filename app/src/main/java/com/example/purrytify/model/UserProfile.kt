package com.example.purrytify.model

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val profilePhoto: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String
)