package com.example.androidbloodbank

import androidx.compose.foundation.layout.Arrangement
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/*
  Android Blood Bank - Enhanced Prototype
  - local persistence via SharedPreferences + Gson
  - in-memory repo backed by prefs
  - simple matching/search and simulated emergency mode
  - schedule donation entries persisted
*/

/* ---------- Models ---------- */
data class User(val id: String = UUID.randomUUID().toString(), val name: String, val email: String, val bloodGroup: BloodGroup)
data class Donor(val id: String = UUID.randomUUID().toString(), val name: String, val bloodGroup: BloodGroup, val phone: String? = null, val verified: Boolean = false)
data class Schedule(val id: String = UUID.randomUUID().toString(), val donorId: String, val dateIso: String, val notes: String? = null)

enum class BloodGroup(val label: String) {
    A_POS("A+"), A_NEG("A-"),
    B_POS("B+"), B_NEG("B-"),
    AB_POS("AB+"), AB_NEG("AB-"),
    O_POS("O+"), O_NEG("O-");

    override fun toString(): String = label
}

/* ---------- Simple persistent repo using SharedPreferences + Gson ---------- */
class LocalRepo(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("abb_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DONORS = "donors_json"
        private const val KEY_USERS = "users_json"
        private const val KEY_SCHEDULES = "schedules_json"
    }

    private fun <T> fromJson(json: String?, typeToken: TypeToken<T>): T? {
        if (json.isNullOrBlank()) return null
        return gson.fromJson(json, typeToken.type)
    }

    private fun toJson(value: Any): String = gson.toJson(value)

    // donors
    fun loadDonors(): MutableList<Donor> {
        val json = prefs.getString(KEY_DONORS, null)
        return fromJson(json, object : TypeToken<MutableList<Donor>>() {}) ?: defaultDonors().toMutableList()
    }

    fun saveDonors(list: List<Donor>) { prefs.edit().putString(KEY_DONORS, toJson(list)).apply() }

    // users (for simple signup/login)
    fun loadUsers(): MutableList<User> {
        val json = prefs.getString(KEY_USERS, null)
        return fromJson(json, object : TypeToken<MutableList<User>>() {}) ?: mutableListOf()
    }

    fun saveUsers(list: List<User>) { prefs.edit().putString(KEY_USERS, toJson(list)).apply() }

    // schedules
    fun loadSchedules(): MutableList<Schedule> {
        val json = prefs.getString(KEY_SCHEDULES, null)
        return fromJson(json, object : TypeToken<MutableList<Schedule>>() {}) ?: mutableListOf()
    }

    fun saveSchedules(list: List<Schedule>) { prefs.edit().putString(KEY_SCHEDULES, toJson(list)).apply() }

    // default donors seeded for demo (useful offline)
    private fun defaultDonors() = listOf(
        Donor(name = "Rahim Uddin", bloodGroup = BloodGroup.O_NEG, phone = "01710000001", verified = true),
        Donor(name = "Ayesha Khan", bloodGroup = BloodGroup.A_POS, phone = "01710000002", verified = true),
        Donor(name = "Jahangir", bloodGroup = BloodGroup.B_NEG, phone = "01710000003"),
        Donor(name = "Mina Sultana", bloodGroup = BloodGroup.AB_NEG, phone = "01710000004"),
        Donor(name = "Sohan", bloodGroup = BloodGroup.O_POS, phone = "01710000005", verified = true),
    )
}

/* ---------- Navigation ---------- */
object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val REQUEST = "request"
    const val SCHEDULES = "schedules"
    const val EMERGENCY = "emergency"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val repo = remember { LocalRepo(this) }
                AppNavHost(navController = navController, repo = repo)
            }
        }
    }
}

