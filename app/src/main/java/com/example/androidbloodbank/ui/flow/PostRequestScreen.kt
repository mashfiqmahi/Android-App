package com.example.androidbloodbank.ui.flow

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

// For enum BloodGroup we need a Saver:
val BloodGroupSaver = Saver<BloodGroup, String>(
    save = { it.name },
    restore = { runCatching { BloodGroup.valueOf(it) }.getOrDefault(BloodGroup.O_POS) }
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    repo: LocalRepo,
    onPosted: () -> Unit,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var requester by rememberSaveable  { mutableStateOf("") }
    var hospital by rememberSaveable  { mutableStateOf("") }
    var location by rememberSaveable  { mutableStateOf("") }
    var phone by rememberSaveable  { mutableStateOf("") }
    var group by rememberSaveable(stateSaver = BloodGroupSaver) { mutableStateOf(BloodGroup.O_POS) }
    var needed by rememberSaveable { mutableStateOf(0L) }
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
                title = { Text("Post blood request") },
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
            OutlinedTextField(value = requester, onValueChange = { requester = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = { Text("Hospital/Clinic") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                Text(if (needed > 0)
                    "Needed on: ${java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date(needed))}"
                else "Pick needed date")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                enabled = requester.isNotBlank() && phone.isNotBlank() && needed > 0 && !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid.isNullOrBlank()) {
                                snackbar.showSnackbar("You are signed out.")
                                return@launch
                            }

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

                            val root = FirebaseDatabase.getInstance(SG_DB_URL).reference
                            // 1) Owner-private copy (edit/delete)
                            val ownerRef = root.child("requests").child(uid).push()
                            ownerRef.setValue(payload).await()
                            val id = ownerRef.key ?: error("no key")

                            // 2) Public feed copy (world-readable)
                            root.child("requests_public").child(id).setValue(payload).await()

                            snackbar.showSnackbar("Request posted")
                            onPosted()
                        } catch (e: Exception) {
                            snackbar.showSnackbar(e.localizedMessage ?: "Failed to post")
                        } finally {
                            loading = false
                        }
                    }
                }
                ,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (loading) "Posting…" else "Post request") }
        }
    }
}
