package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharkfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityScreen(expenses: List<Expense>, bills: List<Bill>) {
    val totalIn = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalOut = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val net = totalIn - totalOut

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBase)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activity", style = SharkTypography.headlineLarge, color = SharkTextPrimary)
            IconButton(onClick = { /* Filter */ }) {
                Icon(Icons.Default.FilterList, null, tint = SharkTextPrimary)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryPill("Total In", totalIn, SharkPositive, Modifier.weight(1f))
            SummaryPill("Total Out", totalOut, SharkNegative, Modifier.weight(1f))
            SummaryPill("Net", net, if (net >= 0) SharkPositive else SharkNegative, Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))
        SharkSectionHeader("THIS MONTH")
        Spacer(Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            EmptyActivityState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(expenses) { index, expense ->
                    val delay = index * 40
                    val state = remember { MutableTransitionState(false) }.apply { targetState = true }
                    
                    AnimatedVisibility(
                        visibleState = state,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = delay)) +
                                slideInVertically(animationSpec = tween(500, delayMillis = delay)) { it / 2 }
                    ) {
                        TransactionRow(expense)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryPill(label: String, amount: Double, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(SharkSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = SharkTypography.labelMedium, color = SharkTextMuted, fontSize = 10.sp)
        Text(
            String.format(Locale.US, "$%.0f", amount),
            style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun TransactionRow(expense: Expense) {
    val categoryColor = getCategoryColor(expense.category)
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    val dateStr = expense.createdAt?.let { dateFormat.format(it) } ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = expense.category.take(1).uppercase(),
                style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = categoryColor
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = SharkTextPrimary
            )
            Text(
                text = dateStr,
                style = SharkTypography.labelMedium,
                color = SharkTextMuted,
                fontSize = 10.sp
            )
        }

        Text(
            text = String.format(Locale.US, (if (expense.category == "Income") "+" else "-") + "$%.2f", expense.amount),
            style = SharkTypography.bodyLarge.copy(fontFamily = DMMonoFontFamily),
            color = if (expense.category == "Income") SharkPositive else SharkNegative
        )
    }
}

@Composable
fun EmptyActivityState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ReceiptLong,
                null,
                modifier = Modifier.size(48.dp),
                tint = SharkTextMuted
            )
            Spacer(Modifier.height(16.dp))
            Text("No transactions yet", style = SharkTypography.headlineMedium, color = SharkTextPrimary)
            Text(
                "Add one from the Snapshot tab",
                style = SharkTypography.bodyMedium,
                color = SharkTextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
