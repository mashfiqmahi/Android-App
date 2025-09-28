package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRequestsScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseDatabase.getInstance(SG_DB_URL).reference

    var items by remember { mutableStateOf<List<Pair<String, BloodRequest>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var editingId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<BloodRequest?>(null) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun parseRequest(s: DataSnapshot): BloodRequest {
        val g = when (s.child("bloodGroup").getValue(String::class.java) ?: "O+") {
            "A+" -> BloodGroup.A_POS; "A-" -> BloodGroup.A_NEG
            "B+" -> BloodGroup.B_POS; "B-" -> BloodGroup.B_NEG
            "AB+"-> BloodGroup.AB_POS; "AB-"-> BloodGroup.AB_NEG
            "O-" -> BloodGroup.O_NEG; else -> BloodGroup.O_POS
        }
        return BloodRequest(
            requesterName = s.child("requesterName").getValue(String::class.java) ?: "",
            hospitalName  = s.child("hospitalName").getValue(String::class.java) ?: "",
            locationName  = s.child("locationName").getValue(String::class.java) ?: "",
            bloodGroup    = g,
            phone         = s.child("phone").getValue(String::class.java) ?: "",
            neededOnMillis= s.child("neededOnMillis").getValue(Long::class.java) ?: 0L
        )
    }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                // auto-clean expired (client-side)
                val now = System.currentTimeMillis()
                val myRef = db.child("requests").child(uid)
                val snap = myRef.get().await()
                for (c in snap.children) {
                    val needed = c.child("neededOnMillis").getValue(Long::class.java) ?: 0L
                    if (needed in 1 until now) myRef.child(c.key!!).removeValue().await()
                }

                val fresh = myRef.get().await().children.mapNotNull { node ->
                    node.key?.let { id -> id to parseRequest(node) }
                }.sortedBy { it.second.neededOnMillis }

                items = fresh
            } catch (e: Exception) {
                snackbar.showSnackbar(e.localizedMessage ?: "Failed to load")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { if (uid.isNotBlank()) refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uid.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sign in to view your requests.") }
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active requests.") }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)
                ) {
                    items(items, key = { it.first }) { (id, req) ->
                        RequestCard(
                            req = req,
                            onEdit = { editingId = id; editing = req.copy() },
                            onDelete = {
                                scope.launch {
                                    runCatching { db.child("requests").child(uid).child(id).removeValue().await() }
                                        .onSuccess { snackbar.showSnackbar("Request deleted"); refresh() }
                                        .onFailure { snackbar.showSnackbar(it.localizedMessage ?: "Delete failed") }
                                }
                            }
                        )
                    }
                }
            }

            if (editing != null) {
                ModalBottomSheet(
                    onDismissRequest = { editing = null; editingId = null },
                    sheetState = sheet,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    EditRequestForm(
                        req = editing!!,
                        onCancel = { editing = null; editingId = null },
                        onSave = { updated ->
                            val id = editingId ?: return@EditRequestForm
                            scope.launch {
                                try {
                                    val payload = mapOf(
                                        "ownerUid" to uid,
                                        "requesterName" to updated.requesterName,
                                        "hospitalName" to updated.hospitalName,
                                        "locationName" to updated.locationName,
                                        "bloodGroup" to updated.bloodGroup.toString(),
                                        "phone" to updated.phone,
                                        "neededOnMillis" to updated.neededOnMillis
                                    )
                                    db.child("requests").child(uid).child(id).setValue(payload).await()
                                    snackbar.showSnackbar("Request updated")
                                    editing = null; editingId = null
                                    refresh()
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(e.localizedMessage ?: "Update failed")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    req: BloodRequest,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(req.requesterName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Hospital: ${req.hospitalName.ifBlank { "—" }}")
            Text("Location: ${req.locationName.ifBlank { "—" }}")
            Text("Blood: ${req.bloodGroup}")
            val daysLeft = if (req.neededOnMillis > 0)
                ((req.neededOnMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).toInt()
            else null
            Text(
                "Needed: " + when {
                    req.neededOnMillis <= 0 -> "—"
                    daysLeft != null && daysLeft >= 0 -> "in $daysLeft day(s)"
                    else -> "expired"
                }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                Button(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRequestForm(
    req: BloodRequest,
    onCancel: () -> Unit,
    onSave: (BloodRequest) -> Unit
) {
    var requester by remember { mutableStateOf(req.requesterName) }
    var hospital by remember { mutableStateOf(req.hospitalName) }
    var location by remember { mutableStateOf(req.locationName) }
    var phone by remember { mutableStateOf(req.phone) }
    var group by remember { mutableStateOf(req.bloodGroup) }
    var needed by remember { mutableStateOf(req.neededOnMillis) }

    var showDate by remember { mutableStateOf(false) }
    if (showDate) {
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { showDate = false }) { Text("Done") } }
        ) {
            val state = rememberDatePickerState(initialSelectedDateMillis = needed.takeIf { it > 0 } ?: System.currentTimeMillis())
            DatePicker(state = state, title = { Text("Needed on") })
            LaunchedEffect(state.selectedDateMillis) { needed = state.selectedDateMillis ?: needed }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Edit request", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = requester, onValueChange = { requester = it }, label = { Text("Requester name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    onSave(
                        BloodRequest(
                            requesterName = requester.trim(),
                            hospitalName = hospital.trim(),
                            locationName = location.trim(),
                            bloodGroup = group,
                            phone = phone.trim(),
                            neededOnMillis = needed
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = requester.isNotBlank() && phone.isNotBlank() && needed > 0
            ) { Text("Save") }
        }
        Spacer(Modifier.height(12.dp))
    }
}
