package com.example.androidbloodbank.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBloodScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseDatabase.getInstance(SG_DB_URL).reference

    var requester by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(BloodGroup.O_POS) }
    var needed by remember { mutableStateOf(0L) }
    var showDate by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    if (showDate) {
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { showDate = false }) { Text("Done") } }
        ) {
            val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
            DatePicker(state = state, title = { Text("Needed on") })
            LaunchedEffect(state.selectedDateMillis) { needed = state.selectedDateMillis ?: needed }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request blood") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uid.isBlank()) {
                Text("Please sign in to post a request.", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            OutlinedTextField(value = requester, onValueChange = { requester = it },
                label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = hospital, onValueChange = { hospital = it },
                label = { Text("Hospital/Clinic") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = location, onValueChange = { location = it },
                label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) },
                label = { Text("Contact number") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            var bgExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = bgExpanded, onExpandedChange = { bgExpanded = !bgExpanded }) {
                OutlinedTextField(
                    value = group.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Blood group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bgExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = bgExpanded, onDismissRequest = { bgExpanded = false }) {
                    BloodGroup.values().forEach { bg ->
                        DropdownMenuItem(text = { Text(bg.toString()) }, onClick = { group = bg; bgExpanded = false })
                    }
                }
            }

            OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (needed > 0)
                        "Needed on: ${java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date(needed))}"
                    else "Pick needed date"
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                enabled = requester.isNotBlank() && phone.isNotBlank() && needed > 0 && !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            // Payload MUST include ownerUid and the 4 required fields for your rules
                            val payload = mapOf(
                                "ownerUid" to uid,
                                "requesterName" to requester.trim(),
                                "hospitalName" to hospital.trim(),
                                "locationName" to location.trim(),
                                "bloodGroup" to group.toString(),
                                "phone" to phone.trim(),
                                "neededOnMillis" to needed,
                                "createdAt" to System.currentTimeMillis()
                            )
                            val ref = db.child("requests").child(uid).push()
                            ref.setValue(payload).await()

                            snackbar.showSnackbar("Request posted")
                            // Optional: clear form
                            requester = ""; hospital = ""; location = ""; phone = ""; needed = 0L; group = BloodGroup.O_POS
                        } catch (e: Exception) {
                            snackbar.showSnackbar(e.localizedMessage ?: "Failed to post")
                        } finally {
                            loading = false
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (loading) "Posting…" else "Post request") }
        }
    }
}
