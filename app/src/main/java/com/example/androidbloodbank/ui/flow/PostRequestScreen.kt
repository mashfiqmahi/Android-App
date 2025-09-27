package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.data.remote.FirebaseRepo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firebase = remember { FirebaseRepo() }

    var requester by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<BloodGroup?>(null) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    fun canSubmit(): Boolean =
        requester.trim().length >= 2 &&
                !phone.trim().isEmpty() &&
                selectedGroup != null

    suspend fun submit() {
        loading = true
        try {
            val req = BloodRequest(
                requesterName = requester.trim(),
                hospitalName = hospital.trim(),
                locationName = location.trim(),
                bloodGroup = selectedGroup!!,
                phone = phone.trim(),
                neededOnMillis = System.currentTimeMillis()
            )

            // --- Local: append to saved requests list ---
            val existing = runCatching { repo.loadRequests() }.getOrElse { mutableListOf() }
            val updated = existing.toMutableList().apply { add(0, req) }
            runCatching { repo.saveRequests(updated) }

            // --- Cloud: push to Firebase (non-blocking with timeout) ---
            withTimeoutOrNull(10_000) { firebase.addBloodRequest(req) }

            snackbarHostState.showSnackbar("Request posted.")
            onBack()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to post request.")
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Blood Request") },
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
                value = requester,
                onValueChange = { v: String -> requester = v },
                label = { Text("Requester name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = hospital,
                onValueChange = { v: String -> hospital = v },
                label = { Text("Hospital / Clinic") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { v: String -> location = v },
                label = { Text("Location (area/city)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Blood group selector ----
            ExposedDropdownMenuBox(
                expanded = groupMenuExpanded,
                onExpandedChange = { expanded: Boolean -> groupMenuExpanded = expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedGroup?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Blood group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = groupMenuExpanded,
                    onDismissRequest = { groupMenuExpanded = false }
                ) {
                    BloodGroup.values().forEach { bg ->
                        DropdownMenuItem(
                            text = { Text(bg.toString()) },
                            onClick = {
                                selectedGroup = bg
                                groupMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { v: String -> phone = v },
                label = { Text("Contact number") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { scope.launch { if (canSubmit()) submit() else snackbarHostState.showSnackbar("Fill required fields.") } },
                enabled = canSubmit() && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Posting…")
                } else {
                    Text("Post request")
                }
            }
        }
    }
}
