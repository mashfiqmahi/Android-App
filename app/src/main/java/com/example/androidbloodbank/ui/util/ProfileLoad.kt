package com.example.androidbloodbank.ui.util

import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.UserProfile
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Safely hydrates a UserProfile from LocalRepo, tolerant of old signup JSON. */
fun loadUserProfileSafely(repo: LocalRepo): UserProfile {
    val json = repo.loadCurrentUserJson()
    if (json.isNullOrBlank()) return UserProfile("", "", null, 0, "", "")
    return try {
        val root: JsonObject = JsonParser.parseString(json).asJsonObject
        UserProfile(
            name = root.get("name")?.asString ?: "",
            bloodGroup = root.get("bloodGroup")?.asString ?: "",
            lastDonationMillis = root.get("lastDonationMillis")?.takeIf { !it.isJsonNull }?.asLong,
            totalDonations = root.get("totalDonations")?.asInt ?: 0,
            contactNumber = root.get("contactNumber")?.asString
                ?: (root.get("phone")?.asString ?: ""),
            location = root.get("location")?.asString ?: ""
        )
    } catch (_: Exception) {
        UserProfile("", "", null, 0, "", "")
    }
}
