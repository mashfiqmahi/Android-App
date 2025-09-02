package com.example.androidbloodbank.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo

@Composable
fun LoginScreen(repo: LocalRepo, onBack: () -> Unit, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var nameDisplay by remember { mutableStateOf("") }
    val users = remember { repo.loadUsers() }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login (demo)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (enter exactly as signed up)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val found = users.find { it.email.equals(email.trim(), ignoreCase = true) }
                if (found != null) {
                    nameDisplay = found.name
                    error = null
                    onLoginSuccess()
                } else error = "No user found for that email. Try signing up."
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Login (Demo)") }

        Spacer(Modifier.height(12.dp))
        if (!error.isNullOrBlank()) Text(error ?: "", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
        if (nameDisplay.isNotBlank()) Text("Welcome, $nameDisplay", modifier = Modifier.padding(top = 8.dp))
    }
}
