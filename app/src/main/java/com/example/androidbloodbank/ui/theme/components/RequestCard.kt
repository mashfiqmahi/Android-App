package com.example.androidbloodbank.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

// Data classes remain the same
data class RequestItem(
    val id: String,
    val ownerUid: String,
    val requesterName: String,
    val hospitalName: String,
    val locationName: String,
    val bloodGroupLabel: String,
    val phone: String,
    val neededOnMillis: Long
)

data class BloodRequest(
    val requesterName: String,
    val hospitalName: String,
    val locationName: String,
    val bloodGroup: BloodGroup,
    val phone: String,
    val neededOnMillis: Long
)

enum class BloodGroup {
    A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG
}

@Composable
fun RequestCard(
    item: RequestItem,
    currentUid: String,
    isFulfilling: Boolean, // To show a loading state
    onCall: () -> Unit,
    onFulfill: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val neededText = if (item.neededOnMillis > 0) dateFmt.format(Date(item.neededOnMillis)) else "ASAP"
    val isToday = remember(item.neededOnMillis) {
        if (item.neededOnMillis <= 0) false
        else {
            val c1 = Calendar.getInstance().apply { timeInMillis = item.neededOnMillis }
            val c2 = Calendar.getInstance()
            c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }
    }
    val isOwner = item.ownerUid == currentUid

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: name + group badge
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(item.requesterName.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(item.bloodGroupLabel, fontWeight = FontWeight.SemiBold)
                }
            }

            if (item.hospitalName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalHospital, null)
                    Spacer(Modifier.width(10.dp))
                    Text(item.hospitalName)
                }
            }

            if (item.locationName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null)
                    Spacer(Modifier.width(10.dp))
                    Text(item.locationName)
                }
            }

            // Date row (no longer has trailing content)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Event, null)
                Spacer(Modifier.width(10.dp))
                if (isToday) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PriorityHigh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Today")
                            }
                        }
                    )
                } else {
                    Text(neededText)
                }
            }

            // Conditional buttons based on ownership
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isOwner && item.phone.isNotBlank()) {
                    // Call Button - only show to non-owners
                    Button(
                        onClick = onCall,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Call, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call")
                    }
                }

                if (isOwner) {
                    // Mark as Fulfilled Button - only show to owner
                    Button(
                        onClick = onFulfill,
                        enabled = !isFulfilling, // Disable button while fulfilling
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.DoneAll, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFulfilling) "Marking..." else "Mark Fulfilled")
                    }
                }
            }
        }
    }
}


// The second RequestCard overload for editing remains unchanged
@Composable
fun RequestCard(
    req: BloodRequest,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // ... (This function is unchanged)
}