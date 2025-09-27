package com.example.androidbloodbank.data.remote

import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

private const val DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"
class FirebaseRepo(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance(DB_URL).reference
) {
    private fun uid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    // ---- Profile (private under /users/{uid}/profile) ----
    suspend fun saveProfile(profile: UserProfile) {
        val data = mapOf(
            "name" to profile.name,
            "bloodGroup" to profile.bloodGroup,                // String like "A+"
            "lastDonationMillis" to profile.lastDonationMillis,
            "totalDonations" to profile.totalDonations,
            "contactNumber" to profile.contactNumber,
            "location" to profile.location,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        db.child("users").child(uid()).child("profile").setValue(data).await()
    }

    suspend fun loadProfile(): UserProfile? {
        val snap = db.child("users").child(uid()).child("profile").get().await()
        if (!snap.exists()) return null
        val name = snap.child("name").getValue(String::class.java) ?: return null
        val bloodGroup = snap.child("bloodGroup").getValue(String::class.java) ?: ""
        val lastDonation = snap.child("lastDonationMillis").getValue(Long::class.java)
        val totalDonations = snap.child("totalDonations").getValue(Int::class.java) ?: 0
        val contact = snap.child("contactNumber").getValue(String::class.java) ?: ""
        val location = snap.child("location").getValue(String::class.java) ?: ""
        return UserProfile(
            name = name,
            bloodGroup = bloodGroup,
            lastDonationMillis = lastDonation,
            totalDonations = totalDonations,
            contactNumber = contact,
            location = location
        )
    }

    // ---- Public donor card (readable by all under /donors_public/{uid}) ----
    suspend fun publishDonorCard(profile: UserProfile) {
        val public = mapOf(
            "name" to profile.name,
            "bloodGroup" to profile.bloodGroup,
            "phone" to profile.contactNumber,
            "location" to profile.location,
            "lastDonationMillis" to profile.lastDonationMillis,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        db.child("donors_public").child(uid()).setValue(public).await()
    }

    // ---- Blood requests (per-user under /requests/{uid}/{requestId}) ----
    suspend fun addBloodRequest(req: BloodRequest) {
        val key = db.child("requests").child(uid()).push().key
            ?: error("Failed to generate request key")
        val data = mapOf(
            "requesterName" to req.requesterName,
            "hospitalName" to req.hospitalName,
            "locationName" to req.locationName,
            "bloodGroup" to req.bloodGroup.toString(), // "A+", etc.
            "phone" to req.phone,
            "neededOnMillis" to req.neededOnMillis,
            "createdAt" to ServerValue.TIMESTAMP
        )
        db.child("requests").child(uid()).child(key).setValue(data).await()
    }

    fun observeMyRequests(onChange: (List<BloodRequest>) -> Unit): ValueEventListener {
        val myUid = runCatching { uid() }.getOrNull() ?: return NoopListener
        val ref = db.child("requests").child(myUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { s ->
                    val bgLabel = s.child("bloodGroup").getValue(String::class.java) ?: return@mapNotNull null
                    val bg = BloodGroup.values().firstOrNull { it.toString() == bgLabel } ?: return@mapNotNull null
                    BloodRequest(
                        requesterName = s.child("requesterName").getValue(String::class.java) ?: return@mapNotNull null,
                        hospitalName = s.child("hospitalName").getValue(String::class.java) ?: "",
                        locationName = s.child("locationName").getValue(String::class.java) ?: "",
                        bloodGroup = bg,
                        phone = s.child("phone").getValue(String::class.java) ?: "",
                        neededOnMillis = s.child("neededOnMillis").getValue(Long::class.java)
                            ?: System.currentTimeMillis()
                    )
                }
                onChange(list)
            }
            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    suspend fun listDonorsByGroup(groupCode: String): List<Donor> {
        val q = db.child("donors_public").orderByChild("bloodGroup").equalTo(groupCode)
        val snap = q.get().await()
        if (!snap.exists()) return emptyList()
        return snap.children.mapNotNull { s ->
            val name = s.child("name").getValue(String::class.java) ?: return@mapNotNull null
            val bgLabel = s.child("bloodGroup").getValue(String::class.java) ?: return@mapNotNull null
            val bg = BloodGroup.values().firstOrNull { it.toString() == bgLabel } ?: return@mapNotNull null
            Donor(
                name = name,
                bloodGroup = bg,
                phone = s.child("phone").getValue(String::class.java),
                city = s.child("location").getValue(String::class.java),
                lastDonationMillis = s.child("lastDonationMillis").getValue(Long::class.java)
            )
        }
    }

    companion object {
        private val NoopListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        }
    }
}
