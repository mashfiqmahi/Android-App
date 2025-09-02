package com.example.androidbloodbank.data.model

import java.util.concurrent.TimeUnit

data class UserProfile(
    var name: String,
    var bloodGroup: String,
    var lastDonationMillis: Long?,  // timestamp
    var totalDonations: Int,
    var contactNumber: String,
    var location: String
) {
    // Eligible if 90+ days passed
    val isEligible: Boolean
        get() = lastDonationMillis?.let {
            val diff = System.currentTimeMillis() - it
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            days >= 90
        } ?: true

    // Remaining days until next donation
    val daysRemaining: Long
        get() = lastDonationMillis?.let {
            val nextEligible = it + TimeUnit.DAYS.toMillis(90)
            val diff = nextEligible - System.currentTimeMillis()
            if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else 0
        } ?: 0
}
