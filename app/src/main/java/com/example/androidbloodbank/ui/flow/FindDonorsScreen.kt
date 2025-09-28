package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.data.remote.FirebaseRepo
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

private const val ELIGIBLE_AFTER_DAYS = 90

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FindDonorsScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firebase = remember { FirebaseRepo() }
    val ctx = LocalContext.current

    var selectedGroup by remember { mutableStateOf<BloodGroup?>(null) }
    var donors by remember { mutableStateOf<List<Donor>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    // fetch donors when group changes
    LaunchedEffect(selectedGroup) {
        val group = selectedGroup ?: return@LaunchedEffect
        loading = true
        try {
            donors = firebase.listDonorsByGroup(group.toString())
        } catch (e: Exception) {
            snackbar.showSnackbar(e.localizedMessage ?: "Failed to load donors.")
            donors = emptyList()
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a donor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Blood group selector (chips) ---
            Text("Select your blood group", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                val groups = listOf(
                    BloodGroup.A_POS, BloodGroup.A_NEG,
                    BloodGroup.B_POS, BloodGroup.B_NEG,
                    BloodGroup.O_POS, BloodGroup.O_NEG,
                    BloodGroup.AB_POS, BloodGroup.AB_NEG
                )
                groups.forEach { bg ->
                    FilterChip(
                        selected = selectedGroup == bg,
                        onClick = { selectedGroup = bg },
                        label = { Text(bg.toString()) }
                    )
                }
            }

            Text("Results", style = MaterialTheme.typography.titleMedium)

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedGroup == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a blood group to search.")
                }
            } else if (donors.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No donors found for ${selectedGroup.toString()}.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = donors, key = { it.hashCode() }) { donor: Donor ->
                        DonorCard(
                            donor = donor,
                            onViewDetails = {
                                scope.launch { snackbar.showSnackbar("Details coming soon") }
                            },
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

@Composable
private fun DonorCard(
    donor: Donor,
    onViewDetails: () -> Unit,
    onCall: (String) -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val container = MaterialTheme.colorScheme.surfaceVariant

    // Derived info
    val daysSince = donor.lastDonationMillis?.let { ts ->
        if (ts > 0L) ((System.currentTimeMillis() - ts) / (1000L * 60 * 60 * 24)).toInt() else null
    }
    val eligible = daysSince == null || daysSince >= ELIGIBLE_AFTER_DAYS
    val location = donor.city?.takeIf { it.isNotBlank() } ?: "Location N/A"
    val lastDonationText = when (daysSince) {
        null -> "No record"
        else -> "$daysSince days ago"
    }
    val phone = donor.phone.orEmpty()
    val canCall = phone.isNotBlank()

    // Use an OutlinedCard with ROUND corners and generous inner padding
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp), // breathing room from screen edges
        colors = CardDefaults.outlinedCardColors(containerColor = surface)
    ) {
        Column(Modifier.padding(18.dp)) { // generous inner padding so border isn't tight
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left: Name + Eligibility (horizontal)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            donor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(10.dp))
                        EligibilityPill(eligible = eligible)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(location, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    // Last donation
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Last donation: $lastDonationText", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Right: Bold blood group with comfy space from border
                Text(
                    donor.bloodGroup.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("View Details")
                }
                Button(
                    onClick = { if (canCall) onCall(phone) },
                    enabled = canCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Call")
                }
            }
        }
    }
}

@Composable
private fun EligibilityPill(eligible: Boolean) {
    val label = if (eligible) "Eligible" else "Not eligible"
    val bg = if (eligible) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val fg = if (eligible) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onErrorContainer

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
