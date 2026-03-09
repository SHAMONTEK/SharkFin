package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharkfin.ui.theme.*
import java.util.*

@Composable
fun SnapshotScreen(
    expenses: List<Expense>,
    bills: List<Bill>,
    goals: List<Goal>,
    uid: String,
    onFeatureClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        SmartGreetingZone()
        Spacer(Modifier.height(24.dp))
        PulseCircleZone(expenses, bills)
        Spacer(Modifier.height(32.dp))
        StatPillsZone(expenses)
        Spacer(Modifier.height(28.dp))
        QuickAccessStrip(onFeatureClick)
        Spacer(Modifier.height(28.dp))
        if (expenses.isNotEmpty()) {
            TransactionFlowZone(expenses.take(5))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SmartGreetingZone() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    Column {
        Text(greeting, style = SharkTypography.bodyMedium, color = SharkTextMuted)
        Text("Your Pulse is steady.", style = SharkTypography.headlineLarge, color = SharkTextPrimary)
    }
}

@Composable
fun PulseCircleZone(expenses: List<Expense>, bills: List<Bill>) {
    val totalIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalSpent = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val balance = totalIncome - totalSpent
    val unpaidBills = bills.filter { !it.isPaid }.sumOf { it.amount }
    
    // Pulse ratio: spending vs income
    val ratio = if (totalIncome > 0) (totalSpent / totalIncome).toFloat().coerceIn(0f, 1f) else 0.5f
    val color = when {
        ratio < 0.4f -> SharkPositive
        ratio < 0.7f -> SharkGold
        else -> SharkNegative
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Outer Glow/Pulse
        Canvas(modifier = Modifier.size(240.dp)) {
            drawCircle(
                color = color.copy(alpha = 0.05f),
                radius = size.minDimension / 2
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * ratio,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = SharkBorderSubtle,
                startAngle = -90f + (360f * ratio),
                sweepAngle = 360f * (1f - ratio),
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NET BALANCE", style = SharkTypography.labelMedium, color = SharkTextMuted)
            Text(
                String.format(Locale.US, "$%.2f", balance),
                style = SharkTypography.displayLarge.copy(fontSize = 32.sp),
                color = SharkTextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                color = SharkGoldGlow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    String.format(Locale.US, "$%.0f in bills due", unpaidBills),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = SharkTypography.labelMedium,
                    color = SharkGold
                )
            }
        }
    }
}

@Composable
fun StatPillsZone(expenses: List<Expense>) {
    val totalIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalSpent = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatPill(
            label = "INCOME",
            amount = totalIncome,
            color = SharkPositive,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "SPENDING",
            amount = totalSpent,
            color = SharkNegative,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(label: String, amount: Double, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(SharkSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SharkBorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(label, style = SharkTypography.labelMedium, color = SharkTextMuted)
        Text(
            String.format(Locale.US, "$%.0f", amount),
            style = SharkTypography.headlineMedium,
            color = color
        )
    }
}

@Composable
fun QuickAccessStrip(onFeatureClick: (String) -> Unit) {
    val items = listOf(
        Triple("Bills", Icons.Default.Receipt, SharkWarning),
        Triple("Goals", Icons.Default.Flag, SharkInfo),
        Triple("Visuals", Icons.Default.BarChart, SharkGold),
        Triple("AI Coach", Icons.Default.AutoAwesome, SharkGold)
    )
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        items.forEach { (name, icon, color) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFeatureClick(name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SharkSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(name, style = SharkTypography.labelMedium, color = SharkTextSecondary)
            }
        }
    }
}

@Composable
fun TransactionFlowZone(recent: List<Expense>) {
    Column {
        SharkSectionHeader("Recent Activity")
        Spacer(Modifier.height(16.dp))
        recent.forEachIndexed { index, expense ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(expense.category).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        expense.category.take(1),
                        color = getCategoryColor(expense.category),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, style = SharkTypography.bodyLarge, color = SharkTextPrimary)
                    Text(expense.category, style = SharkTypography.labelMedium, color = SharkTextMuted)
                }
                Text(
                    String.format(Locale.US, "$%.2f", expense.amount),
                    style = SharkTypography.bodyLarge.copy(fontFamily = DMMonoFontFamily),
                    color = if (expense.category == "Income") SharkPositive else SharkNegative
                )
            }
            if (index < recent.size - 1) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            }
        }
    }
}
