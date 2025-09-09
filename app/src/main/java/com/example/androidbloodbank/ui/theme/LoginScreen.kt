package com.example.androidbloodbank.ui

import android.graphics.ColorSpace.match
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.colorspace.ColorSpaces.match
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var identifier by remember { mutableStateOf("") } // email or phone
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    fun normalizePhone(raw: String?): String =
        raw?.filter { it.isDigit() }.orEmpty()

    fun isEmail(s: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()

    fun canSubmit(): Boolean {
        val id = identifier.trim()
        if (id.isEmpty() || password.isEmpty()) return false
        return isEmail(id) || normalizePhone(id).length >= 7
    }

    // Best-effort field reader to match common User models without changing your data class
    fun readField(any: Any, name: String): String? = runCatching {
        val f = any::class.java.getDeclaredField(name)
        f.isAccessible = true
        (f.get(any) as? String)?.trim()
    }.getOrNull()

    suspend fun tryLogin() {
        loading = true
        focus.clearFocus()

        val users = repo.loadUsers()  // This should be the correct list of users
        if (users.isEmpty()) {
            snackbarHostState.showSnackbar("No users found. Please sign up.")
            loading = false
            return
        }

        val idRaw = identifier.trim()
        val idEmail = idRaw.lowercase()
        val idPhone = normalizePhone(idRaw)

        // Try common field names: email/phone/password (adjust if yours differ)
        val matchedUser = users.firstOrNull { u ->
            val email = readField(u, "email") ?: readField(u, "username")
            val phone = readField(u, "phone") ?: readField(u, "phoneNumber") ?: readField(u, "mobile")
            val pass  = readField(u, "password") ?: readField(u, "pass")

            val emailOk = email?.lowercase() == idEmail && isEmail(idRaw)
            val phoneOk = normalizePhone(phone) == idPhone && idPhone.isNotEmpty()
            val passOk  = (pass ?: "") == password

            (emailOk || phoneOk) && passOk
        }

        if (matchedUser != null) {
            repo.saveCurrentUserJson(Gson().toJson(matchedUser))  // Save the matched user as JSON
            onLoginSuccess() // Navigate to the Home or next screen
        } else {
            snackbarHostState.showSnackbar("Invalid email/phone or password")
        }

        loading = false
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login") },
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
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Email or phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // good for email/phone
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(icon, contentDescription = if (showPassword) "Hide password" else "Show password")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { scope.launch { tryLogin() } },
                enabled = canSubmit() && !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Signing in…")
                } else {
                    Text("Login")
                }
            }

        }
    }
}
