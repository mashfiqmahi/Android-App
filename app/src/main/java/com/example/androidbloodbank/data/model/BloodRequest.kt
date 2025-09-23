package com.example.androidbloodbank.data.model

data class BloodRequest(
    val requesterName: String,
    val hospitalName: String,
    val locationName: String,
    val bloodGroup: BloodGroup,
    val phone: String,
    // NEW: date the blood is needed (epoch millis). Default keeps old saved data loading fine.
    val neededOnMillis: Long = System.currentTimeMillis()
)
