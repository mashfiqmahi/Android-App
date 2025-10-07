package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

private fun codeToLabel(c: String) = when (c) {
    "A_POS" -> "A+"; "A_NEG" -> "A-"
    "B_POS" -> "B+"; "B_NEG" -> "B-"
    "AB_POS" -> "AB+"; "AB_NEG" -> "AB-"
    "O_POS" -> "O+"; "O_NEG" -> "O-"
    else -> c
}

private fun daysSince(millis: Long?): Long? =
    millis?.takeIf { it > 0 }?.let { (System.currentTimeMillis() - it) / (1000L * 60 * 60 * 24) }

private fun formatDate(millis: Long?): String =
    millis?.takeIf { it > 0 }?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Not set"

data class DonorDetails(
    val uid: String = "",
    val name: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "",
    val lastDonationMillis: Long = 0L,
    val eligible: Boolean = true, // Default to true if no data
    val gender: String = "",
    val age: Int = 0,
    val address: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDetailsScreen(
    donorUid: String,
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseDatabase.getInstance(SG_DB_URL).reference }
    val context = LocalContext.current        // <-- use this

    var details by remember { mutableStateOf<DonorDetails?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun load(uid: String): DonorDetails {
        // Ensure we have an auth session (anonymous is fine for public reads)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            runCatching { auth.signInAnonymously().await() }
        }

        // 1) Public donor card (allowed by rules)
        val pub = db.child("donors_public").child(uid).get().await()

        // If there is no public donor record at all, return minimal info
        if (!pub.exists()) {
            // Default eligibility to false if no donor record exists
            return DonorDetails(uid = uid, eligible = false)
        }

        val bgRaw = pub.child("bloodGroup").getValue(String::class.java) ?: ""
        val bg = if (bgRaw.contains("+") || bgRaw.contains("-")) bgRaw else codeToLabel(bgRaw)

        val last = pub.child("lastDonationMillis").getValue(Long::class.java) ?: 0L

        // ✅ FIX for Inconsistency: Prioritize the explicit 'eligible' field from DB (if present),
        // as the Donor List Screen seems to be using it.
        val elig = pub.child("eligible").getValue(Boolean::class.java) ?: run {
            // Fallback to calculation if 'eligible' field is missing:
            if (last == 0L) {
                true // Never donated is eligible
            } else {
                // Calculate if days since last donation is >= 120 (standard recovery time)
                val days = (System.currentTimeMillis() - last) / (1000L * 60 * 60 * 24)
                days >= 120
            }
        }


        // Prefer public fields
        var email: String = pub.child("email").getValue(String::class.java) ?: ""
        val name = pub.child("name").getValue(String::class.java) ?: ""
        val phone = pub.child("phone").getValue(String::class.java) ?: ""
        // FIX: Read from 'location' field
        val publicLocation = pub.child("location").getValue(String::class.java) ?: ""


        // Private profile only for the owner (per rules)
        var gender: String = ""
        var age: Int = 0
        var address: String = ""

        if (auth.currentUser?.uid == uid) {
            // FIX: Prioritize Auth email for owner view
            email = auth.currentUser?.email ?: ""

            runCatching {
                val prof = db.child("users").child(uid).child("profile").get().await()

                // Read private details
                gender = prof.child("gender").getValue(String::class.java) ?: ""
                age = prof.child("age").getValue(Int::class.java)
                    ?: (prof.child("age").getValue(Long::class.java)?.toInt() ?: 0)
                // Use the private 'location' field (which stores address) for the detailed InfoRow
                address = prof.child("location").getValue(String::class.java) ?: ""
            }
        }

        return DonorDetails(
            uid = uid,
            name = name,
            bloodGroup = bg,
            phone = phone,
            email = email,
            location = publicLocation,
            lastDonationMillis = last,
            eligible = elig, // Use the resolved value
            gender = gender,
            age = age,
            address = address
        )
    }



    LaunchedEffect(donorUid) {
        loading = true
        runCatching { load(donorUid) }
            .onSuccess { details = it }
            .onFailure { scope.launch { snackbar.showSnackbar(it.localizedMessage ?: "Failed to load donor") } }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donor details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val d = details
        if (d == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No donor found")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(d.name.ifBlank { "Unnamed donor" },
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
                                Spacer(Modifier.width(6.dp))
                                Text(d.location.ifBlank { "Unknown location" },
                                    color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d.bloodGroup.ifBlank { "—" },
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

//                    val ok = d.eligible
//                    AssistChip(
//                        onClick = {},
//                        label = { Text(if (ok) "Eligible to donate" else "Not eligible now") },
//                        leadingIcon = {
//                            Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Block, null)
//                        },
//                        colors = AssistChipDefaults.assistChipColors(
//                            containerColor = if (ok) Color(0x3328A745) else Color(0x33DC3545),
//                            labelColor = Color.White
//                        )
//                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick actions (use LocalContext instead of 'it')
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        if (d.phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(d.phone)}"))
                            context.startActivity(intent)    // <-- fixed
                        } else {
                            scope.launch { snackbar.showSnackbar("No phone number") }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text("Call") }

                FilledTonalButton(
                    onClick = {
                        if (d.phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${Uri.encode(d.phone)}"))
                            context.startActivity(intent)    // <-- fixed
                        } else {
                            scope.launch { snackbar.showSnackbar("No phone number") }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Outlined.Sms, null); Spacer(Modifier.width(8.dp)); Text("SMS") }
            }

            Spacer(Modifier.height(16.dp))

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(Icons.Outlined.Bloodtype, "Blood group", d.bloodGroup)
                InfoRow(Icons.Outlined.Event, "Last donation", formatDate(d.lastDonationMillis) +
                        (daysSince(d.lastDonationMillis)?.let { "  •  $it days ago" } ?: ""))
                InfoRow(Icons.Outlined.Phone, "Phone", d.phone.ifBlank { "—" })
                InfoRow(Icons.Outlined.Email, "Email", d.email.ifBlank { "—" })
                if (d.gender.isNotBlank()) InfoRow(Icons.Outlined.Wc, "Gender", d.gender)
                if (d.age > 0) InfoRow(Icons.Outlined.Cake, "Age", d.age.toString())
                if (d.address.isNotBlank()) InfoRow(Icons.Outlined.Home, "Address", d.address)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}