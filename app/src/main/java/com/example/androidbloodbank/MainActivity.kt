package com.example.androidbloodbank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val repo = remember { LocalRepo(this) }
                AppNavHost(navController = navController, repo = repo,  onAlert = { msg ->
                    // later you can connect this to SnackbarHostState
                    println("ALERT: $msg")
                })

            }
        }
    }
}



//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