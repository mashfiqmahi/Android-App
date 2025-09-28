package com.example.androidbloodbank.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Your SG RTDB endpoint (matches your console screenshot)
private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseDebugScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    var resultLines by remember { mutableStateOf(listOf<String>()) }
    fun log(line: String) { resultLines = resultLines + line }

    suspend fun writeProbe(toSg: Boolean): String {
        val db = if (toSg) FirebaseDatabase.getInstance(SG_DB_URL) else FirebaseDatabase.getInstance()
        val root = if (toSg) "SG" else "DEFAULT"
        val now = System.currentTimeMillis()
        val needTomorrow = now + 24*60*60*1000

        // Payload that satisfies your rules (requires ownerUid & fields)
        val payload = mapOf(
            "ownerUid" to uid,
            "requesterName" to "PROBE_$root",
            "hospitalName" to "Debug Hospital",
            "locationName" to "Debug City",
            "bloodGroup" to "O+",
            "phone" to "000",
            "neededOnMillis" to needTomorrow,
            "createdAt" to now
        )

        val ref = db.reference.child("requests").child(uid).push()
        return try {
            ref.setValue(payload).await()
            "✅ WROTE to $root DB at /requests/$uid/${ref.key}"
        } catch (e: Exception) {
            "❌ FAILED on $root DB → ${e.javaClass.simpleName}: ${e.message ?: "error"}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firebase Debug", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Signed in UID:", style = MaterialTheme.typography.labelLarge)
            Text(if (uid.isBlank()) "— (NOT SIGNED IN)" else uid,
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

            ElevatedButton(
                onClick = {
                    scope.launch {
                        resultLines = emptyList()
                        if (uid.isBlank()) {
                            snack.showSnackbar("You are not signed in.")
                            return@launch
                        }
                        log("Trying write to SG database…")
                        log(writeProbe(toSg = true))
                        log("Trying write to DEFAULT project database…")
                        log(writeProbe(toSg = false))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Run Write Tests") }

            Divider()

            Text("Results", style = MaterialTheme.typography.titleMedium)
            resultLines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Open your console at the SG URL and look for /requests/$uid/…\n" +
                        "If only DEFAULT succeeds, the app is pointed to your default DB.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
