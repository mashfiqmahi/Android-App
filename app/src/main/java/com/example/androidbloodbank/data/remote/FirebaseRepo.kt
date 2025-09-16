package com.example.androidbloodbank.data.remote

import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await

/**
 * Realtime Database layout
 *
 * /users/{uid}/profile           -> private full profile (only this user can read/write)
 * /donors_public/{uid}           -> public donor card (visible to everyone for search)
 * /requests/{uid}/{requestId}    -> (optional) user’s requests
 */
class FirebaseRepo {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private fun uid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

    // Map enum -> stable string for DB
    private fun BloodGroup.toCode(): String = name   // e.g. O_POS, A_NEG, ...

    // ---------- PROFILE UPSERT ----------
    suspend fun upsertProfile(
        name: String,
        email: String?,
        phone: String?,
        donor: Donor
    ) {
        val uid = uid()

        // Private profile (full)
        val profile = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "bloodGroup" to donor.bloodGroup.toCode(),
            "age" to donor.age,
            "city" to (donor.city ?: ""),
            "area" to (donor.area ?: ""),
            "hospital" to (donor.hospital ?: ""),
            "lastDonationMillis" to (donor.lastDonationMillis ?: 0L),
            "verified" to donor.verified,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        // Public donor card (no sensitive fields)
        val public = hashMapOf(
            "uid" to uid,
            "name" to name,
            "bloodGroup" to donor.bloodGroup.toCode(),
            "city" to (donor.city ?: ""),
            "area" to (donor.area ?: ""),
            "lastDonationMillis" to (donor.lastDonationMillis ?: 0L),
            "verified" to donor.verified,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        // Write both in a single multi-path update
        val updates = hashMapOf<String, Any>(
            "/users/$uid/profile" to profile,
            "/donors_public/$uid" to public
        )
        db.updateChildren(updates).await()
    }

    // ---------- LOAD PROFILE (optional helper) ----------
    suspend fun loadProfile(): Map<String, Any?>? {
        val snap = db.child("users").child(uid()).child("profile").get().await()
        return if (snap.exists()) (snap.value as? Map<String, Any?>) else null
    }

    // ---------- (Optional) Donors query by group ----------
    suspend fun listDonorsByGroup(groupCode: String): List<Map<String, Any?>> {
        // Simple query by child; add indexOn in rules for performance
        val q = db.child("donors_public").orderByChild("bloodGroup").equalTo(groupCode)
        val snap = q.get().await()
        if (!snap.exists()) return emptyList()
        return snap.children.mapNotNull { it.value as? Map<String, Any?> }
    }
}