/* ---------- App ---------- */
@Composable
fun AppNavHost(navController: NavHostController, repo: LocalRepo) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        NavHost(navController = navController, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onLogin = { navController.navigate(Routes.LOGIN) },
                    onSignUp = { navController.navigate(Routes.SIGNUP) },
                    onEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onViewDashboard = { navController.navigate(Routes.DASHBOARD) }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = { navController.navigate(Routes.DASHBOARD) }
                )
            }

            composable(Routes.SIGNUP) {
                SignupScreen(repo = repo, onBack = { navController.popBackStack() }, onSignupSuccess = { navController.navigate(Routes.DASHBOARD) })
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(repo = repo, onRequest = { navController.navigate(Routes.REQUEST) }, onSchedules = { navController.navigate(Routes.SCHEDULES) }, onEmergency = { navController.navigate(Routes.EMERGENCY) })
            }

            composable(Routes.REQUEST) {
                // State for snackbar message
                var snackbarMessage by remember { mutableStateOf<String?>(null) }

                // Listen for snackbarMessage changes
                snackbarMessage?.let { msg ->
                    LaunchedEffect(msg) {
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                        snackbarMessage = null // reset after showing
                    }
                }

                RequestBloodScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onAlert = { msg ->
                        snackbarMessage = msg // just set state here
                    }
                )
            }


            composable(Routes.SCHEDULES) {
                SchedulesScreen(repo = repo, onBack = { navController.popBackStack() })
            }

            composable(Routes.EMERGENCY) {
                EmergencyScreen(repo = repo, onBack = { navController.popBackStack() })
            }
        }
    }
}

/* ---------- Screens ---------- */

