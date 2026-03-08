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

// ─── Color Palette ─────────────────────────────────────────────────────────
val SharkGreen      = Color(0xFF10b981)
val SharkDark       = Color(0xFF064e3b)
val SharkBlack      = Color(0xFF080c10)
val SharkCard       = Color(0xFF0f1923)
val SharkCardBorder = Color(0xFF1e3a2f)
val SharkMuted      = Color(0xFF4b6858)

// ─── Liquid Glass Modifier ─────────────────────────────────────────────────
fun Modifier.glassCard(cornerRadius: Float = 24f, alpha: Float = 0.08f): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .drawBehind {
        drawRoundRect(
            color = Color(0xFF10b981).copy(alpha = 0.06f),
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
    val frequency: String = "Monthly", // Weekly, Biweekly, Monthly, One-time
    val lastReceived: Date? = null
)

data class ExpenseCategory(val name: String, val icon: ImageVector, val color: Color)

val expenseCategories = listOf(
    ExpenseCategory("Income",            Icons.Default.TrendingUp,  Color(0xFF10b981)),
    ExpenseCategory("Bills & Utilities", Icons.Default.Receipt,     Color(0xFFf59e0b)),
    ExpenseCategory("Entertainment",     Icons.Default.MusicNote,   Color(0xFF8b5cf6)),
    ExpenseCategory("People I Owe",      Icons.Default.People,      Color(0xFFef4444))
)

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
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContainerColor = Color.White.copy(alpha = 0.02f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = if (enabled) Color.White else SharkMuted
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun FeatureTutorialOverlay(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() }
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .glassCard(cornerRadius = 24f, alpha = 0.15f)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Lightbulb, null, tint = SharkGreen, modifier = Modifier.size(48.dp))
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
            Text("Tap anywhere to continue", color = SharkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MiniStat(label: String, value: String, positive: Boolean) {
    Column {
        Text(label, color = SharkMuted, fontSize = 11.sp)
        Text(value, color = if (positive) SharkGreen else Color(0xFFef4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

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
        else -> 16290 + (taxableIncome - 95375) * 0.24
    }
}
