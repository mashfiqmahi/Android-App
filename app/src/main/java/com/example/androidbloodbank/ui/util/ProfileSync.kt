package com.example.androidbloodbank.ui.util

import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.UserProfile
import com.example.androidbloodbank.data.remote.FirebaseRepo
import com.google.gson.Gson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

// Use your regioned Realtime Database URL
private const val DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

/**
 * Saves to SharedPreferences (local) and to Realtime Database (cloud).
 * Private profile gets the extra fields: email, gender, age, lastDonationMillis.
 * Public donor card remains minimal (name, bloodGroup, phone, location, lastDonationMillis).
 */
suspend fun saveUserProfile(
    repo: LocalRepo,
    name: String,
    bloodGroup: String?,
    contact: String?,
    location: String?,
    lastDonationMillis: Long?,
    totalDonations: Int?,
    email: String?,           // ✅ new
    gender: String?,          // ✅ new
    age: Int?,                // ✅ new
    onDone: (Boolean, String) -> Unit = { _, _ -> }
) {
    val profile = UserProfile(
        name = name.trim(),
        bloodGroup = (bloodGroup ?: "").trim(),
        lastDonationMillis = lastDonationMillis,
        totalDonations = totalDonations ?: 0,
        contactNumber = (contact ?: "").trim(),
        location = (location ?: "").trim()
    )

    // Local snapshot (so your UI keeps using it)
    runCatching { repo.saveCurrentUserJson(Gson().toJson(profile)) }

    val firebase = FirebaseRepo()
    val ok = withTimeoutOrNull(15_000) {
        runCatching {
            // Base profile + public donor card
            firebase.saveProfile(profile)
            firebase.publishDonorCard(profile)

            // Extra fields in the PRIVATE profile
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: error("Not signed in")
            val extras = mutableMapOf<String, Any?>(
                "email" to (email?.trim() ?: ""),
                "gender" to (gender?.trim() ?: ""),
                "age" to (age ?: 0),
                "lastDonationMillis" to (lastDonationMillis ?: 0L),
                "updatedAt" to ServerValue.TIMESTAMP
            )
            FirebaseDatabase.getInstance(DB_URL).reference
                .child("users").child(uid).child("profile")
                .updateChildren(extras as Map<String, Any?>).await()
        }.isSuccess
    } ?: false

    onDone(ok, if (ok) "Profile saved." else "Saved locally. Network failed.")
}
