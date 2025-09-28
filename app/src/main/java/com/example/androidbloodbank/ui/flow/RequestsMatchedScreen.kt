package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.ui.components.RequestCard
import com.example.androidbloodbank.ui.components.RequestItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

// helpers to convert label <-> code
private fun labelToCode(l: String) = when (l) {
    "A+" -> "A_POS"; "A-" -> "A_NEG"
    "B+" -> "B_POS"; "B-" -> "B_NEG"
    "AB+" -> "AB_POS"; "AB-" -> "AB_NEG"
    "O+" -> "O_POS"; else -> "O_NEG"
}
private fun codeToLabel(c: String) = when (c) {
    "A_POS" -> "A+"; "A_NEG" -> "A-"
    "B_POS" -> "B+"; "B_NEG" -> "B-"
    "AB_POS" -> "AB+"; "AB_NEG" -> "AB-"
    "O_POS" -> "O+"; "O_NEG" -> "O-"
    else -> c
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsMatchedScreen(
    repo: LocalRepo,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseDatabase.getInstance(SG_DB_URL).reference

    var userGroup by remember { mutableStateOf<String?>(null) } // label form, e.g. "A+"
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<RequestItem>>(emptyList()) }

    suspend fun loadUserGroup(): String? {
        if (uid.isBlank()) return null
        val snap = db.child("users").child(uid).child("profile").child("bloodGroup").get().await()
        return snap.getValue(String::class.java)
    }

    fun parse(node: DataSnapshot): RequestItem {
        val dbVal = node.child("bloodGroup").getValue(String::class.java).orEmpty()
        val label = if (dbVal.contains("+") || dbVal.contains("-")) dbVal else codeToLabel(dbVal)
        return RequestItem(
            id = node.key.orEmpty(),
            ownerUid = node.child("ownerUid").getValue(String::class.java).orEmpty(),
            requesterName = node.child("requesterName").getValue(String::class.java).orEmpty(),
            hospitalName = node.child("hospitalName").getValue(String::class.java).orEmpty(),
            locationName = node.child("locationName").getValue(String::class.java).orEmpty(),
            bloodGroupLabel = label,
            phone = node.child("phone").getValue(String::class.java).orEmpty(),
            neededOnMillis = node.child("neededOnMillis").getValue(Long::class.java) ?: 0L
        )
    }

    suspend fun loadMatchesFor(label: String) {
        val code = labelToCode(label)

        // Query both representations and merge by key
        val qLabel = db.child("requests_public").orderByChild("bloodGroup").equalTo(label)
        val qCode  = db.child("requests_public").orderByChild("bloodGroup").equalTo(code)

        val s1 = qLabel.get().await()
        val s2 = qCode.get().await()

        val map = linkedMapOf<String, DataSnapshot>()
        s1.children.forEach { map[it.key!!] = it }
        s2.children.forEach { map[it.key!!] = it }

        items = map.values
            .map(::parse)
            .filter { it.ownerUid != uid }              // exclude my own posts
            .sortedBy { it.neededOnMillis }
    }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                val g = loadUserGroup()
                userGroup = g
                if (g.isNullOrBlank()) {
                    items = emptyList()
                    snackbar.showSnackbar("Set your blood group in Profile to see matching requests.")
                } else {
                    loadMatchesFor(g!!)
                }
            } catch (e: Exception) {
                items = emptyList()
                snackbar.showSnackbar(e.localizedMessage ?: "Failed to load requests")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matching requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                userGroup.isNullOrBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your blood group isn’t set.")
                }
                items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching requests from other users.")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        RequestCard(
                            item = item,
                            showCall = true,
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(item.phone)}"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}
