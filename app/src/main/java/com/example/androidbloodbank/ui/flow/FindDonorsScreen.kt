package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDonorsScreen(
    repo: LocalRepo,
    onSelectBG: () -> Unit,            // kept to match AppNavHost signature
    onOpenDonor: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedGroup by remember { mutableStateOf<BloodGroup?>(null) }

    val all = remember { repo.loadDonors() }
    val donors = remember(selectedGroup, all) {
        all.filter { selectedGroup == null || it.bloodGroup == selectedGroup }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a donor") },
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
            Text(
                "Select your blood group",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BloodGroupGrid(selected = selectedGroup, onSelected = { selectedGroup = it })

            Text("Results", style = MaterialTheme.typography.titleMedium)

            if (donors.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No donors found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(donors, key = { i, d -> d.idOrFallback(i) }) { i, donor ->
                        DonorCard(
                            donor = donor,
                            onView = { onOpenDonor(donor.idOrFallback(i)) }
                        )
                    }
                }
            }
        }
    }
}

/* ------------------ Card ------------------ */

@Composable
private fun DonorCard(
    donor: Donor,
    onView: () -> Unit
) {
    val context = LocalContext.current
    val phone = donor.phone ?: ""

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: avatar, name, badges, blood group on right
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        donor.name ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (donor.verified == true) {
                            Pill(
                                label = "Verified",
                                icon = Icons.Outlined.Verified,
                                bg = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        if (isEligible(donor)) {
                            Pill(
                                label = "Eligible",
                                icon = Icons.Outlined.EventAvailable,
                                bg = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        }
                    }
                }

                PillSmall(label = donor.bloodGroup.toString())
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, null); Spacer(Modifier.width(8.dp))
                Text(donor.locationOrNA(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalHospital, null); Spacer(Modifier.width(8.dp))
                Text(donor.preferredHospitalOrDefault(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null); Spacer(Modifier.width(8.dp))
                Text("Last donation: ${donor.lastDonationText()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("View Details") }

                Button(
                    onClick = {
                        if (phone.isNotBlank()) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        }
                    },
                    enabled = phone.isNotBlank(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text("Call")
                }
            }
        }
    }
}

/* ------------------ Small UI helpers (non-clickable pills) ------------------ */

@Composable
private fun Pill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, bg: Color) {
    Surface(
        color = bg,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PillSmall(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/* ------------------ Blood group selector ------------------ */

@Composable
private fun BloodGroupGrid(selected: BloodGroup?, onSelected: (BloodGroup?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rows = listOf(
            listOf(BloodGroup.A_POS, BloodGroup.A_NEG, BloodGroup.B_POS, BloodGroup.B_NEG),
            listOf(BloodGroup.O_POS, BloodGroup.O_NEG, BloodGroup.AB_POS, BloodGroup.AB_NEG)
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { bg ->
                    val sel = selected == bg
                    FilterChip(
                        selected = sel,
                        onClick = { onSelected(if (sel) null else bg) },
                        label = { Text(bg.toString()) },
                        leadingIcon = if (sel) ({ Icon(Icons.Outlined.Check, null) }) else null
                    )
                }
            }
        }
    }
}

/* ------------------ Donor reflection helpers ------------------ */

private fun Donor.idOrFallback(index: Int = 0): String =
    tryGetString("id", "donorId") ?: "${name}-${phone}-${bloodGroup}-$index"

private fun Donor.locationOrNA(): String =
    tryGetString("location", "address", "city").orEmpty().ifBlank { "Location N/A" }

private fun Donor.preferredHospitalOrDefault(): String =
    tryGetString("preferredHospital", "hospital", "hospitalName").orEmpty()
        .ifBlank { "Preferred hospital not set" }

private fun Donor.lastDonationText(): String {
    val ms = tryGetLong("lastDonationMillis", "lastDonation", "lastDonatedAt") ?: 0L
    if (ms <= 0L) return "No record"
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return df.format(Date(ms))
}

private fun Donor.tryGetString(vararg names: String): String? = runCatching {
    val c = this::class.java
    for (n in names) try {
        val f = c.getDeclaredField(n); f.isAccessible = true
        (f.get(this) as? String)?.let { return it }
    } catch (_: NoSuchFieldException) { /* keep trying */ }
    null
}.getOrNull()

private fun Donor.tryGetLong(vararg names: String): Long? = runCatching {
    val c = this::class.java
    for (n in names) try {
        val f = c.getDeclaredField(n); f.isAccessible = true
        val v = f.get(this)
        when (v) {
            is Long -> return v
            is Int -> return v.toLong()
            is Number -> return v.toLong()
            is String -> return v.toLongOrNull()
        }
    } catch (_: NoSuchFieldException) { }
    null
}.getOrNull()

/** Heuristic: eligible if never donated, or last donation >= 120 days ago. */
private fun isEligible(d: Donor): Boolean {
    val last = d.tryGetLong("lastDonationMillis", "lastDonation", "lastDonatedAt") ?: return true
    val days = (System.currentTimeMillis() - last) / (24L * 60 * 60 * 1000)
    return days >= 120
}
