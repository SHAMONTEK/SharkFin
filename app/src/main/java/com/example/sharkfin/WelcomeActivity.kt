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
    }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val hasOnboarded = doc.getBoolean("onboarded") ?: false
                if (!hasOnboarded) showOnboarding = true
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0a1a14), SharkBlack),
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
                    uid = uid,
                    db = db,
                    displayName = displayName,
                    accountType = accountType,
                    expenses = expenses,
                    incomeSources = incomeSources,
                    onExpenseClick = { editingExpense = it },
                    onAddIncomeClick = { showAddIncome = true }
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
                    onAddClick = { showAddExpense = true },
                    onExpenseClick = { editingExpense = it }
                )
                DashTab.PROFILE -> ProfileScreen(
                    displayName = displayName,
                    accountType = accountType,
                    onLogout = onLogout
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
                containerColor = SharkGreen,
                contentColor = Color.Black,
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
                        Brush.radialGradient(colors = listOf(Color(0xFF0a1a14), SharkBlack), radius = 1200f)
                    )
            ) {
                when (openFeature) {
                    "Bill Tracker"     -> BillTrackerScreen(uid, db, expenses, bills)
                    "Goal Tracker"     -> GoalTrackerScreen(uid, db, expenses, goals)
                    "Visual Models"    -> VisualModelsScreen(expenses, bills, goals)
                    "Tax Tracker"      -> TaxTrackerScreen(expenses)
                    "Inflation Calc"   -> InflationCalcScreen()
                    "Stock/Forex"      -> StockForexScreen()
                    "Profile"          -> ProfileSettingsScreen(displayName, onUpdateProfile)
                    "Account Settings" -> AccountSettingsScreen()
                    else               -> ComingSoonPlaceholder(openFeature!!)
                }

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
                        Box(Modifier.width(18.dp).height(3.dp).background(SharkGreen, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.height(4.dp))
                    } else {
                        Spacer(Modifier.height(7.dp))
                    }
                    Icon(imageVector = tab.icon, contentDescription = null, tint = if (selected) SharkGreen else SharkMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        color = if (selected) SharkGreen else SharkMuted,
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

@Composable
fun HomeScreen(
    uid: String,
    db: FirebaseFirestore,
    displayName: String,
    accountType: String,
    expenses: List<Expense>,
    incomeSources: List<IncomeSource>,
    onExpenseClick: (Expense) -> Unit,
    onAddIncomeClick: () -> Unit
) {
    val totalIncomeConfirmed = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalSpent = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val balance = totalIncomeConfirmed - totalSpent

    // Expected monthly income calculation
    val expectedMonthlyIncome = incomeSources.sumOf { source ->
        when (source.frequency) {
            "Weekly"   -> source.amount * 4.33
            "Biweekly" -> source.amount * 2.16
            "Monthly"  -> source.amount
            else       -> source.amount // One-time
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(60.dp))
        Text("Welcome back,", color = SharkMuted, fontSize = 14.sp)
        Text(displayName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().glassCard(cornerRadius = 24f, alpha = 0.12f).padding(24.dp)) {
            Column {
                Text("Total Balance", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Text("$${String.format("%.2f", balance)}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MiniStat("Income", "+$${String.format("%.0f", totalIncomeConfirmed)}", true)
                    MiniStat("Spent", "-$${String.format("%.0f", totalSpent)}", false)
                }
                
                if (expectedMonthlyIncome > 0) {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Expected this Month", color = SharkMuted, fontSize = 11.sp)
                            Text("$${String.format("%.0f", expectedMonthlyIncome)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val remaining = (expectedMonthlyIncome - totalIncomeConfirmed).coerceAtLeast(0.0)
                            Text("Remaining to Receive", color = SharkMuted, fontSize = 11.sp)
                            Text("$${String.format("%.0f", remaining)}", color = SharkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        IncomeSourcesSection(uid, db, incomeSources, onAddIncomeClick)

        Spacer(Modifier.height(24.dp))
        Text("Recent Transactions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        expenses.take(5).forEach { expense ->
            ExpenseRow(expense, onExpenseClick)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ActivityScreen(expenses: List<Expense>, onAddClick: () -> Unit, onExpenseClick: (Expense) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filteredExpenses = expenses.filter { 
        (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true)) &&
        (selectedCategory == null || it.category == selectedCategory)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(60.dp))
        Text("Activity", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(16.dp))
        SheetInputField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search Transactions",
            placeholder = "Keyword..."
        )
        
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Income", "Bills & Utilities", "Entertainment", "People I Owe").forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SharkGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .clickable { selectedCategory = if (isSelected) null else cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(cat, color = if (isSelected) SharkGreen else SharkMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredExpenses) { expense ->
                ExpenseRow(expense, onExpenseClick)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, onClick: (Expense) -> Unit) {
    val category = expenseCategories.find { it.name == expense.category } ?: expenseCategories[1]
    Row(
        modifier = Modifier.fillMaxWidth().glassCard(16f, 0.06f).clickable { onClick(expense) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(category.color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(expense.category, color = SharkMuted, fontSize = 11.sp)
        }
        Text(
            (if (expense.category == "Income") "+" else "-") + "$${String.format("%.2f", expense.amount)}",
            color = if (expense.category == "Income") SharkGreen else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(expenseCategories[1]) }
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0d1f17)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().imePadding()) {
            Text("Add Transaction", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            SheetInputField(title, { title = it }, "Title", "e.g. Groceries")
            Spacer(Modifier.height(16.dp))
            
            SheetInputField(amount, { amount = it }, "Amount", "0.00", KeyboardType.Decimal)
            Spacer(Modifier.height(16.dp))

            Text("Category", color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(selectedCategory.icon, null, tint = selectedCategory.color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedCategory.name, color = Color.White)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    expenseCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(category.icon, null, tint = category.color, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(category.name)
                                }
                            },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        val expenseRef = db.collection("users").document(uid).collection("expenses").document()
                        val expense = Expense(id = expenseRef.id, title = title, amount = amt, category = selectedCategory.name, createdAt = Date())
                        expenseRef.set(expense).addOnSuccessListener { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGreen)
            ) {
                Text("Save Transaction", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseSheet(expense: Expense, uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(expense.title) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0d1f17)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().imePadding()) {
            Text("Edit Transaction", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            SheetInputField(title, { title = it }, "Title", "e.g. Groceries")
            Spacer(Modifier.height(16.dp))
            SheetInputField(amount, { amount = it }, "Amount", "0.00", KeyboardType.Decimal)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        db.collection("users").document(uid).collection("expenses").document(expense.id).delete().addOnSuccessListener { onDismiss() }
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444).copy(alpha = 0.1f))
                ) {
                    Text("Delete", color = Color(0xFFef4444))
                }
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        db.collection("users").document(uid).collection("expenses").document(expense.id)
                            .update("title", title, "amount", amt).addOnSuccessListener { onDismiss() }
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SharkGreen)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProfileScreen(displayName: String, accountType: String, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp))
        Box(Modifier.size(100.dp).glassCard(50f), contentAlignment = Alignment.Center) {
            Text(displayName.take(1).uppercase(), color = SharkGreen, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(accountType, color = SharkMuted, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444).copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Logout", color = Color(0xFFef4444))
        }
        Spacer(Modifier.height(40.dp))
    }
}
