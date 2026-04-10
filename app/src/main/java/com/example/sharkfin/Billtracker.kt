package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickBillSetupGrid(uid: String, db: FirebaseFirestore) {
    val suggestedBills = listOf(
        RecurringBill("rent", "Rent", 1200.0, "Housing"),
        RecurringBill("car_note", "Car Payment", 400.0, "Transit"),
        RecurringBill("light", "Electric", 120.0, "Utilities"),
        RecurringBill("water", "Water", 60.0, "Utilities"),
        RecurringBill("internet", "Internet", 80.0, "Utilities"),
        RecurringBill("phone", "Phone", 70.0, "Utilities"),
        RecurringBill("netflix", "Netflix", 16.0, "Entertainment"),
        RecurringBill("spotify", "Spotify", 10.0, "Entertainment")
    )

    var selectedKeys by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("TAP TO ADD COMMON BILLS", color = SharkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedBills.forEach { bill ->
                val isSelected = selectedKeys.contains(bill.key)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SharkGold.copy(alpha = 0.2f) else SharkSurface)
                        .border(1.dp, if (isSelected) SharkGold else SharkCardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            if (isSelected) {
                                selectedKeys = selectedKeys - bill.key
                            } else {
                                selectedKeys = selectedKeys + bill.key
                                db.collection("users").document(uid).collection("bills").add(
                                    Bill(
                                        name = bill.label,
                                        amount = bill.amount,
                                        dayOfMonth = 1,
                                        category = bill.category,
                                        recurrence = "Monthly",
                                        isPaid = false
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = SharkGold, modifier = Modifier.size(14.dp))
                        if (isSelected) Spacer(Modifier.width(4.dp))
                        Text(bill.label, color = if (isSelected) SharkGold else SharkLabel, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillTrackerScreen(
    uid: String,
    db: FirebaseFirestore,
    expenses: List<Expense>,
    bills: List<Bill>
) {
    var showAddBill by remember { mutableStateOf(false) }
    var selectedBill by remember { mutableStateOf<Bill?>(null) }
    val scope = rememberCoroutineScope()

    val today = remember { Calendar.getInstance() }
    var viewMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var viewYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    
    // Coach marks state
    var showProjectedCoach by remember { mutableStateOf(false) }
    var showPayCoach by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(800)
        showProjectedCoach = true
        delay(2000)
        if (bills.isNotEmpty()) showPayCoach = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bill Tracker", color = SharkLabel, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Never miss a payment", color = SharkSecondary, fontSize = 13.sp)
            }
            IconButton(
                onClick = { showAddBill = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SharkGold.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Add, null, tint = SharkGold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Upcoming Bills Summary & Projected Balance
        val unpaidBills = bills.filter { !it.isPaid }
        val totalUnpaid = unpaidBills.sumOf { it.amount }
        val totalIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
        val totalSpent = expenses.filter { it.category != "Income" }.sumOf { it.amount }
        val projectedEndBalance = totalIncome - totalSpent - totalUnpaid

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PROJECTED BALANCE", color = SharkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("$${String.format("%.2f", projectedEndBalance)}", color = if (projectedEndBalance >= 0) SharkGold else SharkRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("End of month estimate", color = SharkSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UPCOMING", color = SharkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("$${String.format("%.2f", totalUnpaid)}", color = SharkAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${unpaidBills.size} pending", color = SharkSecondary, fontSize = 11.sp)
                }
            }

            if (showProjectedCoach) {
                CoachMark(
                    text = "Shark predicts your balance after all bills are paid.",
                    modifier = Modifier.align(Alignment.BottomStart).offset(y = 50.dp),
                    onDismiss = { showProjectedCoach = false }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Using the shared BillCalendar from SharedComponents.kt
        BillCalendar(
            bills = bills,
            viewMonth = viewMonth,
            viewYear = viewYear,
            today = today,
            onPrevMonth = { if (viewMonth == 0) { viewMonth = 11; viewYear-- } else viewMonth-- },
            onNextMonth = { if (viewMonth == 11) { viewMonth = 0; viewYear++ } else viewMonth++ },
            onDayClick = { day ->
                val billOnDay = bills.find { it.dayOfMonth == day }
                if (billOnDay != null) selectedBill = billOnDay
            }
        )
        
        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val sortedBills = bills.sortedWith(compareBy({ it.isPaid }, { it.dayOfMonth }))
            items(sortedBills) { bill ->
                Box {
                    BillRowItem(
                        bill = bill,
                        uid = uid,
                        db = db,
                        onClick = { selectedBill = bill }
                    )
                    
                    if (showPayCoach && !bill.isPaid && sortedBills.indexOf(bill) == 0) {
                        CoachMark(
                            text = "Tap 'PAY' to log a payment and update your cash.",
                            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-60).dp, y = 30.dp),
                            onDismiss = { showPayCoach = false }
                        )
                    }
                }
            }
            
            if (bills.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = SharkSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No bills yet", color = SharkSecondary, fontSize = 16.sp)
                            Text("Log recurring costs to see projections", color = SharkSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddBill) {
        AddBillSheet(uid, db) { showAddBill = false }
    }
    
    selectedBill?.let { bill ->
        BillDetailSheet(bill, uid, db) { selectedBill = null }
    }
}

@Composable
fun BillRowItem(bill: Bill, uid: String, db: FirebaseFirestore, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val category = billCategories.find { it.name == bill.category } ?: billCategories.last()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(alpha = if (bill.isPaid) 0.03f else 0.07f)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (bill.isPaid) SharkSecondary.copy(alpha = 0.1f) else category.color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(category.icon, null, tint = if (bill.isPaid) SharkSecondary else category.color, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(bill.name, color = if (bill.isPaid) SharkSecondary else SharkLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Due: ${ordinal(bill.dayOfMonth)} · ${bill.recurrence}", color = SharkSecondary, fontSize = 12.sp)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("$${String.format("%.0f", bill.amount)}", color = if (bill.isPaid) SharkSecondary else SharkLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (bill.isPaid) SharkGold.copy(alpha = 0.1f) else SharkAmber.copy(alpha = 0.1f))
                    .clickable {
                        scope.launch {
                            val nextPaid = !bill.isPaid
                            db.collection("users").document(uid)
                                .collection("bills").document(bill.id)
                                .update("isPaid", nextPaid)
                            
                            if (nextPaid) {
                                // Log as expense to update balance
                                val expenseData = hashMapOf(
                                    "title" to bill.name,
                                    "amount" to bill.amount,
                                    "category" to bill.category,
                                    "note" to "Bill payment",
                                    "createdAt" to java.util.Date()
                                )
                                db.collection("users").document(uid).collection("expenses").add(expenseData)
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    if (bill.isPaid) "PAID" else "PAY",
                    color = if (bill.isPaid) SharkGold else SharkAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillSheet(uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf(billCategories.first().name) }
    var recurrence by remember { mutableStateOf("Monthly") }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SharkSurface,
        contentColor = SharkLabel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Add New Bill", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SharkLabel)

            SheetInputField(name, { name = it }, "BILL NAME", "e.g. Rent, Electric")
            SheetInputField(amount, { amount = it }, "AMOUNT", "0.00", KeyboardType.Decimal)
            SheetInputField(day, { day = it }, "DUE DAY (1-31)", "1", KeyboardType.Number)

            Text("CATEGORY", color = SharkSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                billCategories.take(4).forEach { cat ->
                    CategoryPill(cat.name, selectedCategory == cat.name) { selectedCategory = cat.name }
                }
            }

            Button(
                onClick = {
                    val bill = Bill(
                        name = name,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        dayOfMonth = day.toIntOrNull() ?: 1,
                        category = selectedCategory,
                        recurrence = recurrence,
                        isPaid = false
                    )
                    db.collection("users").document(uid).collection("bills").add(bill).addOnSuccessListener { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Bill", color = SharkBg, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun CategoryPill(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SharkGold.copy(alpha = 0.15f) else SharkSurface)
            .border(1.dp, if (isSelected) SharkGold else SharkCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(name, color = if (isSelected) SharkGold else SharkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailSheet(bill: Bill, uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(bill.name) }
    var amount by remember { mutableStateOf(bill.amount.toString()) }
    var day by remember { mutableStateOf(bill.dayOfMonth.toString()) }
    var selectedCategory by remember { mutableStateOf(bill.category) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SharkSurface) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Edit Bill", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
            
            SheetInputField(name, { name = it }, "BILL NAME", "e.g. Rent")
            SheetInputField(amount, { amount = it }, "AMOUNT", "0.00", KeyboardType.Decimal)
            SheetInputField(day, { day = it }, "DUE DAY (1-31)", "1", KeyboardType.Number)

            Text("CATEGORY", color = SharkSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                billCategories.take(4).forEach { cat ->
                    CategoryPill(cat.name, selectedCategory == cat.name) { selectedCategory = cat.name }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    db.collection("users").document(uid)
                        .collection("bills").document(bill.id)
                        .update(mapOf(
                            "name" to name,
                            "amount" to (amount.toDoubleOrNull() ?: bill.amount),
                            "dayOfMonth" to (day.toIntOrNull() ?: bill.dayOfMonth),
                            "category" to selectedCategory
                        )).addOnSuccessListener { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Update Bill", color = SharkBg, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    db.collection("users").document(uid).collection("bills").document(bill.id).delete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SharkRed.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Bill", color = SharkRed)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

fun markBillPaidByKey(
    uid        : String,
    db         : FirebaseFirestore,
    recurringKey: String,
    bills      : List<Bill>
) {
    // Find the bill that matches the recurring key by name
    val keyToNameMap = mapOf(
        "rent"     to "Rent",
        "car_note" to listOf("Car Note", "Car Payment", "Car Loan"),
        "light"    to listOf("Light", "Electric", "Light Bill"),
        "water"    to listOf("Water", "Water Bill"),
        "gas_bill" to listOf("Gas Bill", "Gas"),
        "internet" to listOf("Internet", "WiFi", "Wi-Fi"),
        "phone"    to listOf("Phone", "Phone Bill"),
        "netflix"  to listOf("Netflix"),
        "spotify"  to listOf("Spotify"),
        "apple"    to listOf("Apple"),
        "car_ins"  to listOf("Car Insurance", "Insurance"),
        "court"    to listOf("Court", "Court Payment"),
        "probation" to listOf("Probation", "Probation Fee"),
        "gas_car"  to listOf("Gas", "Fill Up")
    )

    val possibleNames = keyToNameMap[recurringKey]
    val matchedBill = bills.firstOrNull { bill ->
        when (possibleNames) {
            is String -> bill.name.equals(possibleNames, ignoreCase = true)
            is List<*> -> possibleNames.any { name ->
                bill.name.equals(name as String, ignoreCase = true)
            }
            else -> false
        }
    }

    matchedBill?.let { bill ->
        if (!bill.isPaid) {
            db.collection("users").document(uid)
                .collection("bills")
                .document(bill.id)
                .update("isPaid", true)

            // Log as expense to update balance
            val expenseData = hashMapOf(
                "title" to bill.name,
                "amount" to bill.amount,
                "category" to bill.category,
                "note" to "Paid via Shark command",
                "createdAt" to java.util.Date()
            )
            db.collection("users").document(uid).collection("expenses").add(expenseData)
        }
    }
}
