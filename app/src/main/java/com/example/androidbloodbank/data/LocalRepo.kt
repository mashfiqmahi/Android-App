package com.example.androidbloodbank.data

import android.content.Context
import android.content.SharedPreferences
import com.example.androidbloodbank.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalRepo(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("abb_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DONORS = "donors_json"
        private const val KEY_USERS = "users_json"
        private const val KEY_SCHEDULES = "schedules_json"
        private const val KEY_REQUESTS = "requests_json" // NEW 👈
        private const val KEY_MY_DONOR = "my_donor_json" // NEW
        private const val KEY_CURRENT_USER_JSON = "current_user_json" // NEW
    }

    private fun <T> fromJson(json: String?, typeToken: TypeToken<T>): T? {
        if (json.isNullOrBlank()) return null
        return gson.fromJson(json, typeToken.type)
    }
    private fun toJson(value: Any): String = gson.toJson(value)

    fun loadDonors(): MutableList<Donor> {
        val json = prefs.getString(KEY_DONORS, null)
        return fromJson(json, object : TypeToken<MutableList<Donor>>() {}) ?: defaultDonors().toMutableList()
    }
    fun saveDonors(list: List<Donor>) { prefs.edit().putString(KEY_DONORS, toJson(list)).apply() }

    fun loadUsers(): MutableList<User> {
        val json = prefs.getString(KEY_USERS, null)
        return fromJson(json, object : TypeToken<MutableList<User>>() {}) ?: mutableListOf()
    }
    fun saveUsers(list: List<User>) { prefs.edit().putString(KEY_USERS, toJson(list)).apply() }

    fun loadSchedules(): MutableList<Schedule> {
        val json = prefs.getString(KEY_SCHEDULES, null)
        return fromJson(json, object : TypeToken<MutableList<Schedule>>() {}) ?: mutableListOf()
    }
    fun saveSchedules(list: List<Schedule>) { prefs.edit().putString(KEY_SCHEDULES, toJson(list)).apply() }

    // NEW: Persist Blood Requests 👇
    fun loadRequests(): MutableList<BloodRequest> {
        val json = prefs.getString(KEY_REQUESTS, null)
        return fromJson(json, object : TypeToken<MutableList<BloodRequest>>() {}) ?: mutableListOf()
    }
    fun saveRequests(list: List<BloodRequest>) {
        prefs.edit().putString(KEY_REQUESTS, toJson(list)).apply()
    }
    // NEW: ^^^^^^^^^^^^^^^^^^^^^^^^^

    private fun defaultDonors() = listOf(
        Donor(
            name = "Rahim Uddin",
            bloodGroup = BloodGroup.O_NEG,
            phone = "01710000001",
            verified = true,
            age = 28,
            city = "Dhaka",
            area = "Dhanmondi",
            lastDonationMillis = System.currentTimeMillis() - 120L * 24 * 60 * 60 * 1000, // 120 days ago
            hospital = "Square Hospital"
        ),
        Donor(
            name = "Ayesha Khan",
            bloodGroup = BloodGroup.A_POS,
            phone = "01710000002",
            verified = true,
            age = 24,
            city = "Dhaka",
            area = "Gulshan",
            lastDonationMillis = System.currentTimeMillis() - 95L * 24 * 60 * 60 * 1000, // 95 days ago
            hospital = "United Hospital"
        ),
        Donor(
            name = "Jahangir",
            bloodGroup = BloodGroup.B_NEG,
            phone = "01710000003",
            verified = false,
            age = 35,
            city = "Chattogram",
            area = "Pahartali",
            lastDonationMillis = System.currentTimeMillis() - 200L * 24 * 60 * 60 * 1000, // 200 days ago
            hospital = "CMCH"
        ),
        Donor(
            name = "Mina Sultana",
            bloodGroup = BloodGroup.AB_NEG,
            phone = "01710000004",
            verified = false,
            age = 31,
            city = "Sylhet",
            area = "Zindabazar",
            lastDonationMillis = null, // unknown/new donor
            hospital = "Osmani Medical"
        ),
        Donor(
            name = "Sohan",
            bloodGroup = BloodGroup.O_POS,
            phone = "01710000005",
            verified = true,
            age = 27,
            city = "Dhaka",
            area = "Banani",
            lastDonationMillis = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000, // 1 year ago
            hospital = "Ibn Sina"
        ),
    )
    fun loadMyDonor(): Donor? {
        val json = prefs.getString(KEY_MY_DONOR, null)
        return fromJson(json, object : com.google.gson.reflect.TypeToken<Donor>() {})
    }

    fun saveMyDonor(donor: Donor) {
        prefs.edit().putString(KEY_MY_DONOR, toJson(donor)).apply()
    }

    // Store the logged-in user as RAW JSON so we don't depend on specific field names.
    fun loadCurrentUserJson(): String? = prefs.getString(KEY_CURRENT_USER_JSON, null)

    fun saveCurrentUserJson(json: String?) {
        prefs.edit().apply {
            if (json == null) remove(KEY_CURRENT_USER_JSON) else putString(KEY_CURRENT_USER_JSON, json)
        }.apply()
    }

    fun logoutCurrentUser() {
        prefs.edit().apply {
            remove(KEY_CURRENT_USER_JSON)   // Clear current user session
            remove(KEY_MY_DONOR)            // Clear donor info
        }.apply()
    }


}
