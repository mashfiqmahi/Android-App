package com.example.androidbloodbank.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodRequest
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRequestScreen(
    requestId: String,
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var base by remember { mutableStateOf<BloodRequest?>(null) }

    var requesterName by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(requestId) {
        try {
            val req = repo.getBloodRequestById(requestId) // suspend
            base = req
            if (req != null) {
                requesterName = req.requesterName
                hospitalName = req.hospitalName
                locationName = req.locationName
                phone = req.phone
            } else {
                error = "Request not found."
            }
        } catch (t: Throwable) {
            error = t.localizedMessage ?: "Failed to load request."
            Log.e("EditRequestScreen", "Load failed", t)
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit request") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = requesterName,
                onValueChange = { requesterName = it },
                label = { Text("Requester name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = hospitalName,
                onValueChange = { hospitalName = it },
                label = { Text("Hospital") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    // Keep only digits, allow a single leading '+'
                    val filtered = buildString {
                        input.forEachIndexed { index, c ->
                            if (c.isDigit() || (c == '+' && index == 0)) append(c)
                        }
                    }
                    phone = filtered.take(15) // optional length cap
                },
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                )
            )


            Button(
                onClick = {
                    val current = base ?: return@Button
                    scope.launch {
                        try {
                            val updated = current.copy(
                                requesterName = requesterName,
                                hospitalName = hospitalName,
                                locationName = locationName,
                                phone = phone
                            )
                            repo.updateBloodRequest(requestId, updated) // suspend
                            onBack()
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Failed to save."
                            Log.e("EditRequestScreen", "Save failed", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Save changes")
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
