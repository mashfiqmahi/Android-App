package com.example.androidbloodbank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.navigation.AppNavHost
import com.example.androidbloodbank.ui.theme.AndroidBloodBankTheme

class MainActivity : ComponentActivity() {

    // Hold LocalRepo at the Activity level (needs a Context)
    private lateinit var repo: LocalRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = LocalRepo(this)

        setContent {
            AndroidBloodBankTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    repo = repo,
                    onAlert = { /* no-op */ } // explicitly typed lambda; no inference issues
                )
            }
        }
    }
}
