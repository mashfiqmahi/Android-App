package com.example.androidbloodbank.data

import android.content.Context
import android.content.SharedPreferences
import com.example.androidbloodbank.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

/**
 * Single-source LocalRepo. Holds one prefs, one db, one auth. No duplicates.
 */
class LocalRepo(
    private val appContext: Context,
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(SG_DB_URL),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("abb_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val KEY_DONORS = "donors_json"
        private const val KEY_USERS = "users_json"
        private const val KEY_SCHEDULES = "schedules_json"
        private const val KEY_REQUESTS = "blood_requests_json"

        // session/profile snippets
        private const val KEY_CURRENT_USER_JSON = "current_user_json"
        private const val KEY_MY_DONOR = "my_donor_json"
    }

    /* ---------- generic json helpers ---------- */
    private fun <T> fromJson(json: String?, typeToken: TypeToken<T>): T? {
        if (json.isNullOrBlank()) return null
        return gson.fromJson(json, typeToken.type)
    }
    private fun toJson(value: Any?): String = gson.toJson(value)

    /* ---------- Load current user json ---------- */
    fun loadCurrentUserJson(): String? {
        return prefs.getString(KEY_CURRENT_USER_JSON, null)
    }

    fun saveCurrentUserJson(json: String?) {
        if (json.isNullOrBlank()) {
            prefs.edit().remove(KEY_CURRENT_USER_JSON).apply()
        } else {
            prefs.edit().putString(KEY_CURRENT_USER_JSON, json).apply()
        }
    }

    /* ---------- Donors ---------- */
    fun loadDonors(): MutableList<Donor> {
        val json = prefs.getString(KEY_DONORS, null)
        return fromJson(json, object : TypeToken<MutableList<Donor>>() {}) ?: defaultDonors().toMutableList()
    }
    fun saveDonors(list: List<Donor>) {
        prefs.edit().putString(KEY_DONORS, toJson(list)).apply()
    }

    /* ---------- Users ---------- */
    fun loadUsers(): MutableList<User> {
        val json = prefs.getString(KEY_USERS, null)
        return fromJson(json, object : TypeToken<MutableList<User>>() {}) ?: mutableListOf()
    }
    fun saveUsers(list: List<User>) {
        prefs.edit().putString(KEY_USERS, toJson(list)).apply()
    }

    /* ---------- Schedules ---------- */
    fun loadSchedules(): MutableList<Schedule> {
        val json = prefs.getString(KEY_SCHEDULES, null)
        return fromJson(json, object : TypeToken<MutableList<Schedule>>() {}) ?: mutableListOf()
    }
    fun saveSchedules(list: List<Schedule>) {
        prefs.edit().putString(KEY_SCHEDULES, toJson(list)).apply()
    }

    /* ---------- Blood Requests (local cache, tolerant parser) ---------- */
    fun loadRequests(): MutableList<BloodRequest> {
        val raw = prefs.getString(KEY_REQUESTS, null) ?: return mutableListOf()
        return runCatching {
            val arr = com.google.gson.JsonParser.parseString(raw).asJsonArray
            arr.mapNotNull { el ->
                runCatching {
                    val o = el.asJsonObject

                    fun s(vararg k: String): String {
                        for (key in k) if (o.has(key) && !o.get(key).isJsonNull) return o.get(key).asString
                        return ""
                    }
                    fun l(vararg k: String): Long {
                        for (key in k) if (o.has(key) && !o.get(key).isJsonNull) {
                            val p = o.get(key)
                            if (p.isJsonPrimitive) {
                                val prim = p.asJsonPrimitive
                                if (prim.isNumber) return prim.asLong
                                if (prim.isString) return prim.asString.toLongOrNull() ?: 0L
                            }
                        }
                        return 0L
                    }
                    fun parseGroup(v: String): BloodGroup {
                        val t = v.trim()
                        BloodGroup.values().firstOrNull { it.toString() == t }?.let { return it }
                        return runCatching { BloodGroup.valueOf(t) }.getOrElse { BloodGroup.O_POS }
                    }

                    val name     = s("requesterName", "name")
                    val hospital = s("hospitalName", "hospital")
                    val location = s("locationName", "location", "address")
                    val phone    = s("phone", "contact", "contactNumber")
                    val groupRaw = s("bloodGroup", "group").ifEmpty { "O+" }
                    val needMs   = l("neededOnMillis","neededDateMillis","needDate","dateNeeded","requiredOn")
                        .let { if (it > 0L) it else System.currentTimeMillis() }

                    BloodRequest(
                        requesterName  = name,
                        hospitalName   = hospital,
                        locationName   = location,
                        phone          = phone,
                        bloodGroup     = parseGroup(groupRaw),
                        neededOnMillis = needMs
                    )
                }.getOrNull()
            }.filterNotNull().toMutableList()
        }.getOrElse { mutableListOf() }
    }

    fun saveRequests(list: List<BloodRequest>) {
        val cleaned = list.map { r ->
            r.copy(
                requesterName  = r.requesterName.ifEmpty { "" },
                hospitalName   = r.hospitalName.ifEmpty { "" },
                locationName   = r.locationName.ifEmpty { "" },
                phone          = r.phone.ifEmpty { "" },
                neededOnMillis = if (r.neededOnMillis > 0L) r.neededOnMillis else System.currentTimeMillis()
            )
        }
        prefs.edit().putString(KEY_REQUESTS, gson.toJson(cleaned)).apply()
    }

    fun logoutCurrentUser() {
        prefs.edit().remove(KEY_CURRENT_USER_JSON).apply()
    }

    /* ---------- small JSON helpers used above ---------- */
    private fun firstString(o: JsonObject, vararg keys: String): String? {
        for (k in keys) if (o.has(k) && !o.get(k).isJsonNull) return o.get(k).asString
        return null
    }
    private fun firstLong(o: JsonObject, vararg keys: String): Long? {
        for (k in keys) if (o.has(k) && !o.get(k).isJsonNull) {
            val e = o.get(k)
            if (e.isJsonPrimitive) {
                val p = e.asJsonPrimitive
                if (p.isNumber) return p.asLong
                if (p.isString) return p.asString.toLongOrNull()
            }
        }
        return null
    }
    private fun parseBloodGroup(value: String): BloodGroup {
        val v = value.trim()
        BloodGroup.values().firstOrNull { it.toString() == v }?.let { return it }
        return runCatching { BloodGroup.valueOf(v) }.getOrElse { BloodGroup.O_POS }
    }

    /* ---------- Seeds ---------- */
    private fun defaultDonors() = listOf(
        Donor(name = "Rahim Uddin", bloodGroup = BloodGroup.O_NEG, phone = "01710000001", verified = true),
        Donor(name = "Ayesha Khan", bloodGroup = BloodGroup.A_POS, phone = "01710000002", verified = true),
        Donor(name = "Jahangir", bloodGroup = BloodGroup.B_NEG, phone = "01710000003"),
        Donor(name = "Mina Sultana", bloodGroup = BloodGroup.AB_NEG, phone = "01710000004"),
        Donor(name = "Sohan", bloodGroup = BloodGroup.O_POS, phone = "01710000005", verified = true),
    )

    /* =========================================================================
       Firebase: get + update a single request
       - Read from /requests_public/{requestId} (public mirror)
       - Update both /requests_public/{requestId} and /requests/{ownerUid}/{requestId}
       - Never touch createdAt/ownerUid (rules forbid changing them)
       ========================================================================= */

    suspend fun getBloodRequestById(requestId: String): BloodRequest? {
        val ref = db.reference.child("requests_public").child(requestId)
        val snap = ref.get().await()
        if (!snap.exists()) return null

        val requesterName  = snap.child("requesterName").getValue(String::class.java) ?: ""
        val hospitalName   = snap.child("hospitalName").getValue(String::class.java) ?: ""
        val locationName   = snap.child("locationName").getValue(String::class.java) ?: ""
        val phone          = snap.child("phone").getValue(String::class.java) ?: ""
        val groupStr       = snap.child("bloodGroup").getValue(String::class.java) ?: "O+"
        val neededOnMillis = snap.child("neededOnMillis").getValue(Long::class.java) ?: 0L

        return BloodRequest(
            requesterName = requesterName,
            hospitalName = hospitalName,
            locationName = locationName,
            phone = phone,
            bloodGroup = parseBloodGroup(groupStr),
            neededOnMillis = if (neededOnMillis > 0) neededOnMillis else System.currentTimeMillis()
        )
    }

    suspend fun updateBloodRequest(requestId: String, updated: BloodRequest) {
        val currentUid = auth.currentUser?.uid ?: error("Not signed in")

        // Find the owner from public mirror
        val ownerUid = db.reference
            .child("requests_public").child(requestId).child("ownerUid")
            .get().await()
            .getValue(String::class.java)
            ?: throw IllegalStateException("Request not found")

        // Only allow owner to edit (matches your rules)
        if (ownerUid != currentUid) throw SecurityException("You can only edit your own request")

        // Only fields allowed to change
        val writable = mapOf(
            "requesterName" to updated.requesterName,
            "hospitalName"  to updated.hospitalName,
            "locationName"  to updated.locationName,
            "phone"         to updated.phone
            // If you decide to allow these too, uncomment:
            // "bloodGroup"     to updated.bloodGroup.toString(),
            // "neededOnMillis" to updated.neededOnMillis
        )

        val updates = hashMapOf<String, Any?>()
        for ((k, v) in writable) {
            updates["/requests_public/$requestId/$k"] = v
            updates["/requests/$ownerUid/$requestId/$k"] = v
        }

        db.reference.updateChildren(updates).await()
    }
}
