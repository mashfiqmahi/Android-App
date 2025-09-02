package com.example.androidbloodbank.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.model.UserProfile

@Composable
fun ProfileScreen(user: UserProfile, onUpdate: (UserProfile) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var bloodGroup by remember { mutableStateOf(user.bloodGroup) }
    var contact by remember { mutableStateOf(user.contactNumber) }
    var location by remember { mutableStateOf(user.location) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("User Profile", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Text("Eligibility: ${if (user.isEligible) "✅ Eligible" else "❌ Not Eligible"}")
        if (!user.isEligible) {
            Text("Days remaining: ${user.daysRemaining}")
        }
        Text("Total Donations: ${user.totalDonations}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            onUpdate(user.copy(name = name, bloodGroup = bloodGroup, contactNumber = contact, location = location))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Profile")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
