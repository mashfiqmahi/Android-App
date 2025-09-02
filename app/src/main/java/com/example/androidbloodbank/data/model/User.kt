package com.example.androidbloodbank.data.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val bloodGroup: BloodGroup
)
