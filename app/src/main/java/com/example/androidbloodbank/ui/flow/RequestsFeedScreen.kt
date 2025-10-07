package com.example.androidbloodbank.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.data.remote.FirebaseRepo
import com.example.androidbloodbank.ui.components.RequestCard
import com.example.androidbloodbank.ui.components.RequestItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavHostController
import com.example.androidbloodbank.navigation.Route

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsFeedScreen(
    repo: LocalRepo,
    firebaseRepo: FirebaseRepo,
    onBack: () -> Unit,
    onRequestNew: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseDatabase.getInstance(SG_DB_URL).reference

    var all by remember { mutableStateOf<List<RequestItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val groups = remember { listOf("All groups") + listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-") }
    var groupFilter by remember { mutableStateOf(groups.first()) }

    // Inline edit (bottom sheet)
    var editing by remember { mutableStateOf<RequestItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /** Load everything from public mirror */
    suspend fun fetchAll() {
        val snap = db.child("requests_public").get().await()
        all = snap.children.map { s ->
            RequestItem(
                id = s.key.orEmpty(),
                ownerUid = s.child("ownerUid").getValue(String::class.java).orEmpty(),
                requesterName = s.child("requesterName").getValue(String::class.java).orEmpty(),
                hospitalName = s.child("hospitalName").getValue(String::class.java).orEmpty(),
                locationName = s.child("locationName").getValue(String::class.java).orEmpty(),
                bloodGroupLabel = s.child("bloodGroup").getValue(String::class.java).orEmpty(),
                phone = s.child("phone").getValue(String::class.java).orEmpty(),
                neededOnMillis = s.child("neededOnMillis").getValue(Long::class.java) ?: 0L
            )
        }.sortedBy { it.neededOnMillis }
    }

    fun refresh() {
        scope.launch {
            loading = true
            runCatching { fetchAll() }
                .onFailure { snackbar.showSnackbar(it.localizedMessage ?: "Failed to load requests") }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val filtered by remember(all, query, groupFilter) {
        derivedStateOf {
            all.filter { item ->
                (groupFilter == "All groups" || item.bloodGroupLabel == groupFilter) &&
                        (query.isBlank() || listOf(item.requesterName, item.hospitalName, item.locationName)
                            .any { it.contains(query.trim(), ignoreCase = true) })
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRequestNew,
                icon = { Icon(Icons.Outlined.WaterDrop, null) },
                text = { Text("Request Blood") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Search name / hospital / location") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("Filter", style = MaterialTheme.typography.labelLarge)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = groupFilter, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    groups.forEach { g ->
                        DropdownMenuItem(text = { Text(g) }, onClick = { groupFilter = g; expanded = false })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val mine = item.ownerUid == uid
                        var isFulfilling by remember { mutableStateOf(false) }

                        // ✅ Handle actions INSIDE confirmValueChange to avoid crashy recompositions
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { newValue ->
                                when (newValue) {
                                    StartToEnd -> { // delete
                                        if (mine) {
                                            scope.launch {
                                                runCatching {
                                                    db.child("requests_public").child(item.id).removeValue().await()
                                                    if (uid.isNotBlank()) {
                                                        db.child("requests").child(uid).child(item.id).removeValue().await()
                                                    }
                                                }.onSuccess {
                                                    snackbar.showSnackbar("Request deleted")
                                                    refresh()
                                                }.onFailure {
                                                    snackbar.showSnackbar(it.localizedMessage ?: "Delete failed")
                                                }
                                            }
                                        } else {
                                            scope.launch { snackbar.showSnackbar("You can delete only your own request") }
                                        }
                                        // Return false so the composable doesn't disappear; we manage list via refresh()
                                        false
                                    }
                                    EndToStart -> { // edit
                                        if (mine) {
                                            // Navigate to dedicated edit screen instead of opening the bottom sheet
                                            navController.navigate("${Route.EditRequest.path}/${item.id}")
                                        } else {
                                            scope.launch { snackbar.showSnackbar("You can edit only your own request") }
                                        }
                                        // Do not actually dismiss the card
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )

                        val cardContent: @Composable () -> Unit = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 👇 ADD THIS to ensure the entire card area is opaque
                                    .background(MaterialTheme.colorScheme.background),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RequestCard(
                                    item = item,
                                    currentUid = uid,
                                    isFulfilling = isFulfilling,
                                    onCall = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(item.phone)}"))
                                        context.startActivity(intent)
                                    },
                                    onFulfill = {
                                        if (!isFulfilling) {
                                            isFulfilling = true
                                            scope.launch {
                                                try {
                                                    firebaseRepo.markRequestFulfilled(item.id)
                                                    snackbar.showSnackbar("Marked as fulfilled")
                                                    refresh()
                                                } catch (e: Exception) {
                                                    snackbar.showSnackbar(e.localizedMessage ?: "Failed to mark fulfilled")
                                                } finally {
                                                    isFulfilling = false
                                                }
                                            }
                                        }
                                    }
                                )

                                // Share button (everyone can share)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                            val whenText = if (item.neededOnMillis > 0)
                                                fmt.format(Date(item.neededOnMillis))
                                            else "ASAP"

                                            val shareText = buildString {
                                                append("🚨 URGENT BLOOD NEEDED: ${item.bloodGroupLabel}\n")
                                                if (item.hospitalName.isNotBlank()) append("🏥 Hospital: ${item.hospitalName}\n")
                                                if (item.locationName.isNotBlank()) append("📍 Location: ${item.locationName}\n")
                                                append("📅 Needed: $whenText\n")
                                                if (item.phone.isNotBlank()) append("📞 Contact: ${item.phone}\n")
                                                if (item.requesterName.isNotBlank()) append("👤 Requester: ${item.requesterName}")
                                            }

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share blood request"))
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }

                        if (mine) {
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = true,  // delete
                                enableDismissFromEndToStart = true,  // edit
                                backgroundContent = {
                                    val isDelete =
                                        dismissState.dismissDirection == StartToEnd || dismissState.currentValue == StartToEnd
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(
                                                if (isDelete) MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (isDelete) Arrangement.Start else Arrangement.End
                                    ) {
                                        if (isDelete) {
                                            Icon(Icons.Outlined.Delete, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Delete")
                                        } else {
                                            Text("Edit")
                                            Spacer(Modifier.width(8.dp))
                                            Icon(Icons.Outlined.Edit, null)
                                        }
                                    }
                                },
                                content = { cardContent() }
                            )
                        } else {
                            cardContent()
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Bottom sheet: Edit form ---------------- */
    if (editing != null) {
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = sheetState
        ) {
            val item = editing!!
            var requesterName by remember(item) { mutableStateOf(item.requesterName) }
            var hospitalName by remember(item) { mutableStateOf(item.hospitalName) }
            var locationName by remember(item) { mutableStateOf(item.locationName) }
            var phone by remember(item) { mutableStateOf(item.phone) }
            var group by remember(item) { mutableStateOf(item.bloodGroupLabel.ifBlank { "O+" }) }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = requesterName,
                    onValueChange = { requesterName = it },
                    label = { Text("Requester name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Hospital") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Blood group dropdown
                var bgExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = bgExpanded, onExpandedChange = { bgExpanded = !bgExpanded }) {
                    OutlinedTextField(
                        value = group,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Blood group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bgExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = bgExpanded, onDismissRequest = { bgExpanded = false }) {
                        listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").forEach { g ->
                            DropdownMenuItem(text = { Text(g) }, onClick = { group = g; bgExpanded = false })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { editing = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    // Build updated model
                                    val updated = BloodRequest(
                                        requesterName = requesterName.trim(),
                                        hospitalName = hospitalName.trim(),
                                        locationName = locationName.trim(),
                                        bloodGroup = labelToEnum(group),
                                        phone = phone.trim(),
                                        neededOnMillis = item.neededOnMillis // keep same date
                                    )

                                    // 1) Update your private owned record
                                    firebaseRepo.updateBloodRequest(item.id, updated)

                                    // 2) Mirror to public feed so the list reflects edits
                                    val publicMap = mapOf(
                                        "ownerUid" to uid,
                                        "requesterName" to updated.requesterName,
                                        "hospitalName" to updated.hospitalName,
                                        "locationName" to updated.locationName,
                                        "bloodGroup" to group, // keep label for public
                                        "phone" to updated.phone,
                                        "neededOnMillis" to updated.neededOnMillis
                                    )
                                    db.child("requests_public").child(item.id).updateChildren(publicMap).await()

                                    snackbar.showSnackbar("Request updated")
                                    editing = null
                                    refresh()
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(e.localizedMessage ?: "Failed to update request")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ---------------- helpers ---------------- */

private fun labelToEnum(s: String): BloodGroup = when (s.trim().uppercase()) {
    "A+" -> BloodGroup.A_POS
    "A-" -> BloodGroup.A_NEG
    "B+" -> BloodGroup.B_POS
    "B-" -> BloodGroup.B_NEG
    "AB+" -> BloodGroup.AB_POS
    "AB-" -> BloodGroup.AB_NEG
    "O+" -> BloodGroup.O_POS
    "O-" -> BloodGroup.O_NEG
    else -> BloodGroup.O_POS
}
