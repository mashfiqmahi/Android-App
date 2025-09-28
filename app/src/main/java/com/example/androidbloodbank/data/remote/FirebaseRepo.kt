package com.example.androidbloodbank.data.remote

import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Failover-aware Firebase repo.
 * 1) Tries the explicit Singapore RTDB first (DB_URL).
 * 2) If that fails (rules/region mismatch), falls back to the project's default RTDB.
 *
 * This prevents the “data saved to US DB” vs “console open on Singapore DB” mismatch.
 */
private const val DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

class FirebaseRepo(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Try explicit regional DB first, then default DB from google-services.json
    private fun dbRefs(): List<DatabaseReference> = listOf(
        FirebaseDatabase.getInstance(DB_URL).reference,
        FirebaseDatabase.getInstance().reference
    )

    // ---------- PROFILE ----------
    suspend fun saveProfile(p: UserProfile) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val data = mapOf(
            "name" to p.name,
            "bloodGroup" to p.bloodGroup,
            "lastDonationMillis" to (p.lastDonationMillis ?: 0L),
            "totalDonations" to p.totalDonations,
            "contactNumber" to p.contactNumber,
            "location" to p.location
        )
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                root.child("users").child(uid).child("profile").setValue(data).await()
                return
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    suspend fun publishDonorCard(p: UserProfile) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val data = mapOf(
            "name" to p.name,
            "bloodGroup" to p.bloodGroup,
            "phone" to p.contactNumber,
            "city" to p.location,
            "lastDonationMillis" to (p.lastDonationMillis ?: 0L),
            "updatedAt" to System.currentTimeMillis()
        )
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                root.child("donors_public").child(uid).setValue(data).await()
                return
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    // ---------- DONORS ----------
    suspend fun listDonorsByGroup(groupLabel: String): List<Donor> {
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val snap = root.child("donors_public").get().await()
                val out = mutableListOf<Donor>()
                for (child in snap.children) {
                    val bgStr = child.child("bloodGroup").getValue(String::class.java) ?: ""
                    if (bgStr == groupLabel) {
                        out.add(
                            Donor(
                                id = child.key.orEmpty(),
                                name = child.child("name").getValue(String::class.java) ?: "Unknown",
                                bloodGroup = parseBg(bgStr),
                                phone = child.child("phone").getValue(String::class.java),
                                city = child.child("city").getValue(String::class.java),
                                lastDonationMillis = child.child("lastDonationMillis").getValue(Long::class.java)
                            )
                        )
                    }
                }
                return out
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    // ---------- REQUESTS: hard-owned under /requests/{uid}/{id} ----------
    /** Create request and return autoId (writes ownerUid for rules) */
    suspend fun addBloodRequest(req: BloodRequest): String {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val payload = req.toMap(ownerUid = uid)
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val ref = root.child("requests").child(uid).push()
                ref.setValue(payload).await()
                return ref.key.orEmpty()
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    /** List ONLY my requests */
    suspend fun listMyRequests(): List<Pair<String, BloodRequest>> {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val snap = root.child("requests").child(uid).get().await()
                return snap.children.mapNotNull { node ->
                    val id = node.key ?: return@mapNotNull null
                    id to node.toRequest()
                }.sortedBy { it.second.neededOnMillis }
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    /** Overwrite existing (no push) */
    suspend fun updateBloodRequest(id: String, updated: BloodRequest) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val payload = updated.toMap(ownerUid = uid)
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                root.child("requests").child(uid).child(id).setValue(payload).await()
                return
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    /** Delete existing */
    suspend fun deleteBloodRequest(id: String) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                root.child("requests").child(uid).child(id).removeValue().await()
                return
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

    /** Remove expired (neededOnMillis < now); returns count removed */
    suspend fun cleanupExpiredRequests(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        val now = System.currentTimeMillis()
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val userRef = root.child("requests").child(uid)
                val snap = userRef.get().await()
                var removed = 0
                for (c in snap.children) {
                    val needed = c.child("neededOnMillis").getValue(Long::class.java) ?: 0L
                    if (needed in 1 until now) {
                        userRef.child(c.key!!).removeValue().await()
                        removed++
                    }
                }
                return removed
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }
}

// ---------- helpers ----------
private fun parseBg(label: String): BloodGroup = when (label) {
    "A+" -> BloodGroup.A_POS
    "A-" -> BloodGroup.A_NEG
    "B+" -> BloodGroup.B_POS
    "B-" -> BloodGroup.B_NEG
    "AB+" -> BloodGroup.AB_POS
    "AB-" -> BloodGroup.AB_NEG
    "O-" -> BloodGroup.O_NEG
    else -> BloodGroup.O_POS
}

private fun BloodRequest.toMap(ownerUid: String): Map<String, Any?> = mapOf(
    "ownerUid" to ownerUid,   // <-- REQUIRED by rules
    "requesterName" to requesterName,
    "hospitalName" to hospitalName,
    "locationName" to locationName,
    "bloodGroup" to bloodGroup.toString(),
    "phone" to phone,
    "neededOnMillis" to neededOnMillis,
    "createdAt" to System.currentTimeMillis()
)

private fun DataSnapshot.toRequest(): BloodRequest {
    val groupStr = child("bloodGroup").getValue(String::class.java) ?: "O+"
    return BloodRequest(
        requesterName = child("requesterName").getValue(String::class.java) ?: "",
        hospitalName  = child("hospitalName").getValue(String::class.java) ?: "",
        locationName  = child("locationName").getValue(String::class.java) ?: "",
        bloodGroup    = parseBg(groupStr),
        phone         = child("phone").getValue(String::class.java) ?: "",
        neededOnMillis= child("neededOnMillis").getValue(Long::class.java) ?: 0L
    )
}
