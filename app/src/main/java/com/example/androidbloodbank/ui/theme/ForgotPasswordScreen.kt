package com.example.androidbloodbank.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSent: (String) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val e = email.text.trim()
                    if (e.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                        error = "Enter a valid email address."
                        return@Button
                    }
                    error = null
                    isLoading = true
                    auth.sendPasswordResetEmail(e)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onSent(e)
                            } else {
                                error = task.exception?.localizedMessage ?: "Failed to send reset email."
                            }
                        }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Send reset link")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "We’ll email you a link to reset your password.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
