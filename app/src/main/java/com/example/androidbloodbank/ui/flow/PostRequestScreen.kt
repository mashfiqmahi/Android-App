package com.example.androidbloodbank.ui.flow

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    repo: LocalRepo,
    onPosted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<BloodGroup?>(null) }
    var needDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Done") } }
        ) {
            val state = rememberDatePickerState(initialSelectedDateMillis = needDateMillis)
            DatePicker(state = state, title = { Text("Needed on") })
            LaunchedEffect(state.selectedDateMillis) {
                state.selectedDateMillis?.let { needDateMillis = it }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Request") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
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
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            BloodGroupDropdown(selected = group, onSelected = { group = it })

            OutlinedTextField(
                value = hospital, onValueChange = { hospital = it },
                label = { Text("Hospital") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Contact number") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(8.dp))
                Text(formatDate(needDateMillis))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (submitting) return@Button
                    val n = name.trim()
                    val h = hospital.trim()
                    val l = location.trim()
                    val p = phone.trim()
                    val g = group

                    if (n.isEmpty() || g == null || p.isEmpty()) {
                        Toast.makeText(context, "Please fill name, blood group and contact", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    submitting = true
                    runCatching {
                        val request = BloodRequest(
                            requesterName = name.trim(),
                            hospitalName  = hospital.trim(),
                            locationName  = location.trim(),
                            phone         = phone.trim(),
                            bloodGroup    = group!!,
                            neededOnMillis = needDateMillis
                        )

                        val current = runCatching { repo.loadRequests() }.getOrElse { mutableListOf() }
                        current.add(0, request)
                        repo.saveRequests(current)
                    }.onSuccess {
                        onPosted() // pop back (as wired in AppNavHost)
                    }.onFailure { e ->
                        submitting = false
                        Toast.makeText(context, "Failed to submit: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (submitting) "Submitting..." else "Submit request") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodGroupDropdown(
    selected: BloodGroup?,
    onSelected: (BloodGroup) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val all = BloodGroup.values()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected?.toString() ?: "Select blood group",
            onValueChange = {}, readOnly = true,
            label = { Text("Blood group") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            all.forEach { bg ->
                DropdownMenuItem(text = { Text(bg.toString()) }, onClick = { onSelected(bg); expanded = false })
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(millis))
