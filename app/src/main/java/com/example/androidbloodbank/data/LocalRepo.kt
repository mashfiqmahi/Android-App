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

    private fun defaultDonors() = listOf(
        Donor(name = "Rahim Uddin", bloodGroup = BloodGroup.O_NEG, phone = "01710000001", verified = true),
        Donor(name = "Ayesha Khan", bloodGroup = BloodGroup.A_POS, phone = "01710000002", verified = true),
        Donor(name = "Jahangir", bloodGroup = BloodGroup.B_NEG, phone = "01710000003"),
        Donor(name = "Mina Sultana", bloodGroup = BloodGroup.AB_NEG, phone = "01710000004"),
        Donor(name = "Sohan", bloodGroup = BloodGroup.O_POS, phone = "01710000005", verified = true),
    )
}
