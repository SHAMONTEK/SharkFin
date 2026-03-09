package com.example.sharkfin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

// ─── Core Colors (Shark Ocean Theme) ───────────────────────────────────────
val SharkNavy       = Color(0xFF0A84FF) // Electric Shark Blue
val SharkDeepOcean  = Color(0xFF001220) // Deep Navy Base
val SharkBlack      = Color(0xFF000000)
val SharkDark       = Color(0xFF050505)
val SharkCard       = Color(0xFF0D1B2A)
val SharkCardBorder = Color(0xFF1B263B)
val SharkMuted      = Color(0xFF778DA9) // Steel Blue Muted
val SharkRed        = Color(0xFFFF453A)
val SharkAmber      = Color(0xFFFFD60A)

// ─── Theme Aliases ─────────────────────────────────────────────────────────
val SharkBase           = SharkDeepOcean
val SharkSurface        = Color(0xFF0D1B2A)
val SharkSurfaceHigh    = Color(0xFF1B263B)
val SharkOverlay        = Color(0xFF415A77)
val SharkBorderSubtle   = Color(0xFF1B263B)
val SharkBorderMedium   = Color(0xFF415A77)
val SharkBorderStrong   = Color(0xFF778DA9)
val SharkGold           = SharkNavy
val SharkGoldGlow       = SharkNavy.copy(alpha = 0.12f)
val SharkTextPrimary    = Color.White
val SharkTextSecondary  = Color(0xFFE0E1DD)
val SharkTextMuted      = SharkMuted
val SharkPositive       = SharkNavy
val SharkNegative       = SharkRed
val SharkWarning        = SharkAmber
val SharkInfo           = Color(0xFF64FFDA)

// Redirect SharkGreen to SharkNavy for Ocean theme
val SharkGreen          = SharkNavy

// ─── Liquid Glass Modifier ─────────────────────────────────────────────────
fun Modifier.glassCard(cornerRadius: Float = 24f, alpha: Float = 0.08f): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .drawBehind {
        drawRoundRect(
            color = SharkNavy.copy(alpha = 0.04f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.dp.toPx())
        )
    }
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha + 0.04f),
                Color.White.copy(alpha = alpha)
            )
        )
    )

// ─── Data Classes ──────────────────────────────────────────────────────────
data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "Bills & Utilities",
    val note: String = "",
    @ServerTimestamp val createdAt: Date? = null
)

data class IncomeSource(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val frequency: String = "Monthly",
    val lastReceived: Date? = null
)

data class ExpenseCategory(val name: String, val icon: ImageVector, val color: Color)

val expenseCategories = listOf(
    ExpenseCategory("Income",            Icons.Default.TrendingUp,  SharkNavy),
    ExpenseCategory("Bills & Utilities", Icons.Default.Receipt,     SharkAmber),
    ExpenseCategory("Entertainment",     Icons.Default.MusicNote,   Color(0xFFBF5AF2)),
    ExpenseCategory("People I Owe",      Icons.Default.People,      SharkRed)
)

// ─── UI Components ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.25f), fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContainerColor  = Color.White.copy(alpha = 0.02f),
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor  = Color.Transparent,
                focusedTextColor        = Color.White,
                unfocusedTextColor      = if (enabled) Color.White else SharkMuted
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun FeatureTutorialOverlay(title: String, description: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBlack.copy(alpha = 0.85f))
            .clickable { onDismiss() }
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .glassCard(cornerRadius = 24f, alpha = 0.15f)
                .padding(28.dp)
        ) {
            Icon(Icons.Default.Lightbulb, null, tint = SharkNavy, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            Text("Tap anywhere to continue", color = SharkNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MiniStat(label: String, value: String, positive: Boolean) {
    Column {
        Text(label, color = SharkMuted, fontSize = 11.sp)
        Text(value, color = if (positive) SharkNavy else SharkRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SharkSectionHeader(text: String) {
    Text(text = text, color = SharkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
fun SharkCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SharkSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SharkBorderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp)
    ) { content() }
}

@Composable
fun SharkButtonSecondary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SharkRed),
        border = androidx.compose.foundation.BorderStroke(1.dp, SharkRed.copy(alpha = 0.4f))
    ) {
        Text(text, color = SharkRed, fontWeight = FontWeight.Bold)
    }
}

// ─── Utility Functions ─────────────────────────────────────────────────────
fun ordinal(n: Int): String {
    val suffix = when {
        n in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else        -> "th"
    }
    return "$n$suffix"
}

fun calculateEstimatedTax(taxableIncome: Double): Double {
    return when {
        taxableIncome <= 11000 -> taxableIncome * 0.10
        taxableIncome <= 44725 -> 1100 + (taxableIncome - 11000) * 0.12
        taxableIncome <= 95375 -> 5147 + (taxableIncome - 44725) * 0.22
        else                   -> 16290 + (taxableIncome - 95375) * 0.24
    }
}

fun getCategoryColor(name: String): Color = when (name) {
    "Income"            -> SharkNavy
    "Bills & Utilities" -> SharkAmber
    "Entertainment"     -> Color(0xFFBF5AF2)
    "People I Owe"      -> SharkRed
    "Housing"           -> SharkAmber
    "Utilities"         -> Color(0xFF64FFDA)
    "Subscriptions"     -> Color(0xFFBF5AF2)
    "Transport"         -> SharkNavy
    "Insurance"         -> SharkRed
    "Savings"           -> SharkNavy
    "Emergency"         -> SharkRed
    "Tech"              -> Color(0xFFBF5AF2)
    else                -> SharkMuted
}

// ─── Bill Data Class ───────────────────────────────────────────────────────
data class Bill(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val dayOfMonth: Int = 1,
    val recurrence: String = "Monthly",
    val category: String = "Housing",
    val isPaid: Boolean = false,
    val color: String = "#0A84FF",
    @ServerTimestamp val createdAt: Date? = null
)

data class BillCategory(val name: String, val icon: ImageVector, val color: Color)

val billCategories = listOf(
    BillCategory("Housing",       Icons.Default.Home,           Color(0xFFFFD60A)),
    BillCategory("Utilities",     Icons.Default.ElectricBolt,   Color(0xFF64FFDA)),
    BillCategory("Subscriptions", Icons.Default.Subscriptions,  Color(0xFFBF5AF2)),
    BillCategory("Transport",     Icons.Default.DirectionsCar,  SharkNavy),
    BillCategory("Insurance",     Icons.Default.Security,       SharkRed),
    BillCategory("Other",         Icons.Default.Receipt,        Color(0xFF778DA9))
)

val recurrenceOptions = listOf("Weekly", "Bi-weekly", "Monthly", "One-time")

// ─── Goal Data Class ───────────────────────────────────────────────────────
data class Goal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val category: String = "Savings",
    val deadline: String = "",
    val isCompleted: Boolean = false,
    val colorHex: String = "#0A84FF",
    @ServerTimestamp val createdAt: Date? = null
)

data class GoalCategory(val name: String, val icon: ImageVector, val color: Color)

val goalCategories = listOf(
    GoalCategory("Savings",       Icons.Default.Savings,       SharkNavy),
    GoalCategory("Emergency",     Icons.Default.Shield,         SharkRed),
    GoalCategory("Something New", Icons.Default.Flight,         Color(0xFF64FFDA)),
    GoalCategory("Tech",          Icons.Default.Devices,        Color(0xFFBF5AF2)),
    GoalCategory("Loan",          Icons.Default.School,         SharkAmber),
    GoalCategory("Other",         Icons.Default.Star,           Color(0xFF778DA9))
)