package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.androidbloodbank.ui.util.SafeText
import com.example.androidbloodbank.ui.util.nn


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackRequestScreen(repo: LocalRepo, onBack: () -> Unit) {
    val requests = remember { mutableStateOf(runCatching { repo.loadRequests() }.getOrElse { mutableListOf<BloodRequest>() }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Request") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (requests.value.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No requests to track.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(requests.value) { index, req ->
                    TrackRequestItem(req = req, key = "${req.requesterName}-$index")
                }
            }
        }
    }
}

@Composable
private fun TrackRequestItem(req: BloodRequest, key: String) {
    val context = LocalContext.current
    val safeMillis = if (req.neededOnMillis > 0L) req.neededOnMillis else System.currentTimeMillis()
    val dateStr = remember(safeMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(safeMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        )
    ) {
        Box(Modifier.padding(16.dp)) {

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 0.dp,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = req.bloodGroup.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null); Spacer(Modifier.width(10.dp))
                    SafeText(req.requesterName)

                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalHospital, null); Spacer(Modifier.width(10.dp))
                    SafeText(req.hospitalName)

                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, null); Spacer(Modifier.width(10.dp))
                    SafeText(req.locationName)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(10.dp))
                    Text(dateStr)
                }
                FilledTonalButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${req.phone}"))
                            context.startActivity(intent)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Call, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Call")
                }
            }
        }
    }
}
