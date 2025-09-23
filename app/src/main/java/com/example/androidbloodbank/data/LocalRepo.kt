package com.example.androidbloodbank.data

import android.content.Context
import android.content.SharedPreferences
import com.example.androidbloodbank.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

class LocalRepo(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("abb_prefs", Context.MODE_PRIVATE)
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

    private fun <T> fromJson(json: String?, typeToken: TypeToken<T>): T? {
        if (json.isNullOrBlank()) return null
        return gson.fromJson(json, typeToken.type)
    }
    private fun toJson(value: Any?): String = gson.toJson(value)

    // ---- Donors ----
    fun loadDonors(): MutableList<Donor> {
        val json = prefs.getString(KEY_DONORS, null)
        return fromJson(json, object : TypeToken<MutableList<Donor>>() {}) ?: defaultDonors().toMutableList()
    }
    fun saveDonors(list: List<Donor>) {
        prefs.edit().putString(KEY_DONORS, toJson(list)).apply()
    }

    // ---- Users ----
    fun loadUsers(): MutableList<User> {
        val json = prefs.getString(KEY_USERS, null)
        return fromJson(json, object : TypeToken<MutableList<User>>() {}) ?: mutableListOf()
    }
    fun saveUsers(list: List<User>) {
        prefs.edit().putString(KEY_USERS, toJson(list)).apply()
    }

    // ---- Schedules ----
    fun loadSchedules(): MutableList<Schedule> {
        val json = prefs.getString(KEY_SCHEDULES, null)
        return fromJson(json, object : TypeToken<MutableList<Schedule>>() {}) ?: mutableListOf()
    }
    fun saveSchedules(list: List<Schedule>) {
        prefs.edit().putString(KEY_SCHEDULES, toJson(list)).apply()
    }

    // ---- Blood Requests (always-safe path) ----
    // ---- Blood Requests (safe loader + safer save) ----
    fun loadRequests(): MutableList<BloodRequest> {
        val raw = prefs.getString(KEY_REQUESTS, null) ?: return mutableListOf()
        return try {
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

                    // Accept both new & legacy keys
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
        } catch (_: Throwable) {
            mutableListOf()
        }
    }

    fun saveRequests(list: List<BloodRequest>) {
        // sanitize before saving
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




    fun logoutCurrentUser() { prefs.edit().remove(KEY_CURRENT_USER_JSON).apply() }



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
        // Try by label (toString() of enum returns label like "A+")
        BloodGroup.values().firstOrNull { it.toString() == v }?.let { return it }
        // Try by enum name (e.g., "A_POS")
        return runCatching { BloodGroup.valueOf(v) }.getOrElse { BloodGroup.O_POS }
    }

    // ---- Seed ----
    private fun defaultDonors() = listOf(
        Donor(name = "Rahim Uddin", bloodGroup = BloodGroup.O_NEG, phone = "01710000001", verified = true),
        Donor(name = "Ayesha Khan", bloodGroup = BloodGroup.A_POS, phone = "01710000002", verified = true),
        Donor(name = "Jahangir", bloodGroup = BloodGroup.B_NEG, phone = "01710000003"),
        Donor(name = "Mina Sultana", bloodGroup = BloodGroup.AB_NEG, phone = "01710000004"),
        Donor(name = "Sohan", bloodGroup = BloodGroup.O_POS, phone = "01710000005", verified = true),
    )
    // --- Session helpers (used by AppNavHost) ---
    fun loadCurrentUserJson(): String? = prefs.getString(KEY_CURRENT_USER_JSON, null)

    fun saveCurrentUserJson(json: String?) {
        if (json.isNullOrBlank()) prefs.edit().remove(KEY_CURRENT_USER_JSON).apply()
        else prefs.edit().putString(KEY_CURRENT_USER_JSON, json).apply()
    }

    fun hasLocalSession(): Boolean = !loadCurrentUserJson().isNullOrBlank()


}
