package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.Donor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val donors = remember {
        val users = repo.loadUsers()
        // Map each user to your existing Donor UI model (phone/flags left null/false)
        users.map { u ->
            Donor(
                name = u.name,
                bloodGroup = u.bloodGroup
                // phone = null, verified = false, etc. (defaults are fine)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency SOS (offline)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (donors.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No offline donors available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(donors, key = { it.id }) { d ->
                    EmergencyDonorRow(
                        donor = d,
                        onCall = {
                            d.phone?.let { ph ->
                                val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ph"))
                                context.startActivity(i)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyDonorRow(
    donor: Donor,
    onCall: () -> Unit
) {
    val border = MaterialTheme.colorScheme.outlineVariant
    val placeholder = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, border)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Placeholder avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(placeholder)
                )
                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(donor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        val loc = listOfNotNull(donor.area, donor.city).joinToString(", ").ifBlank { "—" }
                        Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(donor.bloodGroup.label, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onCall, enabled = donor.phone != null) {
                        Icon(Icons.Outlined.Call, contentDescription = "Call")
                    }
                }
            }

            if (!donor.hospital.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Pref. hospital: ${donor.hospital}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
