package com.example.androidbloodbank.ui.flow

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.BloodBank
import com.example.androidbloodbank.ui.components.BloodBankCard
import com.google.firebase.database.*

private const val SG_DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

// All 64 districts of Bangladesh
private val BD_DISTRICTS = listOf(
    "Dhaka", "Faridpur", "Gazipur", "Gopalganj", "Jamalpur", "Kishoreganj", "Madaripur",
    "Manikganj", "Munshiganj", "Mymensingh", "Narayanganj", "Narsingdi", "Netrokona",
    "Rajbari", "Shariatpur", "Sherpur", "Tangail", "Bogra", "Joypurhat", "Naogaon",
    "Natore", "Nawabganj", "Pabna", "Rajshahi", "Sirajgonj", "Dinajpur", "Gaibandha",
    "Kurigram", "Lalmonirhat", "Nilphamari", "Panchagarh", "Rangpur", "Thakurgaon",
    "Barguna", "Barisal", "Bhola", "Jhalokati", "Patuakhali", "Pirojpur", "Bandarban",
    "Brahmanbaria", "Chandpur", "Chittagong", "Comilla", "Cox's Bazar", "Feni",
    "Khagrachari", "Lakshmipur", "Noakhali", "Rangamati", "Habiganj", "Maulvibazar",
    "Sunamganj", "Sylhet", "Bagerhat", "Chuadanga", "Jessore", "Jhenaidah", "Khulna",
    "Kushtia", "Magura", "Meherpur", "Narail", "Satkhira"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodBankScreen(
    onBack: () -> Unit = {}
) {
    var bloodBanks by remember { mutableStateOf<List<BloodBank>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var expandDistrict by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance(SG_DB_URL).reference

    LaunchedEffect(Unit) {
        val bloodBanksRef = database.child("bloodBanks")
        bloodBanksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedBloodBanks = mutableListOf<BloodBank>()

                for (data in snapshot.children) {
                    val bloodBank = BloodBank(
                        id = data.key ?: "",
                        name = data.child("name").getValue(String::class.java) ?: "",
                        district = data.child("district").getValue(String::class.java) ?: "",
                        type = data.child("type").getValue(String::class.java) ?: "",
                        location = data.child("location").getValue(String::class.java) ?: "",
                        phone = data.child("phone").getValue(String::class.java) ?: ""
                    )
                    fetchedBloodBanks.add(bloodBank)
                }

                bloodBanks = fetchedBloodBanks
                loading = false
                Log.d("BloodBankScreen", "Fetched ${fetchedBloodBanks.size} blood banks")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BloodBankScreen", "Error fetching data: ${error.message}")
                loading = false
            }
        })
    }

    // Filter blood banks based on search query and district
    val filteredBloodBanks = remember(bloodBanks, searchQuery, selectedDistrict) {
        bloodBanks.filter { bank ->
            val matchesSearch = searchQuery.isBlank() ||
                    bank.name.contains(searchQuery, ignoreCase = true) ||
                    bank.location.contains(searchQuery, ignoreCase = true) ||
                    bank.type.contains(searchQuery, ignoreCase = true)

            val matchesDistrict = selectedDistrict == null ||
                    bank.district.equals(selectedDistrict, ignoreCase = true)

            matchesSearch && matchesDistrict
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Banks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, location, or type") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true
            )

            // District filter
            Text("Filter by district", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = expandDistrict,
                onExpandedChange = { expandDistrict = !expandDistrict }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = selectedDistrict ?: "All districts",
                    onValueChange = {},
                    readOnly = true,
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
                            selectedDistrict = null
                            expandDistrict = false
                        }
                    )
                    BD_DISTRICTS.forEach { district ->
                        DropdownMenuItem(
                            text = { Text(district) },
                            onClick = {
                                selectedDistrict = district
                                expandDistrict = false
                            }
                        )
                    }
                }
            }

            // Results
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredBloodBanks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No blood banks found")
                    }
                }
                else -> {
                    Text(
                        "${filteredBloodBanks.size} blood bank(s) found",
                        style = MaterialTheme.typography.labelLarge
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredBloodBanks, key = { it.id }) { bloodBank ->
                            BloodBankCard(bloodBank)
                        }
                    }
                }
            }
        }
    }
}