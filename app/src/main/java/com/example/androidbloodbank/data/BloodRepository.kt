package com.example.androidbloodbank.data

import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.data.model.UserProfile

class BloodRepository {

    private val requests = mutableListOf<BloodRequest>()
    private var userProfile: UserProfile? = null

    // --- Blood Requests ---
    fun addRequest(request: BloodRequest) {
        requests.add(request)
    }

    fun getRequests(): List<BloodRequest> = requests

    // --- User Profile ---
    fun setUserProfile(profile: UserProfile) {
        userProfile = profile
    }

    fun getUserProfile(): UserProfile? = userProfile

    fun updateProfile(updated: UserProfile) {
        userProfile = updated
    }
}
