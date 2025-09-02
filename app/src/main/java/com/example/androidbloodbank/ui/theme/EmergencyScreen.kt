package com.example.androidbloodbank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.Donor

@Composable
fun EmergencyScreen(repo: LocalRepo, onBack: () -> Unit) {
    val donors = remember { repo.loadDonors() }
    val priority = donors.sortedWith(compareByDescending<Donor> { it.verified }.thenBy { it.bloodGroup.label })

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFB00020)), contentAlignment = Alignment.Center) {
            Text("EMERGENCY MODE", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        Text("We will show nearby verified donors and priority rare blood types (simulated).")
        Spacer(Modifier.height(12.dp))
        Text("Priority list:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            priority.forEach { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text("Blood: ${d.bloodGroup.label}")
                        }
                        if (d.verified) Text("Verified", color = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { /* production: call/message donor */ }) { Text("Call (sim)") }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Exit Emergency") }
    }
}
