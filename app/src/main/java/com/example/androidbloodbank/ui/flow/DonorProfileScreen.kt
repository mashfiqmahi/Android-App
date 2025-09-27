package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.UserProfile
import com.example.androidbloodbank.data.remote.FirebaseRepo
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorProfileScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firebase = remember { FirebaseRepo() }

    // Basic editable fields for a public donor card + private profile
    var name by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }          // e.g., "A+", "O-"
    var contact by remember { mutableStateOf("") }              // phone
    var location by remember { mutableStateOf("") }
    var lastDonationMillis by remember { mutableStateOf<Long?>(null) }
    var totalDonations by remember { mutableStateOf(0) }

    var loading by remember { mutableStateOf(false) }

    fun canSave(): Boolean =
        name.trim().length >= 2 && bloodGroup.trim().isNotEmpty()

    suspend fun saveProfile() {
        loading = true
        try {
            val profile = UserProfile(
                name = name.trim(),
                bloodGroup = bloodGroup.trim(),                 // stored as String in your model
                lastDonationMillis = lastDonationMillis,
                totalDonations = totalDonations,
                contactNumber = contact.trim(),
                location = location.trim()
            )

            // Save locally (as you already do elsewhere)
            runCatching { repo.saveCurrentUserJson(Gson().toJson(profile)) }

            // Save to Firebase (don’t block UI too long)
            withTimeoutOrNull(10_000) { firebase.saveProfile(profile) }
            withTimeoutOrNull(10_000) { firebase.publishDonorCard(profile) }

            snackbarHostState.showSnackbar("Profile saved.")
            onBack()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to save profile.")
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donor Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bloodGroup, onValueChange = { bloodGroup = it },
                label = { Text("Blood group (e.g., A+, O-)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contact, onValueChange = { contact = it },
                label = { Text("Contact number") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location (city/area)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = totalDonations.toString(),
                onValueChange = { v: String -> totalDonations = v.toIntOrNull() ?: 0 }, // 👈 type added
                label = { Text("Total donations") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // If you have a date picker, set lastDonationMillis from there; otherwise leave null.

            Button(
                onClick = { scope.launch { if (canSave()) saveProfile() else snackbarHostState.showSnackbar("Fill name and blood group.") } },
                enabled = canSave() && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Saving…")
                } else {
                    Text("Save profile")
                }
            }
        }
    }
}
