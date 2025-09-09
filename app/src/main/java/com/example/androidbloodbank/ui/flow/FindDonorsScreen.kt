package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.ui.components.BloodGroupChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDonorsScreen(
    repo: LocalRepo,
    onSelectBG: () -> Unit,   // kept for compatibility; you can remove if unused
    onOpenDonor: (String) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf<BloodGroup?>(null) }
    val all = remember { repo.loadDonors() }
    val donors = remember(selected, all) {
        if (selected == null) all else all.filter { it.bloodGroup == selected }
    }

    Scaffold(
        topBar = {
            TopAppBar( // <- use TopAppBar for wider compatibility
                title = { Text("Find a donor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Select your blood group",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BloodGroupChips(
                selected = selected,
                onSelected = { selected = it }
            )

            Text(
                "Results",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(donors, key = { it.id }) { donor ->
                    DonorListItem(
                        donor = donor,
                        onDetails = { onOpenDonor(donor.id) },
                        onDonate = { onOpenDonor(donor.id) }, // hook to your donate flow if different
                        onCall = { /* TODO open dialer with donor.phone */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun DonorListItem(
    donor: Donor,
    onDetails: () -> Unit,
    onDonate: () -> Unit,
    onCall: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val container = MaterialTheme.colorScheme.surface
    val placeholder = MaterialTheme.colorScheme.surfaceVariant

    // Helpers
    // Helpers
    fun daysSince(millis: Long?): Long? =
        millis?.let { (System.currentTimeMillis() - it) / (1000L * 60 * 60 * 24) }

    val days = daysSince(donor.lastDonationMillis)

// Eligible if 90+ days or unknown
    val eligible = days?.let { it >= 90L } ?: true

// Human text for last donation
    val lastDonationText = days?.let { d ->
        when {
            d < 1L    -> "Today"
            d < 30L   -> "${d}d ago"
            d < 365L  -> "${d / 30L} mo ago"
            else      -> "${d / 365L} yr ago"
        }
    } ?: "No record"

    val locationText = listOfNotNull(donor.area, donor.city).joinToString(separator = ", ").ifBlank { "Location N/A" }

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        color = container,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(placeholder)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            donor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        if (donor.verified) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Verified") },
                                leadingIcon = { Icon(Icons.Outlined.Verified, null) }
                            )
                        }
                        if (eligible) {
                            Spacer(Modifier.width(6.dp))
                            AssistChip(onClick = {}, label = { Text("Eligible") })
                        }
                    }

                    // Location + age
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        val ageText = donor.age?.let { " • Age $it" } ?: ""
                        Text(
                            "$locationText$ageText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Right column: big blood group + call
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        donor.bloodGroup.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onCall) {
                        Icon(Icons.Outlined.Call, contentDescription = "Call")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Hospital row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalHospital,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    donor.hospital ?: "Preferred hospital not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Last donation row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Last donation: $lastDonationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDetails, modifier = Modifier.weight(1f)) {
                    Text("View Details")
                }
                Button(onClick = onDonate, modifier = Modifier.weight(1f)) {
                    Text("Donate")
                }
            }
        }
    }
}
