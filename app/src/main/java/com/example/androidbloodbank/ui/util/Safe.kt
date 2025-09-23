package com.example.androidbloodbank.ui.util

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Coerce any nullable String to a non-null one for Text(). */
fun nn(s: String?): String = s ?: ""

/** Drop-in Text that never crashes if the value is null. */
@Composable
fun SafeText(value: String?) {
    Text(value ?: "")
}
