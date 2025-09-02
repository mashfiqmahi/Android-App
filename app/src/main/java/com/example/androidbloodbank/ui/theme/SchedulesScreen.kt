package com.example.androidbloodbank.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.Schedule
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SchedulesScreen(repo: LocalRepo, onBack: () -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val schedules = remember { repo.loadSchedules().toMutableStateList() }
    val donors = remember { repo.loadDonors() }
    var donorQuery by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(sdf.format(Date())) }
    var notes by remember { mutableStateOf("") }
    var selectedDonorId by remember { mutableStateOf<String?>(donors.firstOrNull()?.id) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Donation Schedules", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Text("Select donor (search by name):")
        OutlinedTextField(value = donorQuery, onValueChange = { donorQuery = it }, label = { Text("Search name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Column {
            donors.filter { donorQuery.isBlank() || it.name.contains(donorQuery, ignoreCase = true) }.forEach { d ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedDonorId = d.id }.padding(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(selected = d.id == selectedDonorId, onClick = { selectedDonorId = d.id })
                    Text("${d.name} (${d.bloodGroup.label})", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            selectedDonorId?.let { did ->
                val s = Schedule(donorId = did, dateIso = dateText, notes = notes)
                schedules.add(s)
                repo.saveSchedules(schedules)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Schedule Donation") }

        Spacer(Modifier.height(16.dp))
        Text("Upcoming schedules:", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (schedules.isEmpty()) Text("No scheduled donations.")
        schedules.forEach { s ->
            val donor = donors.find { it.id == s.donorId }
            Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(donor?.name ?: "Unknown")
                    Text("Date: ${s.dateIso}")
                    if (!s.notes.isNullOrBlank()) Text("Notes: ${s.notes}")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
