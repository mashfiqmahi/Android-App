package com.example.androidbloodbank.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Live list of requests (sorted newest first)
    var requests by remember {
        mutableStateOf(
            repo.loadRequests().sortedByDescending { readLong(it, "timestamp", "time") ?: 0L }
        )
    }

    // ---------- LIST ----------
    if (!showingForm) {
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
                    onClick = { showingForm = true },
                    icon = { Icon(Icons.Outlined.Bloodtype, null) },
                    text = { Text("Request Blood") }
                )
            }
        ) { padding ->
            if (requests.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No requests yet. Tap “Request Blood” to post one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(requests) { index, req ->
                        RequestItem(
                            req = req,
                            key = readString(req, "id") ?: readString(req, "requestId")
                            ?: "${readLong(req, "timestamp", "time") ?: 0L}-$index"
                        )
                    }
                }
            }
        }
        return
    }

    // ---------- FORM (full screen) ----------
    RequestBloodFormScreen(
        onBack = { showingForm = false },
        onSubmit = { name, group, hospital, location, contact, needMillis ->
            // 1) Save
            scope.launch {
                val created = buildRequestObject(name, group, hospital, location, contact, needMillis)
                val updated = repo.loadRequests().toMutableList().apply { add(0, created) }
                repo.saveRequests(updated)
                requests = updated.sortedByDescending { readLong(it, "timestamp", "time") ?: 0L }
                snackbar.showSnackbar("Request posted")
            }
            // 2) Close the form immediately (return to list)
            showingForm = false
        }
    )
}

/* ------------------ Form with DATE and immediate close ------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestBloodFormScreen(
    onBack: () -> Unit,
    onSubmit: (name: String, group: BloodGroup, hospital: String, location: String, contact: String, needMillis: Long?) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<BloodGroup?>(null) }
    var hospital by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    var needDateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
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
                title = { Text("Request Blood") },
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
                        // Call parent submit AND close the form via onBack(), guaranteed return to list
                        onSubmit(name.trim(), group!!, hospital.trim(), location.trim(), contact.trim(), needDateMillis)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Submit request") }
        }
    }
}

/* ------------------ List item ------------------ */

@Composable
private fun RequestItem(req: BloodRequest, key: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null); Spacer(Modifier.width(8.dp))
                val who = readString(req, "name", "requesterName") ?: "—"
                Text(who, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                val bg = readString(req, "bloodGroup", "group") ?: "—"
                AssistChip(onClick = {}, label = { Text(bg) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocalHospital, null); Spacer(Modifier.width(8.dp)); Text(readString(req, "hospital") ?: "—") }
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocationOn, null); Spacer(Modifier.width(8.dp)); Text(readString(req, "location", "address") ?: "—") }
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Phone, null); Spacer(Modifier.width(8.dp)); Text(readString(req, "contact", "contactNumber", "phone") ?: "—") }
            val need = readLong(req, "neededDateMillis", "needDate", "dateNeeded", "requiredOn")
            if (need != null && need > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(8.dp))
                    Text("Needed: ${formatDate(need)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            val t = readLong(req, "timestamp", "time")
            if (t != null && t > 0) {
                val df = remember { SimpleDateFormat("dd MMM, yyyy h:mma", Locale.getDefault()) }
                Text(df.format(Date(t)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ------------------ Helpers ------------------ */

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

/** Build a request object tolerant to different data-class field names (using Gson). */
private fun buildRequestObject(
    name: String,
    group: BloodGroup,
    hospital: String,
    location: String,
    contact: String,
    needMillis: Long?
): BloodRequest {
    val payload = mapOf(
        "requesterName" to name, "name" to name,
        "bloodGroup" to label(group), "group" to label(group),
        "hospital" to hospital, "location" to location,
        "contactNumber" to contact, "contact" to contact, "phone" to contact,
        "neededDateMillis" to (needMillis ?: 0L), "needDate" to (needMillis ?: 0L),
        "dateNeeded" to (needMillis ?: 0L), "requiredOn" to (needMillis ?: 0L),
        "timestamp" to System.currentTimeMillis(), "time" to System.currentTimeMillis(),
        "id" to UUID.randomUUID().toString()
    )
    val json = Gson().toJson(payload)
    return Gson().fromJson(json, BloodRequest::class.java)
}

/** Read a String field by trying multiple names. */
private fun readString(any: Any, vararg names: String): String? = runCatching {
    val c = any::class.java
    for (n in names) try {
        val f = c.getDeclaredField(n); f.isAccessible = true
        (f.get(any) as? String)?.let { return it }
    } catch (_: NoSuchFieldException) {}
    null
}.getOrNull()

/** Read a Long/Number field by trying multiple names. */
private fun readLong(any: Any, vararg names: String): Long? = runCatching {
    val c = any::class.java
    for (n in names) try {
        val f = c.getDeclaredField(n); f.isAccessible = true
        val v = f.get(any)
        when (v) { is Long -> return v; is Int -> return v.toLong(); is Number -> return v.toLong(); is String -> return v.toLongOrNull() }
    } catch (_: NoSuchFieldException) {}
    null
}.getOrNull()
