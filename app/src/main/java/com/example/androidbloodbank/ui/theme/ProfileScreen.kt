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
import com.example.androidbloodbank.data.LocalRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    // Fake user data (replace with repo.getUser() later)
    var fullName by remember { mutableStateOf("John Doe") }
    var email by remember { mutableStateOf("john@example.com") }
    var address by remember { mutableStateOf("Dhaka, Bangladesh") }
    var bloodGroup by remember { mutableStateOf("A+") }
    var contact by remember { mutableStateOf("+880123456789") }
    var donations by remember { mutableStateOf(5) }
    var lastDonation by remember { mutableStateOf("2025-07-10") }
    var eligible by remember { mutableStateOf(true) }

    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { isEditing = !isEditing }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile picture placeholder
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )

            if (isEditing) {
                // Editable fields
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // View mode
                Text("Full Name: $fullName")
                Text("Email: $email")
                Text("Address: $address")
                Text("Blood Group: $bloodGroup")
                Text("Contact: $contact")
                Divider()
                Text("Total Donations: $donations")
                Text("Last Donation: $lastDonation")
                Text("Eligible: ${if (eligible) "Yes" else "No"}")
            }
        }
    }
}
