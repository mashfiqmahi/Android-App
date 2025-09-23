package com.example.androidbloodbank.data.model

enum class RequestStatus { PENDING, MATCHED, CLOSED }

data class BloodRequest(
    val requesterName: String,
    val hospital: String,
    val location: String,
    val contactNumber: String,
    val bloodGroup: String,
    val neededDateMillis: Long = 0L,
    val timestamp: Long = 0L,
    val id: String? = null,
    val status: RequestStatus = RequestStatus.PENDING // NEW
)
