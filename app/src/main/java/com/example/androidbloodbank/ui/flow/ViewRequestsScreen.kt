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
import androidx.compose.runtime.*
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
import com.example.androidbloodbank.data.remote.FirebaseRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

private const val DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRequestsScreen(repo: LocalRepo, onBack: () -> Unit) {
    // Fail-safe load: if anything goes wrong, show empty list instead of crashing
    val requests by remember {
        mutableStateOf(runCatching { repo.loadRequests() }.getOrElse { mutableListOf<BloodRequest>() })
    }
    val firebase = remember { FirebaseRepo() }
    var cloudRequests by remember { mutableStateOf<List<BloodRequest>>(emptyList()) }

    DisposableEffect(Unit) {
        val listener = firebase.observeMyRequests { cloudRequests = it }
        onDispose {
            // Remove if you prefer a single-shot 'get' instead of a live listener:
            FirebaseDatabase.getInstance(DB_URL).reference
                .child("requests").child(FirebaseAuth.getInstance().currentUser?.uid ?: return@onDispose)
                .removeEventListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No requests yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(requests) { index, req ->
                    RequestCard(req = req, key = "${req.requesterName}-$index")
                }
            }
        }
    }
}

@Composable
private fun RequestCard(req: BloodRequest, key: String) {
    val context = LocalContext.current
    // Safe date: if 0 or invalid, use today; format with java.text (no desugaring required)
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

            // Right-side blood group chip
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

                // Call button (replaces phone text)
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
