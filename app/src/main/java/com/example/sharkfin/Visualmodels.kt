package com.example.sharkfin

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import java.util.*

@Composable
fun VisualModelsScreen(
    expenses: List<Expense>,
    bills: List<Bill>,
    goals: List<Goal>
) {
    var showTutorial by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // ── DATA PREP ──────────────────────────────────────────────────────────
    val totalIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalSpending = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val totalBills = bills.sumOf { it.amount }

    val taxableIncome = (totalIncome - 12000.0).coerceAtLeast(0.0) // simplified deduction
    val estimatedTax = calculateEstimatedTax(taxableIncome)
    val netBalance = totalIncome - totalSpending - estimatedTax

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Living Dashboard", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Reactive intelligence powered by your data", color = SharkMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 1. HEALTH SCORE ──────────────────────────────────────────────
            HealthScoreCard(expenses, bills, goals)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. DONUT WITH DRILL-DOWN ──────────────────────────────────────
            Text("Spending Distribution", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            DonutChartWithDrilldown(expenses, selectedCategory) { selectedCategory = it }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. FOUR-BAR COMPARISON (WITH TAXES) ───────────────────────────
            Text("Cash Flow Pillars", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            FlowBarChart(totalIncome, totalSpending, totalBills, estimatedTax)

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. TRIPLE TREND LINE ──────────────────────────────────────────
            Text("Growth Trends", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            TripleTrendChart(expenses)

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5. PROJECTIONS ────────────────────────────────────────────────
            Text("Future Forecast", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            ForecastSection(netBalance, totalSpending, totalIncome)

            Spacer(modifier = Modifier.height(120.dp))
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Living Intelligence",
                description = "This isn't just a chart—it's a story. Every expense you add updates your Health Score and future projections in real-time.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
fun HealthScoreCard(expenses: List<Expense>, bills: List<Bill>, goals: List<Goal>) {
    val totalIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalSpend = expenses.filter { it.category != "Income" }.sumOf { it.amount }

    // Logic: Savings Rate (40%) + Goal Pace (40%) + Bill Burden (20%)
    val savingsRate = if (totalIncome > 0) ((totalIncome - totalSpend) / totalIncome).coerceIn(0.0, 1.0) else 0.0
    val goalsOnPace = if (goals.isNotEmpty()) {
        val activeGoals = goals.filter { !it.isCompleted }
        if (activeGoals.isEmpty()) 1.0
        else {
            // Check if user has saved at least 10% of total target of active goals
            val totalTarget = activeGoals.sumOf { it.targetAmount }
            val totalSaved = activeGoals.sumOf { it.savedAmount }
            if (totalTarget > 0) (totalSaved / totalTarget).coerceIn(0.0, 1.0) else 1.0
        }
    } else 1.0
    val billBurden = if (totalIncome > 0) (1.0 - (bills.sumOf { it.amount } / totalIncome)).coerceIn(0.0, 1.0) else 1.0

    val score = ((savingsRate * 40) + (goalsOnPace * 40) + (billBurden * 20)).toInt().coerceIn(0, 100)
    val scoreColor = when {
        score > 75 -> SharkGreen
        score > 50 -> Color(0xFFf59e0b)
        else -> Color(0xFFef4444)
    }

    Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 0.1f).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text("$score", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text("Financial Health", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        score > 75 -> "Excellent. Your growth is optimized."
                        score > 50 -> "Good. Focus on reducing fixed bills."
                        else -> "Critical. High spending detected."
                    },
                    color = SharkMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DonutChartWithDrilldown(expenses: List<Expense>, selectedCategory: String?, onCategorySelect: (String?) -> Unit) {
    val spendingExpenses = expenses.filter { it.category != "Income" }
    val categories = spendingExpenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    val total = categories.values.sum().toFloat()

    Column(modifier = Modifier.fillMaxWidth().glassCard(alpha = 0.05f).padding(20.dp).animateContentSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                var startAngle = -90f
                categories.forEach { (name, amount) ->
                    val sweep = (amount.toFloat() / (if(total == 0f) 1f else total)) * 360f
                    val color = getCategoryColor(name)
                    drawArc(
                        color = if (selectedCategory == null || selectedCategory == name) color else color.copy(alpha = 0.2f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = if (selectedCategory == name) 45f else 30f, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            if (selectedCategory != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectedCategory, color = Color.White, fontSize = 12.sp)
                    val amount = categories[selectedCategory] ?: 0.0
                    Text("$${String.format("%.0f", amount)}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Spend", color = SharkMuted, fontSize = 11.sp)
                    Text("$${String.format("%.0f", total)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            categories.keys.forEach { name ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(if (selectedCategory == name) 14.dp else 10.dp)
                        .background(getCategoryColor(name), CircleShape)
                        .clickable { if (selectedCategory == name) onCategorySelect(null) else onCategorySelect(name) }
                )
            }
        }

        if (selectedCategory != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Transactions: $selectedCategory", color = SharkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val filtered = spendingExpenses.filter { it.category == selectedCategory }
            filtered.take(3).forEach { exp ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(exp.title, color = Color.White, fontSize = 13.sp)
                    Text("$${String.format("%.2f", exp.amount)}", color = SharkMuted, fontSize = 13.sp)
                }
            }
            if (filtered.size > 3) {
                Text("...and ${filtered.size - 3} more", color = SharkMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun FlowBarChart(income: Double, spending: Double, bills: Double, taxes: Double) {
    val maxVal = maxOf(income, spending + bills + taxes, 1.0)

    Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 0.05f).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            Bar("Income", income, maxVal, SharkGreen)
            Bar("Spend", spending, maxVal, Color(0xFF8b5cf6))
            Bar("Bills", bills, maxVal, Color(0xFFf59e0b))
            Bar("Taxes", taxes, maxVal, Color(0xFFef4444))
        }
    }
}

@Composable
fun Bar(label: String, value: Double, max: Double, color: Color) {
    val heightFactor = (value / max).toFloat().coerceIn(0.01f, 1.0f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$${String.format("%.0f", value)}", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(30.dp).fillMaxHeight(heightFactor).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(color))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = SharkMuted, fontSize = 10.sp)
    }
}

@Composable
fun TripleTrendChart(expenses: List<Expense>) {
    // In a real app, we would group expenses by day/week. Here we mock data points.
    val incomePoints = listOf(0.4f, 0.5f, 0.6f, 0.55f, 0.8f, 0.9f, 1.0f)
    val spendPoints = listOf(0.2f, 0.3f, 0.4f, 0.35f, 0.5f, 0.45f, 0.6f)
    val balancePoints = incomePoints.zip(spendPoints) { i, s -> i - s }

    Box(modifier = Modifier.fillMaxWidth().height(200.dp).glassCard(alpha = 0.05f).padding(16.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                LegendItem("Income", SharkGreen)
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem("Spend", Color(0xFFef4444))
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem("Net", Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrendLine(incomePoints, SharkGreen)
                drawTrendLine(spendPoints, Color(0xFFef4444))
                drawTrendLine(balancePoints, Color.White)
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrendLine(points: List<Float>, color: Color) {
    if (points.size < 2) return
    val path = Path()
    val stepX = size.width / (points.size - 1)
    points.forEachIndexed { i, p ->
        val x = i * stepX
        val y = size.height - (p * size.height)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = SharkMuted, fontSize = 9.sp)
    }
}

@Composable
fun ForecastSection(currentNet: Double, avgSpend: Double, income: Double) {
    val dailyNet = (income - avgSpend) / 30.0

    Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 0.1f).padding(20.dp)) {
        Column {
            ProjectionRow("30 Days", dailyNet * 30)
            ProjectionRow("60 Days", dailyNet * 60)
            ProjectionRow("90 Days", dailyNet * 90)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Based on current velocity of $${String.format("%.2f", dailyNet)}/day",
                color = SharkGreen,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProjectionRow(days: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(days, color = Color.White, fontSize = 14.sp)
        Text(
            (if(amount>=0) "+" else "") + "$${String.format("%,.2f", amount)}",
            color = if(amount >= 0) SharkGreen else Color(0xFFef4444),
            fontWeight = FontWeight.Bold
        )
    }
}