package com.example.androidbloodbank.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.R
import com.example.androidbloodbank.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserProfile,
    onUpdate: (UserProfile) -> Unit,
    onBack: () -> Unit,
    email: String = "" // optional: pass email if available
) {
    // date formatter for display & parsing
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // initial values from user model
    var fullName by remember { mutableStateOf(user.name) }
    var bloodGroup by remember { mutableStateOf(user.bloodGroup) }
    var contact by remember { mutableStateOf(user.contactNumber ?: "") }
    var address by remember { mutableStateOf(user.location ?: "") }
    var totalDonationsStr by remember { mutableStateOf(user.totalDonations.toString()) }
    var lastDonationStr by remember {
        mutableStateOf(user.lastDonationMillis?.let { sdf.format(Date(it)) } ?: "")
    }

    // derived
    val lastDonationMillisFromField: Long? = remember(lastDonationStr) {
        if (lastDonationStr.isBlank()) null
        else try {
            sdf.parse(lastDonationStr)?.time
        } catch (_: Exception) { null }
    }

    val daysRemaining: Long = remember(lastDonationMillisFromField) {
        lastDonationMillisFromField?.let {
            val next = it + 90L * 24 * 60 * 60 * 1000 // 90 days in ms
            val diff = next - System.currentTimeMillis()
            if (diff <= 0) 0L else (diff / (24 * 60 * 60 * 1000))
        } ?: 0L
    }
    val isEligible: Boolean = daysRemaining == 0L

    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (isEditing) {
                            // build updated UserProfile and call onUpdate
                            val updated = user.copy(
                                name = fullName,
                                bloodGroup = bloodGroup,
                                lastDonationMillis = lastDonationMillisFromField,
                                totalDonations = totalDonationsStr.toIntOrNull() ?: user.totalDonations,
                                contactNumber = contact,
                                location = address
                            )
                            onUpdate(updated)
                        }
                        isEditing = !isEditing
                    }) {
                        Text(if (isEditing) "Save" else "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // profile picture placeholder (replace with actual image picker later)
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // email (optional, read-only if not passed)
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    readOnly = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = totalDonationsStr,
                    onValueChange = { totalDonationsStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Donations") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = lastDonationStr,
                    onValueChange = { lastDonationStr = it },
                    label = { Text("Last Donation Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else {
                Text(fullName, style = MaterialTheme.typography.titleMedium)
                if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Blood Group: $bloodGroup")
                Text("Contact: ${if (contact.isBlank()) "N/A" else contact}")
                Text("Address: ${if (address.isBlank()) "N/A" else address}")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Total donations: ${user.totalDonations}")
                Text("Last donation: ${user.lastDonationMillis?.let { sdf.format(Date(it)) } ?: "N/A"}")
                Text("Eligible to donate: ${if (isEligible) "Yes" else "No"}")
                if (!isEligible) Text("Days remaining: $daysRemaining")
            }
        }
    }
}
