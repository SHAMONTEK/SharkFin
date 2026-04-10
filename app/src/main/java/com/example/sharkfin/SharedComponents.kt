package com.example.sharkfin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // Required for 'by' property delegates
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// ─── DESIGN SYSTEM CONSTANTS (Marcus Bank Aesthetic) ──────────────────────
val SharkBg         = Color(0xFF0E1117) // Dark Background
val SharkWhite      = Color(0xFFFFFFFF)
val SharkCardBorder = Color(0xFF1F2D3D) // Subtle Border

val SharkGold       = Color(0xFFE8B84B) // Marcus Gold
val SharkGoldGlow   = Color(0x26E8B84B)

val SharkGreenDark  = Color(0xFF4CAF82)
val SharkGreenMid   = Color(0xFF4CAF82)
val SharkGreenLight = Color(0x1A4CAF82)

val SharkLabel      = Color(0xFFFFFFFF) // High Contrast Text
val SharkSecondary  = Color(0xFFB0BEC5) // Muted Text
val SharkTertiary   = Color(0xFF546E7A)

val SharkRed        = Color(0xFFE05C5C)
val SharkAmber      = Color(0xFFE8944B)
val SharkTeal       = Color(0xFF4BA3E8)
val SharkPurple     = Color(0xFFAF52DE)

val SharkIncomeCategories = listOf("Income", "1099 Income", "Passive Income", "Salary", "Bonus", "Dividend", "Side Hustle")

// ─── LEGACY BRIDGE (Updated for Marcus Aesthetic) ───────────────────────────
val SharkBlack        = Color(0xFF000000)
val SharkDark         = Color(0xFF050505)
val SharkDeepOcean    = Color(0xFF0E1117) 
val SharkCard         = Color(0xFF161D27) // SharkSurface
val SharkBorderSubtle = SharkCardBorder
val SharkGreen        = SharkGold         // Gold as primary accent
val SharkNavy         = SharkGold
val SharkMuted        = SharkSecondary
val SharkBase         = SharkBg
val SharkSurface      = Color(0xFF161D27)
val SharkSurfaceHigh  = Color(0xFF1C2535)

// ─── MODIFIERS ────────────────────────────────────────────────────────────
fun Modifier.glassCard(cornerRadius: Float = 22f, alpha: Float = 1.0f): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(SharkSurface.copy(alpha = alpha))
    .border(0.5.dp, SharkCardBorder, RoundedCornerShape(cornerRadius.dp))

// ─── DATA MODELS ──────────────────────────────────────────────────────────
data class BillCategory(val name: String, val icon: ImageVector, val color: Color)
data class ExpenseCategory(val name: String, val icon: ImageVector, val color: Color)
data class GoalCategory(val name: String, val icon: ImageVector, val color: Color)

val billCategories: List<BillCategory> = listOf(
    BillCategory("Housing", Icons.Default.Home, SharkAmber),
    BillCategory("Utilities", Icons.Default.FlashOn, SharkTeal),
    BillCategory("Subscriptions", Icons.Default.Tv, SharkPurple),
    BillCategory("Transport", Icons.Default.DirectionsCar, SharkNavy),
    BillCategory("Insurance", Icons.Default.Security, SharkRed),
    BillCategory("Other", Icons.Default.MoreHoriz, SharkSecondary)
)

val expenseCategories: List<ExpenseCategory> = listOf(
    ExpenseCategory("Income", Icons.AutoMirrored.Filled.TrendingUp, SharkGreenMid),
    ExpenseCategory("Bills & Utilities", Icons.AutoMirrored.Filled.ReceiptLong, SharkAmber),
    ExpenseCategory("Entertainment", Icons.Default.MusicNote, SharkPurple),
    ExpenseCategory("Food", Icons.Default.Restaurant, SharkTeal),
    ExpenseCategory("Other", Icons.Default.Category, SharkSecondary)
)

val goalCategories: List<GoalCategory> = listOf(
    GoalCategory("Savings", Icons.Default.Savings, SharkGreenMid),
    GoalCategory("Emergency", Icons.Default.Shield, SharkRed),
    GoalCategory("Tech", Icons.Default.Laptop, SharkTeal),
    GoalCategory("Other", Icons.Default.Star, SharkSecondary)
)

