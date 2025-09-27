package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.data.remote.FirebaseRepo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDonorsScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firebase = remember { FirebaseRepo() }
    val ctx = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<BloodGroup?>(null) }
    var donors by remember { mutableStateOf<List<Donor>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    // Fetch when selection changes
    LaunchedEffect(selectedGroup) {
        val group = selectedGroup ?: return@LaunchedEffect
        loading = true
        try {
            donors = firebase.listDonorsByGroup(group.toString())
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to load donors.")
            donors = emptyList()
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Donors") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---- Blood group selector ----
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { v: Boolean -> expanded = v },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedGroup?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Blood group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BloodGroup.values().forEach { bg ->
                        DropdownMenuItem(
                            text = { Text(bg.toString()) },
                            onClick = {
                                selectedGroup = bg
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedGroup == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a blood group to search.")
                }
            } else {
                if (donors.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No donors found for ${selectedGroup.toString()}.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(donors) { donor ->
                            DonorCard(
                                donor = donor,
                                onCall = { number: String ->
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                    ctx.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonorCard(
    donor: Donor,
    onCall: (String) -> Unit
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = donor.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            AssistChip(
                onClick = {},
                label = { Text(donor.bloodGroup.toString()) }
            )
        }

        Spacer(Modifier.height(8.dp))

        donor.city?.takeIf { it.isNotBlank() }?.let { city ->
            Text("Location: $city", style = MaterialTheme.typography.bodyMedium)
        }

        donor.lastDonationMillis?.let { ts ->
            val days = ((System.currentTimeMillis() - ts) / (1000L * 60 * 60 * 24)).toInt()
            Text("Last donation: ${if (days >= 0) "$days days ago" else "N/A"}",
                style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))

        val phone = donor.phone
        if (!phone.isNullOrBlank()) {
            OutlinedButton(
                onClick = { onCall(phone) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Outlined.Call, contentDescription = "Call")
                Spacer(Modifier.width(8.dp))
                Text("Call $phone")
            }
        }
    }
}
