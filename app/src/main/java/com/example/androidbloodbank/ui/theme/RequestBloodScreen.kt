package com.example.androidbloodbank.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBloodScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showingForm by rememberSaveable { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) } // null = add, else edit that index

    // Live list (sanitize + sort later)
    var requests by remember { mutableStateOf(repo.loadRequests()) }

    // ---- Search / Filter ----
    var query by rememberSaveable { mutableStateOf("") }
    var filterGroup by rememberSaveable { mutableStateOf<BloodGroup?>(null) }

    // ---- Derived, filtered & sorted list ----
    val displayList by remember(requests, query, filterGroup) {
        mutableStateOf(
            requests
                .filter { r ->
                    (filterGroup == null || r.bloodGroup == filterGroup) &&
                            (query.isBlank() || r.matchesQuery(query))
                }
                .sortedWith(compareByDescending<BloodRequest> { urgencyRank(it.neededOnMillis) }
                    .thenBy { it.neededOnMillis })
        )
    }

    // ---- Show form if adding or editing ----
    if (showingForm) {
        val initial: BloodRequest? = editingIndex?.let { idx ->
            requests.getOrNull(idx)
        }
        RequestBloodFormScreen(
            initial = initial,
            onBack = {
                showingForm = false
                editingIndex = null
            },
            onSubmit = { name, group, hospital, location, contact, needMillis ->
                scope.launch {
                    val newObj = buildRequestObject(name, group, hospital, location, contact, needMillis)
                    val updated = requests.toMutableList()
                    if (editingIndex == null) {
                        updated.add(0, newObj)
                        snackbar.showSnackbar("Request posted")
                    } else {
                        val idx = editingIndex!!
                        if (idx in updated.indices) updated[idx] = newObj
                        snackbar.showSnackbar("Request updated")
                    }
                    repo.saveRequests(updated)
                    requests = repo.loadRequests() // reload to be safe
                    showingForm = false
                    editingIndex = null
                }
            }
        )
        return
    }

    // ---------- LIST ----------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showingForm = true; editingIndex = null },
                icon = { Icon(Icons.Outlined.Bloodtype, null) },
                text = { Text("Request Blood") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Search & Filter row ---
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                label = { Text("Search name / hospital / location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BloodGroupFilterDropdown(
                    selected = filterGroup,
                    onSelected = { filterGroup = it },
                    modifier = Modifier.weight(1f)
                )
                if (filterGroup != null || query.isNotBlank()) {
                    TextButton(onClick = { filterGroup = null; query = "" }) {
                        Icon(Icons.Outlined.Clear, null); Spacer(Modifier.width(6.dp)); Text("Clear")
                    }
                }
            }

            if (displayList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching requests.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(displayList, key = { _, r -> r.stableKey() }) { _, req ->
                        // Map back to source index for edit/delete
                        val sourceIndex = requests.indexOf(req)
                        RequestRow(
                            req = req,
                            onEdit = {
                                if (sourceIndex >= 0) {
                                    editingIndex = sourceIndex
                                    showingForm = true
                                }
                            },
                            onDelete = {
                                if (sourceIndex >= 0) {
                                    val updated = requests.toMutableList()
                                    updated.removeAt(sourceIndex)
                                    repo.saveRequests(updated)
                                    requests = repo.loadRequests()
                                    scope.launch { snackbar.showSnackbar("Request deleted") }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ------------------ Swipe row wrapper ------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestRow(
    req: BloodRequest,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { // swipe right = Edit
                    onEdit()
                    false // don't keep dismissed
                }
                SwipeToDismissBoxValue.EndToStart -> {  // swipe left = Delete
                    onDelete()
                    false // we remove from list manually
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Fill the whole row behind the card
            val target = state.targetValue
            val color = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val icon = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete
                else -> null
            }
            val align = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = align
            ) {
                icon?.let { Icon(it, contentDescription = null) }
            }
        }

    ) {
        RequestItem(req = req)
    }
}

/* ------------------ List item with urgent badge ------------------ */

@Composable
private fun RequestItem(req: BloodRequest) {
    val context = LocalContext.current

    val safeMillis = if (req.neededOnMillis > 0L) req.neededOnMillis else System.currentTimeMillis()
    val dateStr = remember(safeMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(safeMillis))
    }
    val urgent = remember(safeMillis) { urgencyLabel(safeMillis) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.padding(14.dp)) {
            // Right-side blood group chip (unchanged)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 0.dp,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = req.bloodGroup.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null); Spacer(Modifier.width(8.dp))
                    Text(req.requesterName, style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalHospital, null); Spacer(Modifier.width(8.dp))
                    Text(req.hospitalName)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null); Spacer(Modifier.width(8.dp))
                    Text(req.locationName)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(8.dp))
                    Text(dateStr)
                    Spacer(Modifier.width(8.dp))
                    if (urgent != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(urgent) },
                            leadingIcon = {
                                Icon(
                                    if (urgent == "Today") Icons.Outlined.PriorityHigh else Icons.Outlined.Schedule,
                                    contentDescription = null
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (urgent == "Today")
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }

                // Call button instead of phone text
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${req.phone}"))
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Call, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Call")
                }
            }
        }
    }
}

