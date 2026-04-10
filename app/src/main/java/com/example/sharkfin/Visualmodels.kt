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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun VisualModelsScreen(
    expenses: List<Expense>,
    bills: List<Bill>,
    goals: List<Goal>,
    discoveryData: Map<String, Any>? = null
) {
    var showTutorial by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Coach marks state
    var showBenchmarkCoach by remember { mutableStateOf(false) }
    var showDonutCoach by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1200)
        showBenchmarkCoach = true
        delay(2500)
        showDonutCoach = true
    }

    // ── DATA PREP ──────────────────────────────────────────────────────────
    val wizardIncome = (discoveryData?.get("monthlyIncome") as? Double) ?: 0.0
    val wizardObligations = (discoveryData?.get("monthlyObligations") as? Double) ?: 0.0
    
    val totalIncome = expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }.coerceAtLeast(wizardIncome)
    val totalSpending = expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }
    val totalBills = bills.sumOf { it.amount }.coerceAtLeast(wizardObligations)

    val taxableIncome = (totalIncome - 12000.0).coerceAtLeast(0.0) // simplified deduction
    val estimatedTax = calculateEstimatedTax(taxableIncome)
    val netBalance = totalIncome - totalSpending - estimatedTax

    Box(modifier = Modifier.fillMaxSize().background(SharkBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Living Dashboard", color = SharkWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Financial Survival Engine v1.0", color = SharkSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 1. Benchmark Battle (Student vs S&P 500) ─────────────────────
            Box {
                BenchmarkBattleCard(expenses, totalIncome, netBalance)
                
                if (showBenchmarkCoach) {
                    CoachMark(
                        text = "We compare your savings rate to the stock market's growth.",
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 40.dp),
                        onDismiss = { showBenchmarkCoach = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

            // ── 2. HEALTH SCORE ──────────────────────────────────────────────
            HealthScoreCard(expenses, bills, goals)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. DONUT WITH DRILL-DOWN ──────────────────────────────────────
            Text("Spending Distribution", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Box {
                DonutChartWithDrilldown(expenses, selectedCategory) { selectedCategory = it }
                
                if (showDonutCoach) {
                    CoachMark(
                        text = "Tap categories to see where your money is actually going.",
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-20).dp, y = 30.dp),
                        onDismiss = { showDonutCoach = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. FOUR-BAR COMPARISON (WITH TAXES) ───────────────────────────
            Text("Cash Flow Pillars", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            FlowBarChart(totalIncome, totalSpending, totalBills, estimatedTax)

            Spacer(modifier = Modifier.height(120.dp))
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Financial Survival Engine",
                description = "We don't just track pennies. We track your freedom. Compare your growth against Wall Street and watch your Passive Snowball grow.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
fun BenchmarkBattleCard(expenses: List<Expense>, income: Double, net: Double) {
    val userGrowth = if (income > 0) (net / income) * 100 else 0.0
    val sp500Growth = 8.5 // Simulated YTD S&P 500
    
    // Generate trend points based on real data, otherwise show a flat line
    val userPoints = remember(expenses) {
        if (expenses.size < 2) listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        else {
            // Sort by date and map to 0f-1f range based on amount
            val sorted = expenses.takeLast(10).reversed()
            val max = sorted.maxOf { it.amount }.coerceAtLeast(1.0)
            sorted.map { (it.amount / max).toFloat().coerceIn(0.1f, 1.0f) }
        }
    }
    
    Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 1.0f).padding(20.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("BENCHMARK BATTLE", color = SharkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (userGrowth > sp500Growth) {
                    Text("BEATING THE WHALES 🐋", color = SharkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("YOU", color = SharkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format(Locale.US, "%.1f", userGrowth)}%", color = SharkWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("S&P 500", color = SharkSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${sp500Growth}%", color = SharkWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spPoints = listOf(0.2f, 0.3f, 0.35f, 0.5f, 0.6f, 0.8f)
                    drawTrendLine(spPoints, SharkSecondary.copy(alpha = 0.5f))
                    drawTrendLine(userPoints, SharkGold)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text("Democratizing the 'Rich Man\\'s' metric.", color = SharkSecondary, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
fun HealthScoreCard(expenses: List<Expense>, bills: List<Bill>, goals: List<Goal>) {
    val totalIncome = expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }
    val totalSpend = expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }

    val rawSavingsRate = if (totalIncome > 0 && totalIncome.isFinite()) ((totalIncome - totalSpend) / totalIncome) else 0.0
    val savingsRate = if (rawSavingsRate.isNaN()) 0.0 else rawSavingsRate.coerceIn(0.0, 1.0)
    
    val goalsOnPace = if (goals.isNotEmpty()) {
        val activeGoals = goals.filter { !it.isCompleted }
        if (activeGoals.isEmpty()) 1.0
        else {
            val totalTarget = activeGoals.sumOf { it.targetAmount }
            val totalSaved = activeGoals.sumOf { it.savedAmount }
            val rawPace = if (totalTarget > 0 && totalTarget.isFinite()) (totalSaved / totalTarget) else 1.0
            if (rawPace.isNaN()) 1.0 else rawPace.coerceIn(0.0, 1.0)
        }
    } else 1.0
    
    val rawBillBurden = if (totalIncome > 0 && totalIncome.isFinite()) (1.0 - (bills.sumOf { it.amount } / totalIncome)) else 1.0
    val billBurden = if (rawBillBurden.isNaN()) 1.0 else rawBillBurden.coerceIn(0.0, 1.0)

    val score = ((savingsRate * 40) + (goalsOnPace * 40) + (billBurden * 20)).toInt().coerceIn(0, 100)
    val scoreColor = when {
        score > 75 -> SharkGold
        score > 50 -> SharkAmber
        else -> SharkRed
    }

    Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 1.0f).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    strokeWidth = 8.dp,
                    trackColor = SharkWhite.copy(alpha = 0.1f)
                )
                Text("$score", color = SharkWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text("Financial Health", color = SharkWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        score > 75 -> "Excellent. Your growth is optimized."
                        score > 50 -> "Good. Focus on reducing fixed bills."
                        else -> "Critical. High spending detected."
                    },
                    color = SharkSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DonutChartWithDrilldown(expenses: List<Expense>, selectedCategory: String?, onCategorySelect: (String?) -> Unit) {
    val spendingExpenses = expenses.filter { !SharkIncomeCategories.contains(it.category) }
    val categories = spendingExpenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    val total = categories.values.sum().toFloat()

    Column(modifier = Modifier.fillMaxWidth().glassCard(alpha = 1.0f).padding(20.dp).animateContentSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                var startAngle = -90f
                categories.forEach { (name, amount) ->
                    val rawSweep = (amount.toFloat() / (if(total == 0f || !total.isFinite()) 1f else total)) * 360f
                    val sweep = if (rawSweep.isNaN() || !rawSweep.isFinite()) 0f else rawSweep
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
                    Text(selectedCategory, color = SharkWhite, fontSize = 12.sp)
                    val amount = categories[selectedCategory] ?: 0.0
                    Text(String.format(Locale.US, "$%.0f", amount), color = SharkWhite, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Spend", color = SharkSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "$%.0f", total), color = SharkWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
fun FlowBarChart(income: Double, spending: Double, bills: Double, taxes: Double) {
    val maxVal = maxOf(income, spending + bills + taxes, 1.0)

    Box(modifier = Modifier.fillMaxWidth().glassCard(alpha = 1.0f).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            Bar("Income", income, maxVal, SharkGold)
            Bar("Spend", spending, maxVal, SharkPurple)
            Bar("Bills", bills, maxVal, SharkAmber)
            Bar("Taxes", taxes, maxVal, SharkRed)
        }
    }
}

@Composable
fun Bar(label: String, value: Double, max: Double, color: Color) {
    val rawFactor = (value / max).toFloat()
    val heightFactor = if (rawFactor.isNaN() || !rawFactor.isFinite()) 0.01f else rawFactor.coerceIn(0.01f, 1.0f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(String.format(Locale.US, "$%.0f", value), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(30.dp).fillMaxHeight(heightFactor).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(color))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = SharkSecondary, fontSize = 10.sp)
    }
}
