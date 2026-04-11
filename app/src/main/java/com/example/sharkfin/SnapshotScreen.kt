package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharkfin.ui.theme.*
import java.util.*

@Composable
fun SnapshotScreen(
    expenses: List<Expense>,
    bills: List<Bill>,
    uid: String,
    onFeatureClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        SmartGreetingZone()
        Spacer(Modifier.height(32.dp))
        PulseCircleZone(expenses, bills)
        Spacer(Modifier.height(40.dp))
        StatPillsZone(expenses)
        Spacer(Modifier.height(32.dp))
        SharkSectionHeader("QUICK ACCESS")
        Spacer(Modifier.height(16.dp))
        QuickAccessStrip(onFeatureClick)
        Spacer(Modifier.height(32.dp))
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
        Text(greeting.uppercase(), style = SharkTypography.labelSmall, color = SharkGold.copy(0.7f))
        Text("Your Pulse is steady.", style = SharkTypography.headlineLarge, color = SharkLabel)
    }
}

@Composable
fun PulseCircleZone(expenses: List<Expense>, bills: List<Bill>) {
    val metrics = remember(expenses, bills) {
        val incomeCategories = SharkIncomeCategories
        val totalIncome = expenses.filter { it.category in incomeCategories }.sumOf { it.amount }
        val totalSpent = expenses.filter { it.category !in incomeCategories }.sumOf { it.amount }
        val balance = totalIncome - totalSpent
        val unpaidBills = bills.filter { !it.isPaid }.sumOf { it.amount }
        val ratio = if (totalIncome > 0) (totalSpent / totalIncome).toFloat().coerceIn(0f, 1f) else 0.5f
        
        object {
            val balance = balance
            val unpaidBills = unpaidBills
            val ratio = ratio
            val color = when {
                ratio < 0.4f -> SharkPositive
                ratio < 0.7f -> SharkGold
                else -> SharkNegative
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            drawCircle(
                color = metrics.color.copy(alpha = 0.03f),
                radius = size.minDimension / 2
            )
            drawArc(
                color = metrics.color,
                startAngle = -90f,
                sweepAngle = 360f * metrics.ratio,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = SharkCardBorder,
                startAngle = -90f + (360f * metrics.ratio),
                sweepAngle = 360f * (1f - metrics.ratio),
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NET BALANCE", style = SharkTypography.labelSmall, color = SharkSecondary)
            Text(
                String.format(Locale.US, "$%,.2f", metrics.balance),
                style = SharkTypography.headlineLarge.copy(fontSize = 32.sp),
                color = SharkLabel
            )
            if (metrics.unpaidBills > 0) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = SharkGold.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, SharkGold.copy(alpha = 0.2f))
                ) {
                    Text(
                        String.format(Locale.US, "$%,.0f in bills due", metrics.unpaidBills),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = SharkTypography.labelSmall,
                        color = SharkGold
                    )
                }
            }
        }
    }
}

@Composable
fun StatPillsZone(expenses: List<Expense>) {
    val totalIncome = remember(expenses) { expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount } }
    val totalSpent = remember(expenses) { expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount } }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    SharkCard(modifier = modifier) {
        Text(label, style = SharkTypography.labelSmall, color = SharkSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            String.format(Locale.US, "$%,.0f", amount),
            style = SharkTypography.headlineMedium,
            color = color
        )
    }
}

@Composable
fun QuickAccessStrip(onFeatureClick: (String) -> Unit) {
    val items = listOf(
        Triple("Bills", Icons.Default.Receipt, SharkAmber),
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
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SharkSurfaceHigh)
                        .border(0.5.dp, SharkCardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(name, style = SharkTypography.labelSmall, color = SharkSecondary)
            }
        }
    }
}

@Composable
fun TransactionFlowZone(recent: List<Expense>) {
    Column {
        SharkSectionHeader("RECENT ACTIVITY")
        Spacer(Modifier.height(16.dp))
        recent.forEachIndexed { index, expense ->
            val color = getCategoryColor(expense.category)
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
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        expense.category.take(1),
                        color = color,
                        style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, style = SharkTypography.bodyLarge, color = SharkLabel)
                    Text(expense.category.uppercase(), style = SharkTypography.labelSmall, color = SharkTextMuted)
                }
                Text(
                    String.format(Locale.US, "$%,.2f", expense.amount),
                    style = SharkTypography.bodyLarge.copy(fontFamily = DMMonoFontFamily),
                    color = if (SharkIncomeCategories.contains(expense.category)) SharkPositive else SharkLabel
                )
            }
            if (index < recent.size - 1) {
                HorizontalDivider(color = SharkCardBorder, thickness = 0.5.dp)
            }
        }
    }
}
