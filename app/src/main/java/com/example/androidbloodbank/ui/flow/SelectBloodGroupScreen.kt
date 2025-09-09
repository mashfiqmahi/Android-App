package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.model.BloodGroup
import com.example.androidbloodbank.ui.components.BloodGroupSelector

@Composable
fun SelectBloodGroupScreen(
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf(BloodGroup.O_POS) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Select Blood Group", style = MaterialTheme.typography.headlineSmall)

        BloodGroupSelector(
            value = selected,
            onChange = { selected = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onDone() }, // (Optional) pass 'selected' back via a lambda if needed
            modifier = Modifier.fillMaxWidth()
        ) { Text("Done") }

        TextButton(onClick = onBack) { Text("Back") }
    }
}
