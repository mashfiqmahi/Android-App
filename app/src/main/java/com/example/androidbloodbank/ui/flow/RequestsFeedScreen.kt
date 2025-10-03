package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.ui.components.RequestCard
import com.example.androidbloodbank.ui.components.RequestItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsFeedScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onRequestNew: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseDatabase.getInstance(SG_DB_URL).reference

    var all by remember { mutableStateOf<List<RequestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val groups = remember { listOf("All groups") + listOf("A+","A-","B+","B-","AB+","AB-","O+","O-") }
    var groupFilter by remember { mutableStateOf(groups.first()) }

    var editing by remember { mutableStateOf<RequestItem?>(null) }
    // ← THIS is the correct way: call composable directly (no extra remember block)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // For deleting expired requests
    var items by remember { mutableStateOf<List<Pair<String, BloodRequest>>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun fetchAll() {
        val snap = db.child("requests_public").get().await()
        all = snap.children.map { s ->
            RequestItem(
                id = s.key.orEmpty(),
                ownerUid = s.child("ownerUid").getValue(String::class.java).orEmpty(),
                requesterName = s.child("requesterName").getValue(String::class.java).orEmpty(),
                hospitalName = s.child("hospitalName").getValue(String::class.java).orEmpty(),
                locationName = s.child("locationName").getValue(String::class.java).orEmpty(),
                bloodGroupLabel = s.child("bloodGroup").getValue(String::class.java).orEmpty(),
                phone = s.child("phone").getValue(String::class.java).orEmpty(),
                neededOnMillis = s.child("neededOnMillis").getValue(Long::class.java) ?: 0L
            )
        }.sortedBy { it.neededOnMillis }
    }

    fun refresh() {
        scope.launch {
            loading = true
            runCatching { fetchAll() }
                .onFailure { snackbar.showSnackbar(it.localizedMessage ?: "Failed to load requests") }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(Unit) {
        loading = true
        runCatching { com.example.androidbloodbank.data.remote.FirebaseRepo().listActivePublicRequests() }
            .onSuccess { items = it }
            .onFailure { error = it.localizedMessage ?: "Failed to load" }
        loading = false
    }

    val filtered by remember(all, query, groupFilter) {
        derivedStateOf {
            all.filter { item ->
                (groupFilter == "All groups" || item.bloodGroupLabel == groupFilter) &&
                        (query.isBlank() || listOf(item.requesterName, item.hospitalName, item.locationName)
                            .any { it.contains(query.trim(), ignoreCase = true) })
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRequestNew,
                icon = { Icon(Icons.Outlined.WaterDrop, null) },
                text = { Text("Request Blood") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Search name / hospital / location") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("Filter", style = MaterialTheme.typography.labelLarge)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = groupFilter, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    groups.forEach { g ->
                        DropdownMenuItem(text = { Text(g) }, onClick = { groupFilter = g; expanded = false })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val mine = item.ownerUid == uid

                        // ← THIS is the correct way: the helper already remembers internally
                        val dismissState = rememberSwipeToDismissBoxState()

                        val cardContent: @Composable () -> Unit = {
                            RequestCard(
                                item = item,
                                showCall = !mine,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(item.phone)}"))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        if (mine) {
                            // React to swipe results
                            LaunchedEffect(dismissState.currentValue) {
                                when (dismissState.currentValue) {
                                    StartToEnd -> {
                                        runCatching {
                                            db.child("requests_public").child(item.id).removeValue().await()
                                            db.child("requests").child(uid).child(item.id).removeValue().await()
                                        }.onSuccess {
                                            snackbar.showSnackbar("Request deleted")
                                            refresh()
                                        }.onFailure {
                                            snackbar.showSnackbar(it.localizedMessage ?: "Delete failed")
                                        }
                                        dismissState.snapTo(Settled)
                                    }
                                    EndToStart -> {
                                        editing = item
                                        dismissState.snapTo(Settled)
                                    }
                                    else -> Unit
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    val isDelete =
                                        dismissState.dismissDirection == StartToEnd || dismissState.currentValue == StartToEnd
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(
                                                if (isDelete) MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (isDelete) Arrangement.Start else Arrangement.End
                                    ) {
                                        if (isDelete) {
                                            Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete")
                                        } else {
                                            Text("Edit"); Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.Edit, null)
                                        }
                                    }
                                },
                                content = { cardContent() }
                            )
                        } else {
                            cardContent()
                        }
                    }
                }
            }
        }

        // Inline edit sheet (public inside this file)
        editing?.let { item ->
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { editing = null }
            ) {
                InlineEditRequestForm(
                    req = BloodRequest(
                        requesterName = item.requesterName,
                        hospitalName = item.hospitalName,
                        locationName = item.locationName,
                        bloodGroup = when (item.bloodGroupLabel) {
                            "A+" -> BloodGroup.A_POS
                            "A-" -> BloodGroup.A_NEG
                            "B+" -> BloodGroup.B_POS
                            "B-" -> BloodGroup.B_NEG
                            "AB+" -> BloodGroup.AB_POS
                            "AB-" -> BloodGroup.AB_NEG
                            "O-" -> BloodGroup.O_NEG
                            else -> BloodGroup.O_POS
                        },
                        phone = item.phone,
                        neededOnMillis = item.neededOnMillis
                    ),
                    onCancel = { editing = null },
                    onSave = { updated ->
                        scope.launch {
                            try {
                                val payload = mapOf(
                                    "ownerUid" to item.ownerUid,
                                    "requesterName" to updated.requesterName,
                                    "hospitalName" to updated.hospitalName,
                                    "locationName" to updated.locationName,
                                    "bloodGroup" to updated.bloodGroup.toString(),
                                    "phone" to updated.phone,
                                    "neededOnMillis" to updated.neededOnMillis,
                                    "createdAt" to System.currentTimeMillis()
                                )
                                db.child("requests_public").child(item.id).setValue(payload).await()
                                db.child("requests").child(item.ownerUid).child(item.id).setValue(payload).await()
                                snackbar.showSnackbar("Request updated")
                                editing = null
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

/* -------- Inline edit form (no private composable calls) -------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineEditRequestForm(
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
            val s = rememberDatePickerState(initialSelectedDateMillis = needed.takeIf { it > 0 } ?: System.currentTimeMillis())
            DatePicker(state = s, title = { Text("Needed on") })
            LaunchedEffect(s.selectedDateMillis) { needed = s.selectedDateMillis ?: needed }
        }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Edit request", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = requester, onValueChange = { requester = it }, label = { Text("Requester name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = { Text("Hospital/Clinic") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) },
            label = { Text("Contact number") }, singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            ),
            modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = group.toString(), onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Blood group") }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BloodGroup.values().forEach { bg ->
                    DropdownMenuItem(text = { Text(bg.toString()) }, onClick = { group = bg; expanded = false })
                }
            }
        }

        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
            val label = if (needed > 0)
                java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date(needed))
            else "Pick needed date"
            Text("Needed on: $label")
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
        Spacer(Modifier.height(8.dp))
    }
}
