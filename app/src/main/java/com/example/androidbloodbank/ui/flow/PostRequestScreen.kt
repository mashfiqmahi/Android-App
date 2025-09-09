package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.BloodRequest
import com.example.androidbloodbank.data.model.RequestStatus

@Composable
fun PostRequestScreen(
    repo: LocalRepo,
    onPosted: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var bg by remember { mutableStateOf("O+") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Post Request", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(hospital, { hospital = it }, label = { Text("Hospital") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(contact, { contact = it }, label = { Text("Contact") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bg, { bg = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val list = repo.loadRequests().apply {
                    add(
                        com.example.androidbloodbank.data.model.BloodRequest(
                            requesterName = name,
                            hospital = hospital,
                            location = location,
                            contactNumber = contact,
                            bloodGroup = bg,
                            status = RequestStatus.PENDING   // <-- NEW
                        )
                    )
                }
                repo.saveRequests(list)
                onPosted()

            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Submit") }

        TextButton(onClick = onBack) { Text("Back") }
    }
}
