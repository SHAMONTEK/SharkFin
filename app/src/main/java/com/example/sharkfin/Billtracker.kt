package com.example.sharkfin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*

// ─── Bill Data Class ───────────────────────────────────────────────────────
@Composable
fun BillTrackerScreen(
    uid: String,
    db: FirebaseFirestore,
    expenses: List<Expense>,
    bills: List<Bill>
) {
    var showAddBill by remember { mutableStateOf(false) }
    var selectedBill by remember { mutableStateOf<Bill?>(null) }

    val today = remember { Calendar.getInstance() }
    var viewMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var viewYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }

    val monthlyBillTotal = bills
        .filter { it.recurrence != "One-time" || !it.isPaid }
        .sumOf { it.amount }

    val totalIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalExpenses = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val projectedBalance = totalIncome - totalExpenses - monthlyBillTotal

    val upcomingBills = remember(bills, today) {
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        bills.filter { bill ->
            !bill.isPaid &&
                    bill.dayOfMonth >= todayDay &&
                    bill.dayOfMonth <= todayDay + 14
        }.sortedBy { it.dayOfMonth }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bill Tracker", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("${bills.size} bills tracked", color = SharkMuted, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SharkGreen, CircleShape)
                    .clickable { showAddBill = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add Bill", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(colors = listOf(Color(0xFF0d3d2b), Color(0xFF0a2a1e))))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("End of Month", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(
                        "\$${String.format("%.2f", projectedBalance)}",
                        color = if (projectedBalance >= 0) SharkGreen else Color(0xFFef4444),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (projectedBalance >= 0) "You're on track ✓" else "Bills exceed income ⚠️",
                        color = if (projectedBalance >= 0) SharkGreen.copy(alpha = 0.7f) else Color(0xFFef4444).copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Monthly Bills", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(
                        "-\$${String.format("%.2f", monthlyBillTotal)}",
                        color = Color(0xFFf59e0b),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (upcomingBills.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFf59e0b).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFf59e0b).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFf59e0b), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Due in the next 2 weeks", color = Color(0xFFf59e0b), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    upcomingBills.forEach { bill ->
                        val daysUntil = bill.dayOfMonth - today.get(Calendar.DAY_OF_MONTH)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(bill.name, color = Color.White, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    when (daysUntil) {
                                        0 -> "Today"
                                        1 -> "Tomorrow"
                                        else -> "In $daysUntil days"
                                    },
                                    color = if (daysUntil <= 2) Color(0xFFef4444) else Color(0xFFf59e0b),
                                    fontSize = 11.sp
                                )
                                Text("-\$${String.format("%.2f", bill.amount)}", color = Color(0xFFf59e0b), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

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

        Spacer(modifier = Modifier.height(20.dp))

        Text("All Bills", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        if (bills.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 0.06f).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = SharkMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No bills added yet", color = SharkMuted, fontSize = 13.sp)
                    Text("Tap + to add your first bill", color = SharkMuted.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        } else {
            bills.sortedBy { it.dayOfMonth }.forEach { bill ->
                BillRow(
                    bill = bill,
                    onMarkPaid = { billId, paid ->
                        db.collection("users").document(uid).collection("bills").document(billId).update("isPaid", paid)
                    },
                    onDelete = { billId ->
                        db.collection("users").document(uid).collection("bills").document(billId).delete()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showAddBill) {
        AddBillSheet(uid = uid, db = db, onDismiss = { showAddBill = false })
    }

    if (selectedBill != null) {
        BillDetailSheet(bill = selectedBill!!, uid = uid, db = db, onDismiss = { selectedBill = null })
    }
}

@Composable
fun BillCalendar(
    bills: List<Bill>,
    viewMonth: Int,
    viewYear: Int,
    today: Calendar,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Int) -> Unit
) {
    val cal = Calendar.getInstance().apply { set(viewYear, viewMonth, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 6) % 7
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    val isCurrentMonth = viewMonth == today.get(Calendar.MONTH) && viewYear == today.get(Calendar.YEAR)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val billsByDay = bills.groupBy { it.dayOfMonth }

    Column(modifier = Modifier.fillMaxWidth().glassCard(cornerRadius = 20f, alpha = 0.07f).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, "Previous Month", tint = SharkMuted) }
            Text(monthName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, "Next Month", tint = SharkMuted) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = SharkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).height(48.dp))
                    } else {
                        val billsOnDay = billsByDay[day] ?: emptyList()
                        val isToday = isCurrentMonth && day == todayDay
                        val hasBill = billsOnDay.isNotEmpty()
                        val allPaid = billsOnDay.all { it.isPaid }
                        Box(
                            modifier = Modifier.weight(1f).height(48.dp).padding(2.dp).clip(RoundedCornerShape(10.dp))
                                .then(if (isToday) Modifier.border(1.dp, SharkGreen, RoundedCornerShape(10.dp)) else Modifier)
                                .background(when {
                                    hasBill && !allPaid -> Color(0xFFf59e0b).copy(alpha = 0.1f)
                                    hasBill && allPaid  -> SharkGreen.copy(alpha = 0.07f)
                                    else                -> Color.Transparent
                                })
                                .clickable(enabled = hasBill) { onDayClick(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$day", color = when {
                                    isToday -> SharkGreen
                                    hasBill && !allPaid -> Color(0xFFf59e0b)
                                    hasBill && allPaid  -> SharkMuted
                                    else -> Color.White.copy(alpha = 0.7f)
                                }, fontSize = 13.sp, fontWeight = if (isToday || hasBill) FontWeight.Bold else FontWeight.Normal)
                                if (hasBill) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(modifier = Modifier.size(5.dp).background(if (allPaid) SharkMuted else Color(0xFFf59e0b), CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFf59e0b), CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bill due", color = SharkMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(8.dp).background(SharkGreen, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Paid", color = SharkMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(8.dp).border(1.dp, SharkGreen, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Today", color = SharkMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun BillRow(bill: Bill, onMarkPaid: (String, Boolean) -> Unit, onDelete: (String) -> Unit) {
    val category = billCategories.find { it.name == bill.category } ?: billCategories.last()
    Row(modifier = Modifier.fillMaxWidth().glassCard(cornerRadius = 16f, alpha = if (bill.isPaid) 0.04f else 0.07f).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(category.color.copy(alpha = if (bill.isPaid) 0.08f else 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(category.icon, null, tint = if (bill.isPaid) SharkMuted else category.color, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bill.name, color = if (bill.isPaid) SharkMuted else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Due ${ordinal(bill.dayOfMonth)} · ${bill.recurrence}", color = SharkMuted, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("-\$${String.format("%.2f", bill.amount)}", color = if (bill.isPaid) SharkMuted else Color(0xFFf59e0b), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.size(28.dp).background(if (bill.isPaid) SharkGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), CircleShape).clickable { onMarkPaid(bill.id, !bill.isPaid) }, contentAlignment = Alignment.Center) {
            Icon(if (bill.isPaid) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, null, tint = if (bill.isPaid) SharkGreen else SharkMuted, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.size(28.dp).background(Color(0xFFef4444).copy(alpha = 0.08f), CircleShape).clickable { onDelete(bill.id) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Delete, null, tint = Color(0xFFef4444).copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailSheet(bill: Bill, uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    val category = billCategories.find { it.name == bill.category } ?: billCategories.last()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0d1f17), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).navigationBarsPadding().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(60.dp).background(category.color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(bill.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(bill.category, color = SharkMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
            BillDetailRow("Amount", "-\$${String.format("%.2f", bill.amount)}")
            BillDetailRow("Due Date", "Every ${ordinal(bill.dayOfMonth)}")
            BillDetailRow("Recurrence", bill.recurrence)
            BillDetailRow("Status", if (bill.isPaid) "✓ Paid" else "Unpaid")
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { db.collection("users").document(uid).collection("bills").document(bill.id).update("isPaid", !bill.isPaid); onDismiss() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (bill.isPaid) SharkMuted else SharkGreen)) {
                Icon(if (bill.isPaid) Icons.Default.Refresh else Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (bill.isPaid) "Mark as Unpaid" else "Mark as Paid", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BillDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SharkMuted, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillSheet(uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dayOfMonth by remember { mutableStateOf("1") }
    var selectedCat by remember { mutableStateOf(billCategories[0]) }
    var selectedRec by remember { mutableStateOf("Monthly") }
    var isSaving by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0d1f17), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), dragHandle = { Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).width(40.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))) }) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp).navigationBarsPadding().imePadding()) {
            Text("Add Bill", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Category", color = SharkMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            billCategories.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cat ->
                        val isSelected = selectedCat == cat
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) cat.color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)).clickable { selectedCat = cat }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(cat.icon, null, tint = if (isSelected) cat.color else SharkMuted, modifier = Modifier.size(13.dp))
                                Text(cat.name, color = if (isSelected) cat.color else SharkMuted, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            SheetInputField(value = name, onValueChange = { name = it }, label = "Bill Name", placeholder = "e.g. Rent, Netflix, Car Insurance")
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) { SheetInputField(value = amount, onValueChange = { amount = it }, label = "Amount", placeholder = "0.00", keyboardType = KeyboardType.Decimal) }
                Box(modifier = Modifier.weight(1f)) { SheetInputField(value = dayOfMonth, onValueChange = { dayOfMonth = it }, label = "Day of Month", placeholder = "1–31", keyboardType = KeyboardType.Number) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recurrence", color = SharkMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                recurrenceOptions.forEach { option ->
                    val isSelected = selectedRec == option
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) SharkGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)).clickable { selectedRec = option }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(option, color = if (isSelected) SharkGreen else SharkMuted, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(onClick = {
                val parsedAmount = amount.toDoubleOrNull()
                val parsedDay = dayOfMonth.toIntOrNull()?.coerceIn(1, 31) ?: 1
                if (name.isNotBlank() && parsedAmount != null && parsedAmount > 0) {
                    isSaving = true
                    val bill = Bill(name = name.trim(), amount = parsedAmount, dayOfMonth = parsedDay, recurrence = selectedRec, category = selectedCat.name)
                    db.collection("users").document(uid).collection("bills").add(bill).addOnSuccessListener { isSaving = false; onDismiss() }.addOnFailureListener { isSaving = false }
                }
            }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = SharkGreen), enabled = !isSaving) {
                if (isSaving) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save Bill", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}