package com.example.androidbloodbank.ui.flow
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.data.model.Donor
import com.example.androidbloodbank.data.remote.FirebaseRepo
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.navigation.navArgument
private const val ELIGIBLE_AFTER_DAYS = 90

// Static district list (exact spellings you gave)
private val BD_DISTRICTS = listOf(
    "Dhaka","Faridpur","Gazipur","Gopalganj","Jamalpur","Kishoreganj","Madaripur",
    "Manikganj","Munshiganj","Mymensingh","Narayanganj","Narsingdi","Netrokona",
    "Rajbari","Shariatpur","Sherpur","Tangail","Bogra","Joypurhat","Naogaon",
    "Natore","Nawabganj","Pabna","Rajshahi","Sirajgonj","Dinajpur","Gaibandha",
    "Kurigram","Lalmonirhat","Nilphamari","Panchagarh","Rangpur","Thakurgaon",
    "Barguna","Barisal","Bhola","Jhalokati","Patuakhali","Pirojpur","Bandarban",
    "Brahmanbaria","Chandpur","Chittagong","Comilla","Cox's Bazar","Feni",
    "Khagrachari","Lakshmipur","Noakhali","Rangamati","Habiganj","Maulvibazar",
    "Sunamganj","Sylhet","Bagerhat","Chuadanga","Jessore","Jhenaidah","Khulna",
    "Kushtia","Magura","Meherpur","Narail","Satkhira"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FindDonorsScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onViewDetails: (String) -> Unit,
) {
    // ---- state / deps ----
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firebase = remember { FirebaseRepo() }
    val ctx = LocalContext.current

    var selectedGroup by remember { mutableStateOf<BloodGroup?>(null) }
    var donors by remember { mutableStateOf<List<Donor>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    // District filter UI
    var district by remember { mutableStateOf<String?>(null) }
    var expandDistrict by remember { mutableStateOf(false) }

    // Load donors when blood group OR district changes
    LaunchedEffect(selectedGroup, district) {
        val bg = selectedGroup ?: return@LaunchedEffect
        loading = true
        try {
            donors = firebase.listDonorsByGroup(bg.toString(), district)
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar(e.localizedMessage ?: "Failed to load donors") }
            donors = emptyList()
        } finally { loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a donor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Blood group chips
            Text("Select your blood group", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    BloodGroup.A_POS, BloodGroup.A_NEG,
                    BloodGroup.B_POS, BloodGroup.B_NEG,
                    BloodGroup.O_POS, BloodGroup.O_NEG,
                    BloodGroup.AB_POS, BloodGroup.AB_NEG
                ).forEach { bg ->
                    FilterChip(
                        selected = selectedGroup == bg,
                        onClick = { selectedGroup = bg },
                        label = { Text(bg.toString()) }
                    )
                }
            }

            // District dropdown (simple + always visible; enabled after BG chosen)
            Text("Filter by district", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = expandDistrict,
                onExpandedChange = {
                    if (selectedGroup != null) expandDistrict = !expandDistrict
                }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = district ?: "All districts",
                    onValueChange = {},
                    readOnly = true,
                    enabled = selectedGroup != null,
                    label = { Text("District") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandDistrict) }
                )
                ExposedDropdownMenu(
                    expanded = expandDistrict,
                    onDismissRequest = { expandDistrict = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All districts") },
                        onClick = {
                            district = null
                            expandDistrict = false
                        }
                    )
                    BD_DISTRICTS.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d) },
                            onClick = {
                                district = d
                                expandDistrict = false
                            }
                        )
                    }
                }
            }

            Text("Results", style = MaterialTheme.typography.titleMedium)
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                selectedGroup == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a blood group to search.")
                }
                donors.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val g = selectedGroup.toString()
                    val d = district ?: "all districts"
                    Text("No donors found for $g in $d.")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = donors, key = { it.id }) { donor ->
                        DonorCard(
                            donor = donor,
                            onViewDetails = { onViewDetails(donor.id) },  // <-- keep this
                            onCall = { number ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                ctx.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonorCard(
    donor: Donor,
    onViewDetails: () -> Unit,
    onCall: (String) -> Unit
) {
    val daysSince = donor.lastDonationMillis?.let { ts ->
        if (ts > 0) ((System.currentTimeMillis() - ts) / (1000L * 60 * 60 * 24)).toInt() else null
    }
    val eligible = daysSince == null || daysSince >= ELIGIBLE_AFTER_DAYS
    val location = donor.city?.takeIf { it.isNotBlank() } ?: "Location N/A"
    val lastDonationText = daysSince?.let { "$it days ago" } ?: "No record"
    val phone = donor.phone.orEmpty()
    val canCall = phone.isNotBlank()

    OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(donor.name ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(location, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Last donation: $lastDonationText", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    EligibilityPill(eligible)
                }
                Text(
                    donor.bloodGroup.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onViewDetails, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Text("View Details")
                }
                Button(
                    onClick = { if (canCall) onCall(phone) },
                    enabled = canCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call")
                }
            }
        }
    }
}

@Composable
private fun EligibilityPill(eligible: Boolean) {
    val bg = if (eligible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val fg = if (eligible) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = if (eligible) "Eligible" else "Not eligible",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}