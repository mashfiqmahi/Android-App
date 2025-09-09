package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { onDone() } // simple handoff; add delay/branding if you like
    Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Splash…") }
}
