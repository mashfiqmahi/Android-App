package com.example.androidbloodbank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.navigation.AppNavHost
import com.example.androidbloodbank.ui.theme.AndroidBloodBankTheme // <-- import this

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidBloodBankTheme {
                val navController = rememberNavController()
                val repo = remember { LocalRepo(this) }
                AppNavHost(navController, repo) { msg -> println("ALERT: $msg") }
            }
        }
    }
}
