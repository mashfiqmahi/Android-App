package com.example.androidbloodbank.data.model

import java.util.UUID

data class Donor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bloodGroup: BloodGroup,
    val phone: String? = null,
    val verified: Boolean = false
)