/* ------------------ FORM (add & edit) ------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestBloodFormScreen(
    initial: BloodRequest? = null,
    onBack: () -> Unit,
    onSubmit: (name: String, group: BloodGroup, hospital: String, location: String, contact: String, needMillis: Long?) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.requesterName ?: "") }
    var group by remember { mutableStateOf<BloodGroup?>(initial?.bloodGroup) }
    var hospital by remember { mutableStateOf(initial?.hospitalName ?: "") }
    var location by remember { mutableStateOf(initial?.locationName ?: "") }
    var contact by remember { mutableStateOf(initial?.phone ?: "") }

    var needDateMillis by remember { mutableStateOf<Long?>(initial?.neededOnMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Done") } }
        ) {
            val state = rememberDatePickerState(initialSelectedDateMillis = needDateMillis ?: System.currentTimeMillis())
            DatePicker(state = state, title = { Text("Needed on") })
            LaunchedEffect(state.selectedDateMillis) { needDateMillis = state.selectedDateMillis }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Request Blood" else "Edit Request") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            BloodGroupDropdown(selected = group, onSelected = { group = it })
            OutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = { Text("Hospital") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = contact, onValueChange = { contact = it },
                label = { Text("Contact") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(8.dp)); Text(formatDate(needDateMillis))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank() || group == null || contact.isBlank()) {
                        scope.launch { snackbar.showSnackbar("Please fill name, blood group and contact") }
                    } else {
                        onSubmit(name.trim(), group!!, hospital.trim(), location.trim(), contact.trim(), needDateMillis)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (initial == null) "Submit request" else "Save changes") }
        }
    }
}

/* ------------------ Shared helpers ------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodGroupDropdown(
    selected: BloodGroup?,
    onSelected: (BloodGroup) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val all = BloodGroup.values().toList()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected?.let { label(it) } ?: "Select blood group",
            onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text("Blood group") }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            all.forEach { bg ->
                DropdownMenuItem(text = { Text(label(bg)) }, onClick = { onSelected(bg); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodGroupFilterDropdown(
    selected: BloodGroup?,
    onSelected: (BloodGroup?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val all = listOf<BloodGroup?>(null) + BloodGroup.values().toList()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { label(it) } ?: "All groups",
            onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            label = { Text("Filter") },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All groups") }, onClick = { onSelected(null); expanded = false })
            BloodGroup.values().forEach { bg ->
                DropdownMenuItem(text = { Text(label(bg)) }, onClick = { onSelected(bg); expanded = false })
            }
        }
    }
}

private fun label(bg: BloodGroup): String = when (bg) {
    BloodGroup.A_POS -> "A+"; BloodGroup.A_NEG -> "A-"
    BloodGroup.B_POS -> "B+"; BloodGroup.B_NEG -> "B-"
    BloodGroup.AB_POS -> "AB+"; BloodGroup.AB_NEG -> "AB-"
    BloodGroup.O_POS -> "O+"; BloodGroup.O_NEG -> "O-"
}

private fun formatDate(millis: Long?): String {
    if (millis == null || millis <= 0) return "Select date"
    val df = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return df.format(Date(millis))
}

private fun BloodRequest.matchesQuery(q: String): Boolean {
    val s = q.trim().lowercase(Locale.getDefault())
    if (s.isBlank()) return true
    return requesterName.lowercase(Locale.getDefault()).contains(s) ||
            hospitalName.lowercase(Locale.getDefault()).contains(s) ||
            locationName.lowercase(Locale.getDefault()).contains(s)
}

/** rank: 2 = today, 1 = tomorrow, 0 = later */
private fun urgencyRank(neededOnMillis: Long): Int {
    val (startToday, startTomorrow) = dayStartAndTomorrow()
    val startDayAfter = startTomorrow + 24.hours
    return when (neededOnMillis) {
        in startToday until startTomorrow -> 2
        in startTomorrow until startDayAfter -> 1
        else -> 0
    }
}

private fun urgencyLabel(neededOnMillis: Long): String? {
    return when (urgencyRank(neededOnMillis)) {
        2 -> "Today"
        1 -> "Tomorrow"
        else -> null
    }
}

private fun dayStartAndTomorrow(): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startToday = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val startTomorrow = cal.timeInMillis
    return startToday to startTomorrow
}

private val Int.hours: Long get() = this * 60L * 60L * 1000L

/* Build a request object that exactly matches your model (no Gson tricks). */
private fun buildRequestObject(
    name: String,
    group: BloodGroup,
    hospital: String,
    location: String,
    contact: String,
    needMillis: Long?
): BloodRequest {
    return BloodRequest(
        requesterName = name.trim(),
        hospitalName = hospital.trim(),
        locationName = location.trim(),
        bloodGroup = group,
        phone = contact.trim(),
        neededOnMillis = needMillis ?: System.currentTimeMillis()
    )
}

/** Stable key for LazyColumn item (no id in the model, so derive one) */
private fun BloodRequest.stableKey(): String =
    "${requesterName}-${phone}-${neededOnMillis}-${bloodGroup}"
