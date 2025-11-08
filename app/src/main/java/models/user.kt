package com.campus.dozo.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val rating: Float = 0f,
    val tasksCompleted: Int = 0,
    val tasksPosted: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)