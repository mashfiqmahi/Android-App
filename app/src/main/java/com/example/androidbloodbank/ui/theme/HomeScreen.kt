package com.example.androidbloodbank.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onDonate: () -> Unit,
    onFindDonors: () -> Unit,
    onBloodBank: () -> Unit,
    onRequestBlood: () -> Unit,
    onProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero banner (no cards)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(20.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text("Android Blood Bank", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Find donors fast. Request in seconds.", color = Color.White.copy(alpha = 0.9f))
            }
        }

        Spacer(Modifier.height(4.dp))

        // Minimal, button-first layout (no cards)
        Button(onClick = onRequestBlood, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Request Blood", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        FilledTonalButton(onClick = onFindDonors, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Find Donors")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDonate, modifier = Modifier.weight(1f).height(48.dp)) { Text("Donate") }
            OutlinedButton(onClick = onBloodBank, modifier = Modifier.weight(1f).height(48.dp)) { Text("Blood Bank") }
        }

        OutlinedButton(onClick = onProfile, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Profile")
        }
    }
}