@Composable
fun HomeScreen(onLogin: () -> Unit, onSignUp: () -> Unit, onEmergency: () -> Unit, onViewDashboard: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Android Blood Bank", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Donate or request blood fast — verified donors, rare alerts, emergency mode", textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))

        Button(onClick = onViewDashboard, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Open Dashboard") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Login") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSignUp, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Sign up") }
        Spacer(Modifier.height(20.dp))
        Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)), onClick = onEmergency, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("EMERGENCY MODE (SOS)", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("Impact: real-time donor matching; rare blood alerts; offline access.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LoginScreen(repo: LocalRepo, onBack: () -> Unit, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var nameDisplay by remember { mutableStateOf("") }
    val users = remember { repo.loadUsers() }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Login (demo)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (enter exactly as signed up)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val found = users.find { it.email.equals(email.trim(), ignoreCase = true) }
            if (found != null) {
                nameDisplay = found.name
                error = null
                // In a more advanced app you'd handle tokens / secure auth
                onLoginSuccess()
            } else {
                error = "No user found for that email. Try signing up."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Login (Demo)") }

        Spacer(Modifier.height(12.dp))
        if (!error.isNullOrBlank()) Text(error ?: "", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
        if (nameDisplay.isNotBlank()) Text("Welcome, $nameDisplay", modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun SignupScreen(repo: LocalRepo, onBack: () -> Unit, onSignupSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedBG by remember { mutableStateOf(BloodGroup.O_POS) }
    var verified by remember { mutableStateOf(false) } // in real app, verification via ID checks
    val ctx = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sign up as Donor (demo)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        // blood group simple chooser
        Text("Blood Group:")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BloodGroup.values().forEach { bg ->
                val selected = bg == selectedBG
                Box(modifier = Modifier
                    .padding(4.dp)
                    .clickable { selectedBG = bg }
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                ) {
                    Text(bg.label, color = if (selected) Color.White else Color.Black)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = verified, onCheckedChange = { verified = it })
            Text("Mark as Verified (demo)")
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val users = repo.loadUsers()
            if (email.isBlank() || name.isBlank()) return@Button
            users.add(User(name = name.trim(), email = email.trim(), bloodGroup = selectedBG))
            repo.saveUsers(users)
            // also register as a donor by default for visibility
            val donors = repo.loadDonors()
            donors.add(Donor(name = name.trim(), bloodGroup = selectedBG, phone = phone.ifBlank { null }, verified = verified))
            repo.saveDonors(donors)
            onSignupSuccess()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Create Account & Register as Donor")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun DashboardScreen(repo: LocalRepo, onRequest: () -> Unit, onSchedules: () -> Unit, onEmergency: () -> Unit) {
    val donors = remember { mutableStateListOf<Donor>().apply { addAll(repo.loadDonors()) } }
    // Live update: if repo changed elsewhere, reload from storage on recomposition trigger - kept simple here

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Quick Actions", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Request Blood / Find Donors") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSchedules, modifier = Modifier.fillMaxWidth()) { Text("View Donation Schedules") }
        Spacer(Modifier.height(8.dp))
        Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)), onClick = onEmergency, modifier = Modifier.fillMaxWidth()) {
            Text("Emergency Mode (SOS)", color = Color.White)
        }
        Spacer(Modifier.height(16.dp))

        Text("Nearby Donors (seeded/offline):", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        // list donors
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            donors.forEach { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text("Blood: ${d.bloodGroup.label}")
                            if (d.phone != null) Text("Phone: ${d.phone}")
                        }
                        if (d.verified) {
                            Text("Verified", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestBloodScreen(repo: LocalRepo, onBack: () -> Unit, onAlert: (String) -> Unit) {
    var selectedBG by remember { mutableStateOf(BloodGroup.O_POS) }
    val donors = remember { repo.loadDonors() }
    var matched by remember { mutableStateOf(listOf<Donor>()) }
    val rareTypes = setOf(BloodGroup.A_NEG, BloodGroup.B_NEG, BloodGroup.AB_NEG, BloodGroup.O_NEG)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Find Donors", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Select desired blood group:")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BloodGroup.values().forEach { bg ->
                val sel = bg == selectedBG
                Box(modifier = Modifier
                    .clickable { selectedBG = bg }
                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                ) {
                    Text(bg.label, color = if (sel) Color.White else Color.Black)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            matched = donors.filter { it.bloodGroup == selectedBG }
            if (matched.isEmpty()) {
                onAlert("No local donors found for $selectedBG. Consider creating an alert.")
            }
            // Rare blood alert check
            if (selectedBG in rareTypes) {
                onAlert("⚠️ $selectedBG is a rare type. Alerting verified donors (simulated).")
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Search") }

        Spacer(Modifier.height(12.dp))
        if (matched.isEmpty()) {
            Text("No matches yet.")
        } else {
            Text("Matches (${matched.size}):", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            matched.forEach { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text("Blood: ${d.bloodGroup.label}")
                            if (!d.phone.isNullOrBlank()) Text("Phone: ${d.phone}")
                        }
                        Column {
                            if (d.verified) Text("Verified", color = Color(0xFF2E7D32))
                            Spacer(Modifier.height(6.dp))
                            Button(onClick = {
                                // In a real app -> direct call / message / directions
                                onAlert("Contacting ${d.name} (simulated)...")
                            }) { Text("Contact") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Tip: In a production app, connect to location & push-notifications to reach donors in real-time.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun SchedulesScreen(repo: LocalRepo, onBack: () -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val schedules = remember { repo.loadSchedules().toMutableStateList() }
    val donors = remember { repo.loadDonors() }
    var donorQuery by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(sdf.format(Date())) }
    var notes by remember { mutableStateOf("") }
    var selectedDonorId by remember { mutableStateOf<String?>(donors.firstOrNull()?.id) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Donation Schedules", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Text("Select donor (search by name):")
        OutlinedTextField(value = donorQuery, onValueChange = { donorQuery = it }, label = { Text("Search name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Column {
            donors.filter { donorQuery.isBlank() || it.name.contains(donorQuery, ignoreCase = true) }.forEach { d ->
                Row(modifier = Modifier.fillMaxWidth().clickable { selectedDonorId = d.id }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = d.id == selectedDonorId, onClick = { selectedDonorId = d.id })
                    Text("${d.name} (${d.bloodGroup.label})", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            selectedDonorId?.let { did ->
                val s = Schedule(donorId = did, dateIso = dateText, notes = notes)
                schedules.add(s)
                repo.saveSchedules(schedules)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Schedule Donation") }

        Spacer(Modifier.height(16.dp))
        Text("Upcoming schedules:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (schedules.isEmpty()) Text("No scheduled donations.")
        schedules.forEach { s ->
            val donor = donors.find { it.id == s.donorId }
            Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(donor?.name ?: "Unknown")
                    Text("Date: ${s.dateIso}")
                    if (!s.notes.isNullOrBlank()) Text("Notes: ${s.notes}")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun EmergencyScreen(repo: LocalRepo, onBack: () -> Unit) {
    // Emergency UI: big attention-grabbing layout; lists matched donors (simulated nearby)
    val donors = remember { repo.loadDonors() }
    // For demo, we pick rare types and verified donors first
    val priority = donors.sortedWith(compareByDescending<Donor> { it.verified }.thenBy { it.bloodGroup.label })
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFB00020)), contentAlignment = Alignment.Center) {
            Text("EMERGENCY MODE", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        Text("We will show nearby verified donors and priority rare blood types (simulated).", textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Priority list:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            priority.forEach { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text("Blood: ${d.bloodGroup.label}")
                        }
                        if (d.verified) Text("Verified", color = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { /* In production: call / message donor immediately */ }) {
                            Text("Call (sim)")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Exit Emergency") }
    }
}
