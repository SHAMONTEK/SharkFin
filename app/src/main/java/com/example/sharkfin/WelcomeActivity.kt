package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.work.*
import com.example.sharkfin.ui.theme.SharkFinTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import java.util.concurrent.TimeUnit

enum class DashTab(val label: String, val icon: ImageVector) {
    HOME("Home",         Icons.Default.Home),
    FEATURES("Features", Icons.Default.Apps),
    ACTIVITY("Activity", Icons.Default.ReceiptLong),
    PROFILE("Profile",   Icons.Default.Person)
}

class WelcomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            finish()
            return
        }

        scheduleBillReminders()

        setContent {
            SharkFinTheme {
                var displayName by remember { mutableStateOf("Shark") }
                var accountType by remember { mutableStateOf("INDIVIDUAL") }
                var discoveryData by remember { mutableStateOf<Map<String, Any>?>(null) }

                LaunchedEffect(firebaseUser.uid) {
                    db.collection("users").document(firebaseUser.uid).get()
                        .addOnSuccessListener { doc ->
                            displayName = doc.getString("displayName") ?: "Shark"
                            accountType = doc.getString("accountType") ?: "INDIVIDUAL"
                            discoveryData = doc.data?.filterKeys { 
                                it in listOf("monthlyIncome", "monthlyObligations", "targetSavings", "totalDebt", "discoveryCompleted")
                            }
                        }
                }

                SharkFinDashboard(
                    uid = firebaseUser.uid,
                    displayName = displayName,
                    accountType = accountType,
                    discoveryData = discoveryData,
                    db = db,
                    onUpdateProfile = { newName -> displayName = newName },
                    onLogout = {
                        auth.signOut()
                        val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    private fun scheduleBillReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val reminderRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BillReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}

@Composable
fun SharkFinDashboard(
    uid: String,
    displayName: String,
    accountType: String,
    discoveryData: Map<String, Any>?,
    db: FirebaseFirestore,
    onUpdateProfile: (String) -> Unit,
    onLogout: () -> Unit
) {
    var expenses      by remember { mutableStateOf(listOf<Expense>()) }
    var bills         by remember { mutableStateOf(listOf<Bill>()) }
    var goals         by remember { mutableStateOf(listOf<Goal>()) }
    var debts         by remember { mutableStateOf(listOf<Debt>()) }
    var incomeSources by remember { mutableStateOf(listOf<IncomeSource>()) }
    var recurringBills by remember { mutableStateOf(listOf<RecurringBill>()) }
    var portfolio     by remember { mutableStateOf(listOf<PortfolioAsset>()) }

    var showAddExpense by remember { mutableStateOf(false) }
    var showAddIncome  by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var openFeature by remember { mutableStateOf<String?>(null) }
    var showOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        db.collection("users").document(uid)
            .collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        db.collection("users").document(uid)
            .collection("bills")
            .addSnapshotListener { snapshot, _ ->
                bills = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Bill::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        db.collection("users").document(uid)
            .collection("goals")
            .addSnapshotListener { snapshot, _ ->
                goals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Goal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        db.collection("users").document(uid)
            .collection("debts")
            .addSnapshotListener { snapshot, _ ->
                debts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Debt::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        db.collection("users").document(uid)
            .collection("incomeSources")
            .addSnapshotListener { snapshot, _ ->
                incomeSources = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(IncomeSource::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        db.collection("users").document(uid)
            .collection("recurringBills")
            .addSnapshotListener { snapshot, _ ->
                recurringBills = snapshot?.documents?.mapNotNull { doc ->
                    RecurringBill(
                        key      = doc.getString("key")      ?: "",
                        label    = doc.getString("label")    ?: "",
                        amount   = doc.getDouble("amount")   ?: 0.0,
                        category = doc.getString("category") ?: "Other"
                    )
                } ?: emptyList()
            }

        db.collection("users").document(uid)
            .collection("portfolio")
            .addSnapshotListener { snapshot, _ ->
                portfolio = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PortfolioAsset::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val hasOnboarded = doc.getBoolean("onboarded") ?: false
                showOnboarding = !hasOnboarded
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeScreen(
            uid              = uid,
            db               = db,
            displayName      = displayName,
            accountType      = accountType,
            expenses         = expenses,
            incomeSources    = incomeSources,
            bills            = bills,
            goals            = goals,
            debts            = debts,
            recurringBills   = if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills,
            discoveryData    = discoveryData,
            onFeatureClick   = { feature ->
                when (feature) {
                    "Expense Tracker" -> {
                        openFeature = "Activity"
                        showAddExpense = true
                    }
                    "Account Settings" -> {
                        openFeature = "Profile"
                    }
                    else -> openFeature = feature
                }
            }
        )

        if (openFeature == "Activity") {
            FloatingActionButton(
                onClick = { showAddExpense = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 110.dp),
                containerColor = SharkGold,
                contentColor = SharkBg,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }

        if (openFeature != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (openFeature) {
                    "Activity"         -> ActivityScreen(expenses, bills, uid, db)
                    "Features"         -> FixedExpensesSettingsScreen(uid, db)
                    "Profile"          -> ProfileScreen(uid, "", displayName, onLogout)
                    "Bill Tracker"     -> BillTrackerScreen(uid, db, expenses, bills)
                    "Goal Tracker"     -> GoalTrackerScreen(uid, db, expenses, goals)
                    "Visual Models"    -> VisualModelsScreen(expenses, bills, goals, discoveryData)
                    "Import Statement" -> ImportStatementScreen(uid, db) { openFeature = null }
                    "Tax Tracker"      -> TaxTrackerScreen(expenses)
                    "Dividend Tracker" -> DividendTrackerScreen(uid, db, portfolio)
                    "Inflation Calc"   -> InflationCalcScreen()
                    "AI Prediction"    -> AiPredictionScreen(expenses, incomeSources, bills, discoveryData)
                    "Stock/Forex"      -> StockForexScreen(uid, db, portfolio)
                    "Passive Snowball" -> PassiveSnowballScreen(uid, db, expenses, bills)
                    "Debt Vanish"      -> DebtVanishScreen(uid, db, debts)
                    "Freedom Runway"   -> FreedomRunwayScreen(expenses, bills, discoveryData)
                    "Account Settings" -> AccountSettingsScreen()
                    else               -> ComingSoonPlaceholder(openFeature!!)
                }

                if (openFeature != "Import Statement") {
                    Box(
                        modifier = Modifier
                            .padding(top = 56.dp, start = 20.dp)
                            .size(44.dp)
                            .background(SharkSurface.copy(alpha = 0.5f), CircleShape)
                            .clickable { openFeature = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                }
            }
        }

        if (showOnboarding) {
            OnboardingScreen(
                uid      = uid,
                db       = db,
                onFinish = { showOnboarding = false }
            )
        }
    }

    if (showAddExpense) {
        AddExpenseSheet(uid, db) { showAddExpense = false }
    }

    if (showAddIncome) {
        AddIncomeSourceSheet(uid, db) { showAddIncome = false }
    }

    if (editingExpense != null) {
        EditExpenseSheet(editingExpense!!, uid, db) { editingExpense = null }
    }
}


@Composable
fun ComingSoonPlaceholder(feature: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.HourglassEmpty, null, tint = SharkSecondary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("$feature", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Module under development", color = SharkSecondary, fontSize = 16.sp)
        }
    }
}
