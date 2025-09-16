package com.example.androidbloodbank.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ---------- small utils ----------
private fun formatDate(millis: Long?): String {
    if (millis == null || millis == 0L) return "Not set"
    return try {
        if (Build.VERSION.SDK_INT >= 26) {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val dt = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            fmt.format(dt)
        } else {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(millis))
        }
    } catch (_: Exception) { "Not set" }
}

private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
private val bloodLabels = listOf("A+","A-","B+","B-","AB+","AB-","O+","O-")
private fun labelToEnum(label: String) = when (label) {
    "A+" -> BloodGroup.A_POS; "A-" -> BloodGroup.A_NEG
    "B+" -> BloodGroup.B_POS; "B-" -> BloodGroup.B_NEG
    "AB+" -> BloodGroup.AB_POS; "AB-" -> BloodGroup.AB_NEG
    "O+" -> BloodGroup.O_POS; else -> BloodGroup.O_NEG
}

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scroll = rememberScrollState()

    // pull whatever you saved last time; safe parsing with regex
    val current = remember { repo.loadCurrentUserJson() }
    fun pick(field: String, def: String = "") =
        Regex("\"$field\"\\s*:\\s*\"([^\"]*)\"").find(current ?: "")?.groupValues?.getOrNull(1) ?: def
    fun pickLong(field: String) =
        Regex("\"$field\"\\s*:\\s*(\\d+)").find(current ?: "")?.groupValues?.getOrNull(1)?.toLongOrNull()

    var editing by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(pick("name", "Your name")) }
    var email by remember { mutableStateOf(pick("email")) }
    var phone by remember { mutableStateOf(pick("phone")) }
    var gender by remember { mutableStateOf(pick("gender")) }
    var address by remember { mutableStateOf(pick("address")) }
    var age by remember { mutableStateOf(Regex("\"age\"\\s*:\\s*(\\d+)").find(current ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0) }
    var bloodLabel by remember { mutableStateOf(pick("bloodGroup").ifBlank { "O+" }.let { if (it in bloodLabels) it else "O+" }) }
    var lastDonationMillis by remember { mutableStateOf<Long?>(pickLong("lastDonationMillis")) }
    var photoUri by remember { mutableStateOf(pick("photoUri").ifBlank { null }) }

    // date picker
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Done") }
            }
        ) {
            val state = rememberDatePickerState(initialSelectedDateMillis = lastDonationMillis ?: System.currentTimeMillis())
            DatePicker(state = state, title = { Text("Last donation date") })
            LaunchedEffect(state.selectedDateMillis) { lastDonationMillis = state.selectedDateMillis }
        }
    }

    // image picker: OpenDocument so we can persist access across restarts
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Persist read permission so image still loads after reboot
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* some providers may not support persist; it's ok */ }
            photoUri = uri.toString()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit Profile" else "Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } },
                actions = {
                    if (!editing) {
                        TextButton(onClick = { editing = true }) { Text("Edit") }
                    } else {
                        TextButton(onClick = {
                            // build and persist a tiny JSON snapshot (add photoUri)
                            val json = buildString {
                                append("{")
                                append("\"name\":\"${name.trim()}\",")
                                append("\"email\":\"${email.trim()}\",")
                                append("\"phone\":\"${phone.trim()}\",")
                                append("\"gender\":\"${gender.trim()}\",")
                                append("\"address\":\"${address.trim()}\",")
                                append("\"age\":${age.coerceAtLeast(0)},")
                                append("\"bloodGroup\":\"$bloodLabel\",")
                                append("\"lastDonationMillis\":${lastDonationMillis ?: 0L},")
                                append("\"photoUri\":\"${photoUri ?: ""}\"")
                                append("}")
                            }
                            repo.saveCurrentUserJson(json)
                            scope.launch { snack.showSnackbar("Profile saved") }
                            editing = false
                        }) { Text("Save", fontWeight = FontWeight.SemiBold) }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .imePadding()
        ) {

            // Header with gradient & avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF6F00), Color(0xFFFF8A80)) // orange -> soft coral
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                            .let { base ->
                                if (editing) base.clickable { pickImage.launch(arrayOf("image/*")) } else base
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile photo",
                                modifier = Modifier.matchParentSize().clip(CircleShape)
                            )
                        } else {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                        if (editing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFF6D4C41), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text = name.ifBlank { "Your name" }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (email.isNotBlank()) Text(text = email, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Account Info", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow(Icons.Outlined.Badge, "Full name") {
                    if (editing) OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    else Text(name.ifBlank { "—" })
                }
                InfoRow(Icons.Outlined.Phone, "Mobile") {
                    if (editing) OutlinedTextField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    else Text(phone.ifBlank { "—" })
                }
                InfoRow(Icons.Outlined.Email, "Email") {
                    if (editing) OutlinedTextField(value = email, onValueChange = { email = it }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    else Text(email.ifBlank { "—" })
                }
                InfoRow(Icons.Outlined.Wc, "Gender") {
                    if (editing) ExposedDropdown(current = gender.ifBlank { genderOptions.last() }, options = genderOptions) { gender = it }
                    else Text(gender.ifBlank { "—" })
                }
                InfoRow(Icons.Outlined.Home, "Address") {
                    if (editing) OutlinedTextField(value = address, onValueChange = { address = it }, singleLine = false, minLines = 2, modifier = Modifier.fillMaxWidth())
                    else Text(address.ifBlank { "—" })
                }
                InfoRow(Icons.Outlined.Bloodtype, "Blood group") {
                    if (editing) ExposedDropdown(current = bloodLabel, options = bloodLabels) { bloodLabel = it }
                    else Text(bloodLabel)
                }
                InfoRow(Icons.Outlined.Cake, "Age") {
                    if (editing) OutlinedTextField(
                        value = (age.takeIf { it > 0 }?.toString() ?: ""),
                        onValueChange = { age = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(120.dp)
                    ) else Text(if (age > 0) "$age" else "—")
                }
                InfoRow(Icons.Outlined.Event, "Last donation") {
                    if (editing) OutlinedButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(8.dp)); Text(formatDate(lastDonationMillis))
                    } else Text(formatDate(lastDonationMillis))
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        repo.logoutCurrentUser()
                        onLoggedOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log out")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    valueContent: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                valueContent()
            }
        }
        Spacer(Modifier.height(10.dp)); Divider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = current, onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
