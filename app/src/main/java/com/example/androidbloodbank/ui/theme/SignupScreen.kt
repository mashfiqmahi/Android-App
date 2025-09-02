package com.example.androidbloodbank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.data.model.User

@Composable
fun SignupScreen(repo: LocalRepo, onBack: () -> Unit, onSignupSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedBG by remember { mutableStateOf(BloodGroup.O_POS) }
    var verified by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign up as Donor (demo)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Text("Blood Group:")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BloodGroup.values().forEach { bg ->
                val selected = bg == selectedBG
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { selectedBG = bg }
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) { Text(bg.label, color = if (selected) Color.White else Color.Black) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = verified, onCheckedChange = { verified = it })
            Text("Mark as Verified (demo)")
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            if (email.isBlank() || name.isBlank()) return@Button
            val users = repo.loadUsers().apply {
                add(User(name = name.trim(), email = email.trim(), bloodGroup = selectedBG))
            }
            repo.saveUsers(users)
            val donors = repo.loadDonors().apply {
                add(Donor(name = name.trim(), bloodGroup = selectedBG, phone = phone.ifBlank { null }, verified = verified))
            }
            repo.saveDonors(donors)
            onSignupSuccess()
        }, modifier = Modifier.fillMaxWidth()) { Text("Create Account & Register as Donor") }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
