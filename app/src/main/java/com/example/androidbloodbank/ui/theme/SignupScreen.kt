package com.example.androidbloodbank.ui

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.User
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onSignupSuccess: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // Normalize phone number by removing any non-digit characters
    fun normalizePhone(raw: String?): String = raw?.filter { it.isDigit() }.orEmpty()

    // Check if the provided string is a valid email
    fun isEmail(s: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()

    // Can submit if email or phone is provided, password is valid and confirmed
    fun canSubmit(): Boolean {
        val hasId = email.isNotBlank() || phone.isNotBlank()
        val emailOk = email.isBlank() || isEmail(email)
        val phoneOk = phone.isBlank() || normalizePhone(phone).length >= 7
        val passOk = password.length >= 6 && password == confirm
        val nameOk = name.trim().length >= 2
        return hasId && emailOk && phoneOk && passOk && nameOk
    }

    // Best-effort field reader to match common User models without changing your data class
    fun readField(any: Any, name: String): String? = runCatching {
        val f = any::class.java.getDeclaredField(name)
        f.isAccessible = true
        (f.get(any) as? String)?.trim()
    }.getOrNull()

    suspend fun doSignup() {
        loading = true
        focus.clearFocus()

        val users = repo.loadUsers()

        // Duplicate check (email or phone)
        val emailLower = email.trim().lowercase()
        val phoneDigits = normalizePhone(phone)

        val duplicate = users.any { u ->
            val uEmail = readField(u, "email") ?: readField(u, "username")
            val uPhone = normalizePhone(readField(u, "phone") ?: "")
            (emailLower.isNotEmpty() && uEmail == emailLower) ||
                    (phoneDigits.isNotEmpty() && uPhone == phoneDigits)
        }

        if (duplicate) {
            snackbarHostState.showSnackbar("An account with this email/phone already exists.")
            loading = false
            return
        }

        // Build a User instance via Gson
        val payload = mapOf(
            "name" to name.trim(),
            "email" to email.trim().ifEmpty { null },
            "phone" to phone.trim().ifEmpty { null },
            "password" to password
        )

        val json = Gson().toJson(payload)
        val newUser: User = Gson().fromJson(json, User::class.java)

        val updated = users.toMutableList().apply { add(newUser) }
        repo.saveUsers(updated)

        // Save current user after signup
        repo.saveCurrentUserJson(Gson().toJson(newUser))  // Save current user JSON

        snackbarHostState.showSnackbar("Account created. You can log in now.")
        loading = false
        onSignupSuccess()  // Trigger navigation to next screen after signup
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign up") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6 chars)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { scope.launch { doSignup() } },
                enabled = canSubmit() && !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Creating account…")
                } else {
                    Text("Create account")
                }
            }
        }
    }
}
