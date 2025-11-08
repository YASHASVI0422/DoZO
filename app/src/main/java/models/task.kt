package com.campus.dozo.models

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "",
    var status: String = "",
    var postedBy: String = "",
    var postedByName: String = "",
    var createdAt: Long = 0L,
    var acceptedBy: String = "",
    var acceptedByName: String = "",
    var acceptedAt: Long = 0L,
    var completedAt: Long = 0L
)
