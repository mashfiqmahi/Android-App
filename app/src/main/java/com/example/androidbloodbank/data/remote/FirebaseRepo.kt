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
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.tasks.await
/**
 * Failover-aware Firebase repo.
 * 1) Tries the explicit Singapore RTDB first (DB_URL).
 * 2) If that fails (rules/region mismatch), falls back to the project's default RTDB.
 *
 * This prevents the "data saved to US DB" vs "console open on Singapore DB" mismatch.
 */
private const val DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"
class FirebaseRepo(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Ensure we have an authenticated session (anonymous is fine for read rules)
    private suspend fun ensureAuth() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }
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
    suspend fun publishDonorCard(
        p: UserProfile,
        district: String? = null // pass the selected district from your dropdown
    ) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")

        val data = mapOf(
            "name" to p.name,
            "bloodGroup" to p.bloodGroup,
            "phone" to p.contactNumber,
            "location" to p.location,            // ✅ renamed from "city"
            "district" to district,              // ✅ NEW
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
    suspend fun listDonorsByGroup(groupLabel: String, district: String? = null): List<Donor> {
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val snap = root.child("donors_public").get().await()
                val out = mutableListOf<Donor>()
                for (child in snap.children) {
                    val bgStr = child.child("bloodGroup").getValue(String::class.java) ?: ""

                    // Read fields
                    val locationStr = child.child("location").getValue(String::class.java)
                        ?: child.child("city").getValue(String::class.java) // backward compat read
                    val districtStr = child.child("district").getValue(String::class.java)

                    if (bgStr == groupLabel) {
                        // ✅ STRICT district filter (no fallback to location)
                        if (district != null) {
                            if (districtStr == null || !districtStr.equals(district, ignoreCase = true)) {
                                continue
                            }
                        }

                        out.add(
                            Donor(
                                id = child.key.orEmpty(),
                                name = child.child("name").getValue(String::class.java) ?: "Unknown",
                                bloodGroup = parseBg(bgStr),
                                phone = child.child("phone").getValue(String::class.java),
                                // Donor.city used by UI -> feed from NEW 'location' (or old 'city' if missing)
                                city = locationStr,
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


    // List ALL users (reads users/{uid}/profile) and maps to Donor UI model
    // List ALL users as Donor models from users/*/profile.
// Tries users first; if empty, falls back to donors_public.
    suspend fun listAllUsers(): List<Donor> {
        ensureAuth() // <-- important for rules requiring auth
        // Try users/*/profile
        run {
            var last: Exception? = null
            for (root in dbRefs()) {
                try {
                    val snap = root.child("users").get().await()
                    val out = mutableListOf<Donor>()
                    for (userNode in snap.children) {
                        val prof = userNode.child("profile")
                        if (!prof.exists()) continue
                        val name  = prof.child("name").getValue(String::class.java) ?: "User"
                        val bgStr = prof.child("bloodGroup").getValue(String::class.java) ?: ""
                        val phone = prof.child("phone").getValue(String::class.java)
                            ?: prof.child("contactNumber").getValue(String::class.java)
                        val city  = prof.child("city").getValue(String::class.java)
                            ?: prof.child("location").getValue(String::class.java)
                        val hospital = prof.child("hospital").getValue(String::class.java)
                            ?: prof.child("hospitalName").getValue(String::class.java)
                        val lastDonation = prof.child("lastDonationMillis").getValue(Long::class.java)
                        val verified = prof.child("verified").getValue(Boolean::class.java) ?: false
                        out.add(
                            Donor(
                                id = userNode.key.orEmpty(),
                                name = name,
                                bloodGroup = parseBgFlexible(bgStr),
                                phone = phone,
                                city = city,
                                hospital = hospital,
                                lastDonationMillis = lastDonation,
                                verified = verified
                            )
                        )
                    }
                    if (out.isNotEmpty()) return out
                    // else fall through to donors_public
                } catch (e: Exception) { last = e }
            }
            // ignore last; attempt donors_public next
        }
        // Fallback: donors_public/*
        return listAllDonorsPublic()
    }
    // Public donors: donors_public/*  (no group filter)
    suspend fun listAllDonorsPublic(): List<Donor> {
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val snap = root.child("donors_public").get().await()
                val out = mutableListOf<Donor>()
                for (child in snap.children) {
                    val bgStr = child.child("bloodGroup").getValue(String::class.java) ?: ""
                    val locationStr = child.child("location").getValue(String::class.java)
                        ?: child.child("city").getValue(String::class.java) // backward compat

                    out.add(
                        Donor(
                            id = child.key.orEmpty(),
                            name = child.child("name").getValue(String::class.java) ?: "Unknown",
                            bloodGroup = parseBgFlexible(bgStr),
                            phone = child.child("phone").getValue(String::class.java),
                            city = locationStr,
                            lastDonationMillis = child.child("lastDonationMillis").getValue(Long::class.java)
                        )
                    )
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
    suspend fun listActivePublicRequests(nowMs: Long = System.currentTimeMillis())
            : List<Pair<String, BloodRequest>> {
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val snap = root.child("requests_public")
                    .orderByChild("neededOnMillis")
                    .startAt(nowMs.toDouble())        // RTDB range uses Double
                    .get()
                    .await()
                return snap.children.mapNotNull { c ->
                    val id = c.key ?: return@mapNotNull null
                    id to DataSnapshotToRequestPublic(c)
                }.sortedBy { it.second.neededOnMillis }
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }
    // helper to read what you write in PostRequestScreen
    private fun DataSnapshotToRequestPublic(c: com.google.firebase.database.DataSnapshot): BloodRequest {
        val groupStr = c.child("bloodGroup").getValue(String::class.java) ?: "O+"
        return BloodRequest(
            requesterName   = c.child("requesterName").getValue(String::class.java) ?: "",
            hospitalName    = c.child("hospitalName").getValue(String::class.java) ?: "",
            locationName    = c.child("locationName").getValue(String::class.java) ?: "",
            bloodGroup      = parseBgFlexible(groupStr),
            phone           = c.child("phone").getValue(String::class.java) ?: "",
            neededOnMillis  = c.child("neededOnMillis").getValue(Long::class.java) ?: 0L
        )
    }
    // Automatic delete expired requests of current user when user sign in
    suspend fun cleanupExpiredRequestsForCurrentUser(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        val now = System.currentTimeMillis()
        var removed = 0
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                val userRef = root.child("requests").child(uid)
                val snap = userRef.get().await()
                for (c in snap.children) {
                    val needed = c.child("neededOnMillis").getValue(Long::class.java) ?: 0L
                    if (needed in 1 until now) {
                        val id = c.key ?: continue
                        userRef.child(id).removeValue().await()
                        root.child("requests_public").child(id).removeValue().await()
                        removed++
                    }
                }
                return removed
            } catch (e: Exception) { last = e }
        }
        if (last != null) throw last
        return removed
    }

    suspend fun markRequestFulfilled(id: String) {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        var last: Exception? = null
        for (root in dbRefs()) {
            try {
                // mark private as fulfilled
                root.child("requests").child(uid).child(id)
                    .child("fulfilledAt").setValue(System.currentTimeMillis()).await()
                // remove from public feed
                root.child("requests_public").child(id).removeValue().await()
                return
            } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("Unknown DB error")
    }

}
// ---------- helpers ----------
private fun parseBgFlexible(raw: String): BloodGroup {
    val s = raw.trim().uppercase()
    // Common label forms
    when (s) {
        "A+" -> return BloodGroup.A_POS
        "A-" -> return BloodGroup.A_NEG
        "B+" -> return BloodGroup.B_POS
        "B-" -> return BloodGroup.B_NEG
        "AB+" -> return BloodGroup.AB_POS
        "AB-" -> return BloodGroup.AB_NEG
        "O+" -> return BloodGroup.O_POS
        "O-" -> return BloodGroup.O_NEG
    }
    // Enum-like forms (what saveProfile might have written)
    return when (s) {
        "A_POS", "A POS", "A_POSITIVE" -> BloodGroup.A_POS
        "A_NEG", "A NEG", "A_NEGATIVE" -> BloodGroup.A_NEG
        "B_POS", "B POS", "B_POSITIVE" -> BloodGroup.B_POS
        "B_NEG", "B NEG", "B_NEGATIVE" -> BloodGroup.B_NEG
        "AB_POS","AB POS","AB_POSITIVE"-> BloodGroup.AB_POS
        "AB_NEG","AB NEG","AB_NEGATIVE"-> BloodGroup.AB_NEG
        "O_POS", "O POS", "O_POSITIVE" -> BloodGroup.O_POS
        "O_NEG", "O NEG", "O_NEGATIVE" -> BloodGroup.O_NEG
        else -> BloodGroup.O_POS
    }
}
// Back-compat shim for older call sites
private fun parseBg(label: String): BloodGroup = parseBgFlexible(label)
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