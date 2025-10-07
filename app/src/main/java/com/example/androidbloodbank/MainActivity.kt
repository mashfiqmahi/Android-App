package com.example.androidbloodbank

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.navigation.compose.rememberNavController
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.navigation.AppNavHost
import com.example.androidbloodbank.ui.theme.AndroidBloodBankTheme


//import com.google.firebase.database.FirebaseDatabase
//private const val SG_DB_URL =
//    "https://blood-bank-e6626-default-rtdb.asia-southeast1.firebasedatabase.app"
//// Define the BloodBank data class
//data class BloodBank(
//    val name: String,
//    val district: String,
//    val type: String,
//    val location: String,
//    val phone: String
//)
//
//fun addBloodBanks() {
//    // Get reference to Firebase Realtime Database
//    val database = FirebaseDatabase.getInstance(SG_DB_URL).reference
//    Log.d("Firebase", "Firebase Database Initialized: $database")
//    // List of blood bank data to add to Firebase
//    val bloodBanks = listOf(
//        BloodBank(
//            "Red Crescent Blood Center",
//            "Dhaka",
//            "NGO Blood Bank",
//            "7/5, Aurongzeb Road, Mohammadpur",
//            "+8801811458537 (Hotline)"
//        ),
//        BloodBank(
//            "Holy Family Red Crescent Blood Center",
//            "Dhaka",
//            "NGO Blood Bank",
//            "1 Eskaton Garden Road",
//            "+8801811458536"
//        ),
//        BloodBank(
//            "Quantum Blood Bank",
//            "Dhaka",
//            "NGO Blood Bank",
//            "31/V Shilpacharya Zainul Abedin Sarak, Shantinagar",
//            "+8801714010869"
//        ),
//        BloodBank(
//            "BADHAN Central Office (Donor Coordination)",
//            "Dhaka",
//            "Voluntary Donor Org.",
//            "T.S.C, University of Dhaka",
//            "+8801534982674"
//        ),
//        BloodBank(
//            "Fatema Begum Red Crescent Blood Center",
//            "Chattogram",
//            "NGO Blood Bank",
//            "395 Anderkilla",
//            "+8801815850533, +88031620926"
//        ),
//        BloodBank(
//            "Chattogram Medical College Hospital (Blood Bank)",
//            "Chattogram",
//            "Govt. Hospital",
//            "Chattogram Medical College",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Rajshahi Red Crescent Blood Center",
//            "Rajshahi",
//            "NGO Blood Bank",
//            "Rajshahi District Road",
//            "+8801865055075"
//        ),
//        BloodBank(
//            "Rajshahi Medical College Hospital (Blood Bank)",
//            "Rajshahi",
//            "Govt. Hospital",
//            "Rajshahi Medical College",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Mujib Jahan Red Crescent Blood Center",
//            "Sylhet",
//            "NGO Blood Bank",
//            "Chowhatta",
//            "+8801611300900, +880821724098"
//        ),
//        BloodBank(
//            "Sylhet MAG Osmani Medical College Hospital (Blood Bank)",
//            "Sylhet",
//            "Govt. Hospital",
//            "Sylhet Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Khulna Medical College Hospital (Blood Bank)",
//            "Khulna",
//            "Govt. Hospital",
//            "Khulna Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Barishal Sher-e-Bangla Medical College Hospital (Blood Bank)",
//            "Barishal",
//            "Govt. Hospital",
//            "Barishal Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Rangpur Medical College Hospital (Blood Bank)",
//            "Rangpur",
//            "Govt. Hospital",
//            "Rangpur Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Begum Tayeeba Mojumder Red Crescent Blood Center",
//            "Dinajpur",
//            "NGO Blood Bank",
//            "1 New Town",
//            "+8801765311450"
//        ),
//        BloodBank(
//            "Ahad Red Crescent Blood Center",
//            "Jashore",
//            "NGO Blood Bank",
//            "Munshi Mehabullah Road",
//            "+8801939109722"
//        ),
//        BloodBank(
//            "Natore Red Crescent Blood Center",
//            "Natore",
//            "NGO Blood Bank",
//            "Hospital Road",
//            "+8801850124225"
//        ),
//        BloodBank(
//            "Achia Khatun Memorial Red Crescent Blood Center",
//            "Magura",
//            "NGO Blood Bank",
//            "Jhenidah Road, Stadium Para",
//            "+8801913137366"
//        ),
//        BloodBank(
//            "Mymensingh Medical College Hospital (Blood Bank)",
//            "Mymensingh",
//            "Govt. Hospital",
//            "Mymensingh Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Comilla Medical College Hospital (Blood Bank)",
//            "Cumilla",
//            "Govt. Hospital",
//            "Cumilla Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Bogra Shaheed Ziaur Rahman Medical College Hospital (Blood Bank)",
//            "Bogura",
//            "Govt. Hospital",
//            "Bogura Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Kushtia Sadar Hospital",
//            "Kushtia",
//            "Govt. Hospital",
//            "Kushtia Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Narayanganj Sadar Hospital",
//            "Narayanganj",
//            "Govt. Hospital",
//            "Narayanganj Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Gazipur Sadar Hospital",
//            "Gazipur",
//            "Govt. Hospital",
//            "Gazipur Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Cox's Bazar Sadar Hospital",
//            "Cox's Bazar",
//            "Govt. Hospital",
//            "Cox's Bazar Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Noakhali Sadar Hospital",
//            "Noakhali",
//            "Govt. Hospital",
//            "Noakhali Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Pabna Sadar Hospital",
//            "Pabna",
//            "Govt. Hospital",
//            "Pabna Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Dinajpur Sadar Hospital",
//            "Dinajpur",
//            "Govt. Hospital",
//            "Dinajpur Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Blood Transfusion Department, Jessore Sadar Hospital",
//            "Jashore",
//            "Govt. Hospital",
//            "Jashore Sadar",
//            "N/A (Suggest calling Sadar Hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Police Blood Bank",
//            "Dhaka",
//            "Govt. Blood Bank",
//            "Central Police Hospital, Rajarbag",
//            "+8801713398386"
//        ),
//        BloodBank(
//            "Thalassemia Blood Bank",
//            "Dhaka",
//            "Specialized NGO",
//            "30 Chamelibag, 1st Lane, Shantinagar",
//            "+88028332481"
//        ),
//        BloodBank(
//            "Islami Bank Hospital Blood Bank",
//            "Dhaka",
//            "Private Hospital",
//            "30, VIP Road, Kakrail",
//            "+88028317090"
//        ),
//        BloodBank(
//            "Sandhani Central (Donor Coordination)",
//            "Dhaka",
//            "Voluntary Donor Org.",
//            "BSMMU, Shahabag",
//            "+88028621658"
//        ),
//        BloodBank(
//            "Monno Medical College Hospital (Blood Bank)",
//            "Manikganj",
//            "Private Hospital",
//            "Manikganj Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "International Medical College Hospital (Blood Bank)",
//            "Gazipur",
//            "Private Hospital",
//            "Gazipur Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        ),
//        BloodBank(
//            "Eastern Medical College Hospital (Blood Bank)",
//            "Cumilla",
//            "Private Hospital",
//            "Cumilla Sadar",
//            "N/A (Suggest calling hospital PABX/Emergency)"
//        )
//    )
//
//    // Loop through the list and add each blood bank to the Firebase database
//    for (bloodBank in bloodBanks) {
//        val bloodBankRef = database.child("bloodBanks").push()  // Automatically generates unique ID
//        bloodBankRef.setValue(bloodBank)
//            .addOnSuccessListener {
//                Log.d("Firebase", "Blood bank added successfully!")
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firebase", "Error adding blood bank: ${exception.message}")
//            }
//    }
//
//    println("Blood banks added successfully!")
//}

class MainActivity : ComponentActivity() {

    // Hold LocalRepo at the Activity level (needs a Context)
    private lateinit var repo: LocalRepo

    @RequiresApi(Build.VERSION_CODES.O)
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
