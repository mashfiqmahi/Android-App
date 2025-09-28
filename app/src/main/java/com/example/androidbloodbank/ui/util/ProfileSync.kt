package com.example.androidbloodbank.ui.util

import android.net.Uri
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.UserProfile
import com.example.androidbloodbank.data.remote.FirebaseRepo
import com.google.gson.Gson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

// Realtime Database URL (your regioned DB)
private const val DB_URL =
    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"

/**
 * Saves to SharedPreferences (local) + Realtime Database (cloud).
 * Uploads profile photo to Firebase Storage IFF 'photoUri' is a content:// or file:// URI;
 * if 'photoUri' is an https:// URL, reuses it without uploading again.
 */
suspend fun saveUserProfile(
    repo: LocalRepo,
    name: String,
    bloodGroup: String?,
    contact: String?,
    location: String?,
    lastDonationMillis: Long?,
    totalDonations: Int?,
    email: String?,
    gender: String?,
    age: Int?,
    photoUri: String?,                       // can be content://, file://, or https:// (already uploaded)
    onDone: (Boolean, String) -> Unit = { _, _ -> }
) {
    // 1) Save a lean snapshot locally so your UI stays consistent even offline
    val profile = UserProfile(
        name = name.trim(),
        bloodGroup = (bloodGroup ?: "").trim(),
        lastDonationMillis = lastDonationMillis,
        totalDonations = totalDonations ?: 0,
        contactNumber = (contact ?: "").trim(),
        location = (location ?: "").trim()
    )
    runCatching { repo.saveCurrentUserJson(Gson().toJson(profile)) }

    // 2) Cloud sync with robust behavior
    val resultOk = withTimeoutOrNull(45_000) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return@withTimeoutOrNull false

        // 2a) Save core profile via your repo (private + public donor card)
        val repoRemote = FirebaseRepo()
        val coreOk = runCatching {
            repoRemote.saveProfile(profile)
            repoRemote.publishDonorCard(profile)
        }.isSuccess

        // 2b) Upload photo only when needed
        var photoUrlToSave: String? = null
        var photoUploadFailed = false
        if (!photoUri.isNullOrBlank()) {
            val u = runCatching { Uri.parse(photoUri) }.getOrNull()
            val scheme = u?.scheme?.lowercase(Locale.getDefault())

            when {
                // Already a remote download URL; reuse
                scheme == "http" || scheme == "https" -> {
                    photoUrlToSave = photoUri
                }
                // Upload content/file URIs to Storage
                scheme == "content" || scheme == "file" -> {
                    val storage = FirebaseStorage.getInstance() // default bucket from google-services.json
                    val ref = storage.reference
                        .child("profile_photos")
                        .child("$uid.jpg")
                    // If you want to keep EXIF or format, consider using the original extension
                    runCatching {
                        ref.putFile(u).await()
                        photoUrlToSave = ref.downloadUrl.await().toString()
                    }.onFailure { photoUploadFailed = true }
                }
                else -> {
                    // Unknown scheme; ignore and keep going
                }
            }
        }

        // 2c) Update private profile with extras (and photoUrl if available)
        val extras = mutableMapOf<String, Any?>(
            "email" to (email?.trim() ?: ""),
            "gender" to (gender?.trim() ?: ""),
            "age" to (age ?: 0),
            "lastDonationMillis" to (lastDonationMillis ?: 0L),
            "updatedAt" to ServerValue.TIMESTAMP
        )
        if (!photoUrlToSave.isNullOrBlank()) {
            extras["photoUrl"] = photoUrlToSave
        }

        val db = FirebaseDatabase.getInstance(DB_URL).reference
        val extrasOk = runCatching {
            db.child("users").child(uid).child("profile")
                .updateChildren(extras as Map<String, Any?>).await()
        }.isSuccess

        // 2d) Also store photoUrl in public donors node (optional)
        val publicPhotoOk = if (!photoUrlToSave.isNullOrBlank()) {
            runCatching {
                db.child("donors_public").child(uid)
                    .updateChildren(mapOf("photoUrl" to photoUrlToSave)).await()
            }.isSuccess
        } else true

        // Final result: succeed if DB writes succeeded (photo upload may fail independently)
        return@withTimeoutOrNull (coreOk && extrasOk && publicPhotoOk).also {
            // Report back with detailed message
            val msg = when {
                it && !photoUploadFailed -> "Profile saved."
                it && photoUploadFailed  -> "Profile saved (photo upload skipped/failed)."
                else                     -> "Saved locally. Network failed."
            }
            onDone(it, msg)
        }
    } ?: false

    if (!resultOk) onDone(false, "Saved locally. Network failed.")
}
