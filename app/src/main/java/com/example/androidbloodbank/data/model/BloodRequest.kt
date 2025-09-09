package com.example.androidbloodbank.data.model

enum class RequestStatus { PENDING, MATCHED, CLOSED }

data class BloodRequest(
    val requesterName: String,
    val hospital: String,
    val location: String,
    val contactNumber: String,
    val bloodGroup: String,
    val status: RequestStatus = RequestStatus.PENDING // NEW
)
