package com.example.androidbloodbank.ui

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onSignupSuccess: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val auth = remember { FirebaseAuth.getInstance() }
    val dbRef = remember { FirebaseDatabase.getInstance().reference } // optional profile save

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun isEmail(s: String) = Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()
    fun canSubmit(): Boolean {
        val emailOk = isEmail(email)
        val passOk = password.length >= 6 && password == confirm
        val nameOk = name.trim().length >= 2
        return emailOk && passOk && nameOk
    }

    suspend fun doSignup() {
        loading = true
        try {
            // 1) Create the account in Firebase Auth (20s timeout to avoid infinite spinner)
            val authResult = withTimeoutOrNull(20_000) {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
            }
            if (authResult == null) {
                snackbarHostState.showSnackbar("Signup timed out. Check internet and try again.")
                return
            }

            val uid = auth.currentUser?.uid.orEmpty()
            if (uid.isEmpty()) {
                snackbarHostState.showSnackbar("Signup failed: no UID returned.")
                return
            }

            // 2) Optional: store public profile to Realtime Database
            val profile = mapOf(
                "uid" to uid,
                "name" to name.trim(),
                "email" to email.trim(),
                "phone" to phone.trim().ifEmpty { null }
            )
            // Don’t block UX if DB write fails; but we try with timeout
            withTimeoutOrNull(10_000) {
                dbRef.child("users").child(uid).setValue(profile).await()
            }

            // 3) Optional: keep a tiny local session snapshot so your Splash/Gate logic works
            repo.saveCurrentUserJson(Gson().toJson(profile))

            snackbarHostState.showSnackbar("Account created.")
            onSignupSuccess()
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthException -> when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use."
                    "ERROR_INVALID_EMAIL" -> "Invalid email address."
                    "ERROR_WEAK_PASSWORD" -> "Weak password. Use 6+ characters."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in not enabled in Firebase."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection."
                    else -> e.localizedMessage ?: "Sign up failed."
                }
                else -> e.localizedMessage ?: "Sign up failed."
            }
            snackbarHostState.showSnackbar(msg)
        } finally {
            loading = false
        }
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
                value = name, onValueChange = { name = it },
                label = { Text("Full name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone (optional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password (min 6 chars)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("Confirm password") }, singleLine = true,
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
