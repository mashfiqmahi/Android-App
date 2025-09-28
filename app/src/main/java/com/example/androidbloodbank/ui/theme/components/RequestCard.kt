package com.example.androidbloodbank.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/** Public, shared item model used by both feed + matched screens */
data class RequestItem(
    val id: String,
    val ownerUid: String,
    val requesterName: String,
    val hospitalName: String,
    val locationName: String,
    val bloodGroupLabel: String, // e.g., "A+", "O-"
    val phone: String,
    val neededOnMillis: Long
)

@Composable
fun RequestCard(
    item: RequestItem,
    showCall: Boolean,
    onCall: () -> Unit
) {
    val date = if (item.neededOnMillis > 0)
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.neededOnMillis))
    else "—"
    val days = if (item.neededOnMillis > 0)
        ((item.neededOnMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).toInt()
    else null

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null)
                Spacer(Modifier.width(10.dp))
                Text(
                    item.requesterName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = {}, label = {
                    Text(item.bloodGroupLabel, fontWeight = FontWeight.Bold)
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MedicalServices, null); Spacer(Modifier.width(10.dp))
                Text(item.hospitalName.ifBlank { "—" })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null); Spacer(Modifier.width(10.dp))
                Text(item.locationName.ifBlank { "—" })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(10.dp))
                Text(date)
                Spacer(Modifier.width(10.dp))
                AnimatedVisibility(visible = days != null && days <= 0) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ErrorOutline, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Today")
                            }
                        }
                    )
                }
            }
            if (showCall) {
                FilledTonalButton(onClick = onCall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text("Call")
                }
            }
        }
    }
}
