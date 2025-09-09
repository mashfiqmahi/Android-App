package com.example.androidbloodbank.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.model.BloodGroup

@Composable
fun BloodGroupChips(
    selected: BloodGroup?,
    onSelected: (BloodGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    // Order to match your mock
    val order = listOf(
        BloodGroup.A_POS, BloodGroup.A_NEG, BloodGroup.B_POS, BloodGroup.B_NEG,
        BloodGroup.O_POS, BloodGroup.O_NEG, BloodGroup.AB_POS, BloodGroup.AB_NEG
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Two tidy rows of 4 chips (no extra libs)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            order.take(4).forEach { bg ->
                FilterChip(
                    selected = selected == bg,
                    onClick = { onSelected(bg) },
                    label = { Text(bg.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            order.drop(4).forEach { bg ->
                FilterChip(
                    selected = selected == bg,
                    onClick = { onSelected(bg) },
                    label = { Text(bg.label) }
                )
            }
        }
    }
}
