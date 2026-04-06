package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun PassiveSnowballScreen(uid: String, db: FirebaseFirestore, expenses: List<Expense>, bills: List<Bill>) {
    val passiveIncome = expenses.filter { it.category == "Passive Income" }.sumOf { it.amount }
    val totalBills = bills.sumOf { it.amount }
    
    var projectionMode by remember { mutableStateOf("Weekly") }
    val multiplier = if (projectionMode == "Weekly") 1.0 else 4.33
    val projectedAmount = passiveIncome * multiplier

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(100.dp))
        Text("Passive Snowball", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("The more you make, the easier life gets.", color = SharkMuted, fontSize = 14.sp)
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(24f, 0.1f)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(projectionMode.uppercase(), color = SharkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    IconButton(onClick = { projectionMode = if (projectionMode == "Weekly") "Monthly" else "Weekly" }) {
                        Icon(Icons.Default.SwapVert, null, tint = SharkMuted, modifier = Modifier.size(16.dp))
                    }
                }
                Text("$${String.format(Locale.US, "%.2f", projectedAmount)}", color = SharkGreen, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(24.dp))
                
                val coverage = if (totalBills > 0) (projectedAmount / (totalBills / 4.33 * multiplier)).toFloat().coerceIn(0f, 2f) else 0f
                
                LinearProgressIndicator(
                    progress = { (coverage / 1f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                    color = SharkGreen,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                
                Spacer(Modifier.height(12.dp))
                val percent = (coverage * 100).toInt()
                Text("Covers $percent% of your ${projectionMode.lowercase()} bills", color = if (percent >= 100) SharkGreen else SharkMuted, fontSize = 14.sp)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Quick adjust bar
        Text("ADJUST WEEKLY TARGET", color = SharkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = passiveIncome.toFloat(),
            onValueChange = { /* In a real app, update state/DB */ },
            valueRange = 0f..5000f,
            colors = SliderDefaults.colors(thumbColor = SharkGreen, activeTrackColor = SharkGreen)
        )

        Spacer(Modifier.height(32.dp))
        
        Text("INCOME BREAKDOWN", color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        val items = expenses.filter { it.category == "Passive Income" }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No passive income logged yet.", color = SharkMuted, textAlign = TextAlign.Center)
            }
        } else {
            items.forEach { item ->
                PassiveIncomeRow(item)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PassiveIncomeRow(expense: Expense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(16f, 0.05f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(SharkGreen.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = SharkGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(expense.category, color = SharkMuted, fontSize = 12.sp)
        }
        Text("+$${String.format(Locale.US, "%.2f", expense.amount)}", color = SharkGreen, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtVanishScreen(uid: String, db: FirebaseFirestore, debts: List<Debt>) {
    val totalDebt = debts.sumOf { it.currentBalance }
    var showAddDebt by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(100.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Debt Vanish", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Visualize your exit from debt", color = SharkMuted, fontSize = 14.sp)
            }
            IconButton(onClick = { showAddDebt = true }, modifier = Modifier.background(SharkRed.copy(alpha = 0.1f), CircleShape)) {
                Icon(Icons.Default.Add, null, tint = SharkRed)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(24f, 0.1f)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL REMAINING DEBT", color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$${String.format(Locale.US, "%,.2f", totalDebt)}", color = SharkRed, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("ACTIVE DEBTS", color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        if (debts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No debts added. Add them to track progress!", color = SharkMuted, textAlign = TextAlign.Center)
            }
        } else {
            debts.forEach { debt ->
                DebtRow(debt, uid, db)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showAddDebt) {
        AddDebtSheet(uid, db) { showAddDebt = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtSheet(uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("") }
    
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SharkBase) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add Debt", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            SheetInputField(name, { name = it }, "DEBT NAME", "e.g. Credit Card")
            SheetInputField(balance, { balance = it }, "TOTAL BALANCE", "0.00", androidx.compose.ui.text.input.KeyboardType.Decimal)
            SheetInputField(apr, { apr = it }, "APR (%)", "19.99", androidx.compose.ui.text.input.KeyboardType.Decimal)
            
            Button(
                onClick = {
                    val debt = Debt(
                        name = name,
                        currentBalance = balance.toDoubleOrNull() ?: 0.0,
                        apr = apr.toDoubleOrNull() ?: 0.0,
                        id = ""
                    )
                    db.collection("users").document(uid).collection("debts").add(debt).addOnSuccessListener { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Start Vanishing", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun DebtRow(debt: Debt, uid: String, db: FirebaseFirestore) {
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmount by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(16f, 0.05f)
            .clickable { showPaymentDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(SharkRed.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CreditCard, null, tint = SharkRed, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(debt.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("${debt.apr}% APR", color = SharkMuted, fontSize = 12.sp)
        }
        Text("$${String.format(Locale.US, "%,.0f", debt.currentBalance)}", color = Color.White, fontWeight = FontWeight.Bold)
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            containerColor = SharkSurface,
            title = { Text("Make a Payment", color = Color.White) },
            text = {
                Column {
                    Text("Paying down ${debt.name}", color = SharkMuted)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = SharkMuted, focusedBorderColor = SharkRed, focusedLabelColor = SharkRed, cursorColor = SharkRed, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pay = paymentAmount.toDoubleOrNull() ?: 0.0
                    if (pay > 0) {
                        db.collection("users").document(uid).collection("debts").document(debt.id)
                            .update("currentBalance", (debt.currentBalance - pay).coerceAtLeast(0.0))
                    }
                    showPaymentDialog = false
                }) { Text("Confirm", color = SharkRed) }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel", color = SharkMuted) }
            }
        )
    }
}

@Composable
fun FreedomRunwayScreen(expenses: List<Expense>, bills: List<Bill>) {
    val income  = expenses.filter { it.category == "Income" || it.category == "1099 Income" || it.category == "Passive Income" }.sumOf { it.amount }
    val spent   = expenses.filter { it.category != "Income" && it.category != "Passive Income" }.sumOf { it.amount }
    val balance = income - spent
    
    // Average daily burn (last 30 days)
    val avgDailyBurn = if (expenses.isNotEmpty()) {
        val totalSpent = spent
        val firstExpense = expenses.minByOrNull { it.createdAtDate.time }?.createdAtDate?.time ?: System.currentTimeMillis()
        val daysElapsed = ((System.currentTimeMillis() - firstExpense) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        totalSpent / daysElapsed.toDouble()
    } else 0.0
    
    val runwayDays = if (avgDailyBurn > 0) (balance / avgDailyBurn).toInt().coerceAtLeast(0) else 0

    var expandedBurn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(100.dp))
        Text("Freedom Runway", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("How long you can survive without a job", color = SharkMuted, fontSize = 14.sp)
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(24f, 0.1f)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$runwayDays", color = SharkNavy, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                Text("DAYS OF FREEDOM", color = SharkMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("INSIGHTS", color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        RunwayInsight("Current Balance", "$${String.format(Locale.US, "%,.2f", balance)}", Icons.Default.AccountBalance)
        Spacer(Modifier.height(12.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(16f, 0.05f)
                .clickable { expandedBurn = !expandedBurn }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = SharkNavy, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text("Avg. Daily Burn", color = SharkMuted, modifier = Modifier.weight(1f))
                Text("$${String.format(Locale.US, "%.2f", avgDailyBurn)}", color = Color.White, fontWeight = FontWeight.Bold)
                Icon(
                    if (expandedBurn) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = SharkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            AnimatedVisibility(visible = expandedBurn) {
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your burn rate is calculated based on all recorded expenses. To extend your runway, try reducing recurring subscriptions or dining out.",
                        color = SharkMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    // Simple trend indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trend:", color = SharkMuted, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("STABLE", color = SharkNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.TrendingFlat, null, tint = SharkNavy, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        
        val nextBill = bills.filter { !it.isPaid }.minByOrNull { it.dayOfMonth }
        RunwayInsight(
            "Next Major Bill", 
            nextBill?.let { "${it.name} (${ordinal(it.dayOfMonth)})" } ?: "None", 
            Icons.Default.Event
        )
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun RunwayInsight(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(16f, 0.05f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkNavy, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = SharkMuted, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
