package com.example.androidbloodbank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.model.BloodGroup

@Composable
fun BloodGroupSelector(
    value: BloodGroup,
    onChange: (BloodGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("Blood Group:", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BloodGroup.values().forEach { bg ->
                val selected = bg == value
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onChange(bg) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        bg.label,
                        color = if (selected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}