// ─── CORE DATA CLASSES ────────────────────────────────────────────────────
enum class SharkMood { HAPPY, NEUTRAL, CONCERNED, SAD, PROUD, CURIOUS, UPSET, HUNGRY }
enum class SharkAgentSession { IDLE, AWAITING_SETUP_BALANCE, AWAITING_SAVINGS_TARGET, AWAITING_PAYDAY, AWAITING_RESET_CONFIRM }

data class SharkChatMessage(val text: String, val isShark: Boolean, val timestamp: Long = System.currentTimeMillis())

data class Expense(val id: String = "", val title: String = "", val amount: Double = 0.0, val category: String = "Other", val note: String = "", val createdAt: Any? = null) {
    val createdAtDate: Date get() = when (val c = createdAt) { is Timestamp -> c.toDate(); is Date -> c; else -> Date() }
}

data class Bill(val id: String = "", val name: String = "", val amount: Double = 0.0, val dayOfMonth: Int = 1, val recurrence: String = "Monthly", val isPaid: Boolean = false, val category: String = "Bills & Utilities")

data class Goal(val id: String = "", val name: String = "", val targetAmount: Double = 0.0, val savedAmount: Double = 0.0, val category: String = "Savings", val deadline: String = "", val isCompleted: Boolean = false, val colorHex: String = "#3B6D11")

data class Debt(val id: String = "", val name: String = "", val totalAmount: Double = 0.0, val currentBalance: Double = 0.0, val minimumPayment: Double = 0.0, val interestRate: Double = 0.0, val apr: Double = 0.0)

data class IncomeSource(val id: String = "", val name: String = "", val amount: Double = 0.0, val frequency: String = "Monthly")

data class RecurringBill(val key: String = "", val label: String = "", val amount: Double = 0.0, val category: String = "Other")

data class PortfolioAsset(val id: String = "", val symbol: String = "", val shares: Double = 0.0, val quantity: Double = 0.0, val currentPrice: Double = 0.0, val averageCost: Double = 0.0, val currentValue: Double = 0.0)

// ─── UI COMPONENTS ────────────────────────────────────────────────────────
@Composable
fun SharkCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SharkSurface, RoundedCornerShape(22.dp))
            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
fun SharkSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = SharkSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun FeatureTutorialOverlay(title: String, description: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBlack.copy(alpha = 0.8f))
            .clickable { onDismiss() }
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        SharkCard {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SharkLabel)
            Spacer(Modifier.height(8.dp))
            Text(description, fontSize = 15.sp, color = SharkSecondary, lineHeight = 20.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGreenMid),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = SharkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = SharkTertiary) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = SharkGold,
                unfocusedIndicatorColor = SharkCardBorder
            )
        )
    }
}

