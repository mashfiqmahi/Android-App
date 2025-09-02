package com.example.androidbloodbank.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onLogin: () -> Unit, onSignUp: () -> Unit, onEmergency: () -> Unit, onViewDashboard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Android Blood Bank", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Donate or request blood fast — verified donors, rare alerts, emergency mode", textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))

        Button(onClick = onViewDashboard, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Open Dashboard") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Login") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSignUp, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Sign up") }
        Spacer(Modifier.height(20.dp))
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
            onClick = onEmergency,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("EMERGENCY MODE (SOS)", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
