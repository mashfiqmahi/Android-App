package com.example.androidbloodbank.data.model

data class BloodRequest(
    val requesterName: String,
    val hospital: String,
    val location: String,
    val contactNumber: String,
    val bloodGroup: String
)
