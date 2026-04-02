package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

        // Schedule Bill Reminders
        scheduleBillReminders()

        setContent {
            var displayName by remember { mutableStateOf("Shark") }
            var accountType by remember { mutableStateOf("INDIVIDUAL") }

            LaunchedEffect(firebaseUser.uid) {
                db.collection("users").document(firebaseUser.uid).get()
                    .addOnSuccessListener { doc ->
                        displayName = doc.getString("displayName") ?: "Shark"
                        accountType = doc.getString("accountType") ?: "INDIVIDUAL"
                    }
            }

            SharkFinDashboard(
                uid = firebaseUser.uid,
                displayName = displayName,
                accountType = accountType,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharkFinDashboard(
    uid: String,
    displayName: String,
    accountType: String,
    db: FirebaseFirestore,
    onUpdateProfile: (String) -> Unit,
    onLogout: () -> Unit
) {
    val tabs = DashTab.values()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // ── Unified Source of Truth (Single Dashboard State) ──
    var expenses      by remember { mutableStateOf(listOf<Expense>()) }
    var bills         by remember { mutableStateOf(listOf<Bill>()) }
    var goals         by remember { mutableStateOf(listOf<Goal>()) }
    var incomeSources by remember { mutableStateOf(listOf<IncomeSource>()) }
    var recurringBills by remember { mutableStateOf(listOf<RecurringBill>()) }

    var showAddExpense by remember { mutableStateOf(false) }
    var showAddIncome  by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var openFeature by remember { mutableStateOf<String?>(null) }
    var showOnboarding by remember { mutableStateOf(false) }

    // Unified Listeners: Everything updates from this single block
    LaunchedEffect(uid) {
        // Listen for Expenses
        db.collection("users").document(uid)
            .collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        // Listen for Bills
        db.collection("users").document(uid)
            .collection("bills")
            .addSnapshotListener { snapshot, _ ->
                bills = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Bill::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        // Listen for Goals
        db.collection("users").document(uid)
            .collection("goals")
            .addSnapshotListener { snapshot, _ ->
                goals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Goal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        // Listen for Income Sources
        db.collection("users").document(uid)
            .collection("incomeSources")
            .addSnapshotListener { snapshot, _ ->
                incomeSources = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(IncomeSource::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }

        // Listen for recurring bills from Firestore
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
                } ?: defaultRecurringBills
            }
    }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val hasOnboarded = doc.getBoolean("onboarded") ?: false
                if (!hasOnboarded) showOnboarding = false // Keep consistent with existing logic
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SharkDeepOcean, SharkBlack),
                    radius = 1200f
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(bottom = 100.dp)
        ) { page ->
            when (tabs[page]) {
                DashTab.HOME -> HomeScreen(
                    uid              = uid,
                    db               = db,
                    displayName      = displayName,
                    accountType      = accountType,
                    expenses         = expenses,
                    incomeSources    = incomeSources,
                    bills            = bills,
                    goals            = goals,
                    recurringBills   = if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills,
                    onExpenseClick   = { editingExpense = it },
                    onAddIncomeClick = { showAddIncome = true },
                    onFeatureClick   = { openFeature = it },
                    onAddExpense     = { showAddExpense = true }
                )
                DashTab.FEATURES -> FeaturesScreen(
                    onFeatureClick = { feature ->
                        when (feature) {
                            "Expense Tracker" -> {
                                scope.launch { pagerState.animateScrollToPage(DashTab.ACTIVITY.ordinal) }
                                showAddExpense = true
                            }
                            else -> openFeature = feature
                        }
                    }
                )
                DashTab.ACTIVITY -> ActivityScreen(
                    expenses = expenses,
                    bills = bills,
                    uid = uid,
                    db = db
                )
                DashTab.PROFILE -> ProfileScreen(
                    uid = uid,
                    userEmail = "",
                    userName = displayName,
                    onSignOut = onLogout
                )
            }
        }

        GlassBottomNav(
            tabs = tabs,
            currentPage = pagerState.currentPage,
            onTabClick = { index ->
                openFeature = null
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (pagerState.currentPage == DashTab.ACTIVITY.ordinal) {
            FloatingActionButton(
                onClick = { showAddExpense = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 110.dp),
                containerColor = SharkNavy,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }

        if (openFeature != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(colors = listOf(SharkDeepOcean, SharkBlack), radius = 1200f)
                    )
            ) {
                when (openFeature) {
                    "Bill Tracker"     -> BillTrackerScreen(uid, db, expenses, bills)
                    "Goal Tracker"     -> GoalTrackerScreen(uid, db, expenses, goals)
                    "Visual Models"    -> VisualModelsScreen(expenses, bills, goals)
                    "Import Statement" -> ImportStatementScreen(uid, db) { openFeature = null }
                    "Tax Tracker"      -> TaxTrackerScreen(expenses)
                    "Inflation Calc"   -> InflationCalcScreen()
                    "Stock/Forex"      -> StockForexScreen()
                    "Profile"          -> ProfileSettingsScreen(displayName, onUpdateProfile)
                    "Account Settings" -> AccountSettingsScreen()
                    else               -> ComingSoonPlaceholder(openFeature!!)
                }

                if (openFeature != "Import Statement") {
                    Box(
                        modifier = Modifier
                            .padding(top = 56.dp, start = 20.dp)
                            .size(44.dp)
                            .glassCard(22f, 0.15f)
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
fun GlassBottomNav(
    tabs: Array<DashTab>,
    currentPage: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).navigationBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 30f, alpha = 0.1f)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = currentPage == index
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabClick(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selected) {
                        Box(Modifier.width(18.dp).height(3.dp).background(SharkNavy, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.height(4.dp))
                    } else {
                        Spacer(Modifier.height(7.dp))
                    }
                    Icon(imageVector = tab.icon, contentDescription = null, tint = if (selected) SharkNavy else SharkMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        color = if (selected) SharkNavy else SharkMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun ComingSoonPlaceholder(feature: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.HourglassEmpty, null, tint = SharkMuted, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("$feature", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Module under development", color = SharkMuted, fontSize = 16.sp)
        }
    }
}
