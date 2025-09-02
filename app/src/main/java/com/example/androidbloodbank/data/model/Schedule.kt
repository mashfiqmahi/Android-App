package com.example.androidbloodbank.data.model

import java.util.UUID

data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val donorId: String,
    val dateIso: String,
    val notes: String? = null
)
