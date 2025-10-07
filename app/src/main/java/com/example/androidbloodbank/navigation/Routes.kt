package com.example.androidbloodbank.navigation

sealed class Route(val path: String) {
    // Entry
    data object Splash : Route("splash")
    data object Gate   : Route("gate")

    // Auth
    data object SignIn : Route("signin")
    data object SignUp : Route("signup")

    // Forget Pass
    object ForgotPassword : Route("forgot_password")

    // Main
    data object Home   : Route("home")

    // Donate flow
    data object Donate       : Route("donate")
    data object ViewRequests : Route("view_requests")
    data object PostRequest  : Route("post_request")

    // Find donors flow
    data object FindDonors       : Route("find_donors")
    data object SelectBloodGroup : Route("select_bg")
    data object DonorProfile : Route("donor_profile/{donorId}") {
        private const val KEY = "donorId"
        fun create(donorId: String) = "donor_profile/$donorId"
        const val ArgKey = KEY
    }

    // Blood bank flow
    data object BloodBank       : Route("blood_bank")
    data object NearbyBloodBank : Route("nearby_blood_bank")
    data object AvailableBlood  : Route("available_blood")

    // Request blood flow
    data object RequestBlood : Route("request_blood")
    data object TrackRequest : Route("track_request")
    data object EditRequest : Route("edit_request")

    // Profile & info
    data object Profile   : Route("profile")
    data object BloodInfo : Route("blood_info")

    // NEW: Emergency SOS (offline donors)
    data object EmergencySOS : Route("emergency_sos")
}
