package com.example.androidbloodbank.data.model

import java.util.UUID

data class Donor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bloodGroup: BloodGroup,
    val phone: String? = null,
    val verified: Boolean = false,

    // NEW optional fields (so old data still works)
    val age: Int? = null,
    val city: String? = null,
    val area: String? = null,
    /** Epoch millis when this donor LAST donated. Null if unknown or new donor. */
    val lastDonationMillis: Long? = null,
    /** Preferred hospital/clinic or usual donation place */
    val hospital: String? = null
)