@Composable
fun MiniStat(label: String, value: String, positive: Boolean) {
    Column {
        Text(label, color = SharkSecondary, fontSize = 11.sp)
        Text(value, color = if (positive) SharkGreenMid else SharkRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CoachMark(text: String, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(SharkGreenLight, RoundedCornerShape(12.dp))
            .clickable { onDismiss() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, null, tint = SharkGreenMid, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = SharkGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SharkChatBubble(message: String, isShark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isShark) Arrangement.Start else Arrangement.End
    ) {
        if (isShark) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(SharkGreenLight), contentAlignment = Alignment.Center) {
                Text("🦈", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isShark) Color(0xFFE9E9EB) else SharkGreenDark,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message, color = if (isShark) SharkLabel else Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
fun BillCalendar(
    bills: List<Bill> = emptyList(),
    viewMonth: Int,
    viewYear: Int,
    today: Calendar = Calendar.getInstance(),
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Int) -> Unit = {}
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, viewYear)
        set(Calendar.MONTH, viewMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val billsByDay = bills.groupBy { it.dayOfMonth }
    val isTodayMonth = today.get(Calendar.MONTH) == viewMonth && today.get(Calendar.YEAR) == viewYear

    SharkCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, null, tint = SharkSecondary) }
            Text(monthName, color = SharkLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, null, tint = SharkSecondary) }
        }
        Spacer(Modifier.height(16.dp))

        // Weekday headers
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(day, color = SharkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
            }
        }
        
        Spacer(Modifier.height(8.dp))

        // Calendar Grid
        val totalCells = ((maxDays + firstDayOfWeek - 1 + 6) / 7) * 7
        for (i in 0 until totalCells step 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (j in 0 until 7) {
                    val dayNum = i + j - (firstDayOfWeek - 2)
                    if (dayNum in 1..maxDays) {
                        val hasBill = billsByDay.containsKey(dayNum)
                        val isToday = isTodayMonth && today.get(Calendar.DAY_OF_MONTH) == dayNum
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isToday) SharkGold.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onDayClick(dayNum) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$dayNum", 
                                    color = if (isToday) SharkGold else SharkLabel, 
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasBill) {
                                    Box(Modifier.size(4.dp).background(SharkAmber, CircleShape))
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── UTILITIES ────────────────────────────────────────────────────────────
fun ordinal(n: Int): String = when {
    n in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}

fun getCategoryColor(name: String): Color = when (name) {
    "Income" -> SharkGreenMid
    "Bills & Utilities", "Housing" -> SharkAmber
    "Entertainment", "Subscriptions" -> SharkPurple
    "Food" -> SharkTeal
    "Insurance", "Emergency" -> SharkRed
    "Transport" -> SharkTeal
    else -> SharkSecondary
}

fun calculateEstimatedTax(taxableIncome: Double, stateCode: String = "GA", is1099: Boolean = false): Double {
    val federal = when {
        taxableIncome <= 11000 -> taxableIncome * 0.10
        taxableIncome <= 44725 -> 1100 + (taxableIncome - 11000) * 0.12
        taxableIncome <= 95375 -> 5147 + (taxableIncome - 44725) * 0.22
        else                   -> 16290 + (taxableIncome - 95375) * 0.24
    }
    val stateRate = when(stateCode) {
        "CA" -> 0.09
        "NY" -> 0.06
        "GA" -> 0.0549
        else -> 0.00
    }
    val selfEmployment = if(is1099) taxableIncome * 0.153 else 0.0
    return federal + (taxableIncome * stateRate) + selfEmployment
}

fun calcMoneyScore(
    income: Double, spent: Double, bills: List<Bill>,
    goals: List<Goal>, debts: List<Debt>,
    avgDailyBurn: Double, balance: Double,
    passiveIncome: Double
): Int {
    var score = 0
    val savingsRate = if (income > 0) (income - spent) / income * 100 else 0.0
    score += when { savingsRate >= 20 -> 30; savingsRate >= 10 -> 20; savingsRate >= 0 -> 10; else -> 0 }
    val totalBills = bills.size
    val paidBills = bills.count { it.isPaid }
    score += if (totalBills > 0) ((paidBills.toDouble() / totalBills) * 20).toInt() else 20
    val activeGoals = goals.filter { !it.isCompleted }
    val avgGoalPct = if (activeGoals.isNotEmpty()) activeGoals.map { if (it.targetAmount > 0) it.savedAmount / it.targetAmount else 0.0 }.average() else 0.5
    score += (avgGoalPct * 15).toInt()
    val totalDebt = debts.sumOf { it.currentBalance }
    val debtRatio = if (income > 0) totalDebt / (income * 12) else 1.0
    score += when { debtRatio == 0.0 -> 15; debtRatio < 0.2 -> 12; debtRatio < 0.5 -> 7; debtRatio < 1.0 -> 3; else -> 0 }
    val runwayDays = if (avgDailyBurn > 0) (balance / avgDailyBurn).toInt() else 999
    score += when { runwayDays >= 90 -> 10; runwayDays >= 30 -> 7; runwayDays >= 14 -> 4; runwayDays >= 7 -> 2; else -> 0 }
    val passiveVsSpend = if (spent > 0) passiveIncome / spent else 0.0
    score += when { passiveVsSpend >= 1.0 -> 10; passiveVsSpend >= 0.5 -> 7; passiveVsSpend >= 0.1 -> 4; passiveVsSpend > 0 -> 2; else -> 0 }
    return score.coerceIn(0, 100)
}

@Composable
fun MarcusThemeContrastAudit() {
    // This is a helper component for the developer to audit contrast levels
    // during development of the Marcus-style theme.
    Column(Modifier.background(Color(0xFF0E1117)).padding(16.dp)) {
        Text("Marcus Dark Theme Contrast Audit", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(Color(0xFFE8B84B)))
            Spacer(Modifier.width(8.dp))
            Text("SharkGold (AA on Black)", color = Color(0xFFE8B84B))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(Color(0xFFB0BEC5)))
            Spacer(Modifier.width(8.dp))
            Text("SharkTextSecondary (Pass)", color = Color(0xFFB0BEC5))
        }
    }
}