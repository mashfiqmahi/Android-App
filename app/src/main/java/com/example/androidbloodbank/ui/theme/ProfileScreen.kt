package com.example.androidbloodbank.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.ui.components.BloodGroupChips
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val gson = remember { Gson() }

    // --- Load current user as RAW JSON (works with any User model) ---
    var userJson by remember { mutableStateOf(repo.loadCurrentUserJson()) }
    fun json(): JsonObject = (userJson?.let { JsonParser.parseString(it).asJsonObject } ?: JsonObject())

    // helpers to read & write flexible field names
    fun readField(obj: JsonObject, vararg names: String): String {
        for (n in names) if (obj.has(n) && !obj.get(n).isJsonNull) return obj.get(n).asString
        return ""
    }
    fun writeField(obj: JsonObject, name: String, value: String) {
        if (value.isBlank()) obj.addProperty(name, null as String?) else obj.addProperty(name, value)
    }

    // editable copies shown in UI
    var name by remember { mutableStateOf(readField(json(), "name", "fullName", "username")) }
    var email by remember { mutableStateOf(readField(json(), "email", "mail")) }
    var phone by remember { mutableStateOf(readField(json(), "phone", "phoneNumber", "mobile")) }

    // Donor profile (same info used in Find a donor)
    var donor by remember {
        mutableStateOf(
            repo.loadMyDonor() ?: Donor(
                name = name,
                bloodGroup = BloodGroup.O_POS,
                phone = phone,
                verified = false,
                age = null,
                city = "",
                area = "",
                lastDonationMillis = null,
                hospital = ""
            )
        )
    }

    var editing by remember { mutableStateOf(false) }

    fun showDatePicker() {
        if (!editing) return
        val cal = Calendar.getInstance().apply {
            timeInMillis = donor.lastDonationMillis ?: System.currentTimeMillis()
        }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                donor = donor.copy(lastDonationMillis = picked)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun formatDate(millis: Long?): String =
        millis?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Not set"

    fun daysSince(millis: Long?): Long? =
        millis?.let { (System.currentTimeMillis() - it) / (1000L * 60 * 60 * 24) }

    val eligible = (daysSince(donor.lastDonationMillis)?.let { it >= 90L }) ?: true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (editing) {
                        TextButton(onClick = {
                            // Cancel: reload from stored JSON and donor
                            userJson = repo.loadCurrentUserJson()
                            val obj = json()
                            name  = readField(obj, "name", "fullName", "username")
                            email = readField(obj, "email", "mail")
                            phone = readField(obj, "phone", "phoneNumber", "mobile")
                            donor = repo.loadMyDonor() ?: donor.copy(name = name, phone = phone)
                            editing = false
                        }) { Text("Cancel") }

                        TextButton(onClick = {
                            // Save: update JSON, save donor, leave edit mode
                            val obj = json()
                            writeField(obj, "name", name.trim())
                            writeField(obj, "email", email.trim())
                            writeField(obj, "phone", phone.trim())
                            userJson = gson.toJson(obj)
                            repo.saveCurrentUserJson(userJson)

                            donor = donor.copy(name = name.trim(), phone = phone.trim())
                            repo.saveMyDonor(donor)

                            editing = false
                        }) { Text("Save", fontWeight = FontWeight.SemiBold) }
                    } else {
                        TextButton(onClick = { editing = true }) { Text("Edit") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // --- Account (view or edit) ---
            Text("Account", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full name") }, singleLine = true,
                readOnly = !editing, enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                readOnly = !editing, enabled = editing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone") }, singleLine = true,
                readOnly = !editing, enabled = editing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // --- Donor preferences (same info as Find a donor) ---
            Divider()
            Text("Donor preferences", style = MaterialTheme.typography.titleMedium)

            Text("Blood group", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BloodGroupChips(
                selected = donor.bloodGroup,
                onSelected = { if (editing) donor = donor.copy(bloodGroup = it) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = donor.city.orEmpty(),
                    onValueChange = { if (editing) donor = donor.copy(city = it) },
                    label = { Text("City") },
                    singleLine = true, readOnly = !editing, enabled = editing,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = donor.area.orEmpty(),
                    onValueChange = { if (editing) donor = donor.copy(area = it) },
                    label = { Text("Area") },
                    singleLine = true, readOnly = !editing, enabled = editing,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = donor.age?.toString().orEmpty(),
                onValueChange = { v -> if (editing) donor = donor.copy(age = v.filter { it.isDigit() }.toIntOrNull()) },
                label = { Text("Age") },
                singleLine = true, readOnly = !editing, enabled = editing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = donor.hospital.orEmpty(),
                onValueChange = { if (editing) donor = donor.copy(hospital = it) },
                label = { Text("Preferred hospital/clinic") },
                singleLine = true, readOnly = !editing, enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Last donation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker() }, enabled = editing) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(donor.lastDonationMillis))
                }
                if (editing && donor.lastDonationMillis != null) {
                    TextButton(onClick = { donor = donor.copy(lastDonationMillis = null) }) { Text("Clear") }
                }
            }
            AssistChip(
                onClick = {},
                label = { Text(if (eligible) "Eligible (90+ days)" else "Not yet eligible") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (eligible) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            )

            // --- Logout ---
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    // Call logout to clear session data
                    repo.logoutCurrentUser()
                    // Navigate back to the Gate screen after logout
                    onLoggedOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Log out") }
        }
    }
}
