package com.example.androidbloodbank.data.model

data class BloodRequest(
    val requesterName: String,
    val hospitalName: String,
    val locationName: String,
    val bloodGroup: BloodGroup,
    val phone: String,
    val neededOnMillis: Long
) {
    // Convert BloodRequest to a Map for Firebase
    fun toMap(): Map<String, Any> {
        return mapOf(
            "requesterName" to requesterName,
            "hospitalName" to hospitalName,
            "locationName" to locationName,
            "bloodGroup" to bloodGroup.toString(),
            "phone" to phone,
            "neededOnMillis" to neededOnMillis
        )
    }
}
