package com.example.androidbloodbank.ui.util

import android.content.Context
import android.content.Intent
import com.example.androidbloodbank.data.model.BloodRequest
import java.text.SimpleDateFormat
import java.util.*

fun shareBloodRequest(context: Context, req: BloodRequest) {
    val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val whenText = if (req.neededOnMillis > 0) fmt.format(Date(req.neededOnMillis)) else "ASAP"

    val text = buildString {
        append("🚨 URGENT BLOOD NEEDED: ${req.bloodGroup}\n")
        if (req.hospitalName.isNotBlank()) append("🏥 Hospital: ${req.hospitalName}\n")
        if (req.locationName.isNotBlank()) append("📍 Location: ${req.locationName}\n")
        append("📅 Needed: $whenText\n")
        if (req.phone.isNotBlank()) append("📞 Contact: ${req.phone}\n")
        if (req.requesterName.isNotBlank()) append("👤 Requester: ${req.requesterName}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share blood request"))
}
