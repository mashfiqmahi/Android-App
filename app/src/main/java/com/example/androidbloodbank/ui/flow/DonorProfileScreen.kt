package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorProfileScreen(
    donorId: String,
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val donors = remember { repo.loadDonors() }

    // Try to locate by id/donorId; if not found, also compare with the fallback id we used in the list.
    val donor = remember(donorId, donors) {
        donors.withIndex().firstOrNull { (i, d) ->
            d.tryGetString("id", "donorId") == donorId || d.fallbackId(i) == donorId
        }?.value ?: donors.firstOrNull() // very safe fallback
    }

    val context = LocalContext.current
    val phone = donor?.phone.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donor Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (donor == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Donor not found.")
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                donor.name ?: "—",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (donor.verified == true) {
                                    Pill("Verified", Icons.Outlined.Verified, MaterialTheme.colorScheme.secondaryContainer)
                                }
                                if (isEligible(donor)) {
                                    Pill("Eligible", Icons.Outlined.EventAvailable, MaterialTheme.colorScheme.tertiaryContainer)
                                }
                            }
                        }

                        PillSmall(donor.bloodGroup.toString())
                    }

                    // Info rows
                    InfoRow(Icons.Outlined.Place, donor.locationOrNA())
                    InfoRow(Icons.Outlined.LocalHospital, donor.preferredHospitalOrDefault())
                    InfoRow(Icons.Outlined.Schedule, "Last donation: ${donor.lastDonationText()}")

                    // Optional: show phone number as text (not a button)
                    if (phone.isNotBlank()) {
                        InfoRow(Icons.Outlined.Call, phone)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Primary action
            Button(
                onClick = {
                    if (phone.isNotBlank()) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                    }
                },
                enabled = phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text("Call")
            }
        }
    }
}

/* ------------------ Reusable rows & pills ------------------ */

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null); Spacer(Modifier.width(10.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
            if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
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
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

/* ------------------ Donor helpers ------------------ */

private fun Donor.fallbackId(index: Int): String =
    "${name}-${phone}-${bloodGroup}-$index"

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
