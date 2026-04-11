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
import com.example.sharkfin.ui.theme.*
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun VisualModelsScreen(
    expenses: List<Expense>,
    bills: List<Bill>,
    goals: List<Goal>,
    discoveryData: Map<String, Any>? = null
) {
    var showTutorial by remember { mutableStateOf(false) } // Set to false to avoid annoying repeat
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

    // ── DATA PREP (Optimized) ──────────────────────────────────────────────
    val metrics = remember(expenses, bills, discoveryData) {
        val wizardIncome = (discoveryData?.get("monthlyIncome") as? Double) ?: 0.0
        val wizardObligations = (discoveryData?.get("monthlyObligations") as? Double) ?: 0.0
        
        val totalIncome = expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }.coerceAtLeast(wizardIncome)
        val totalSpending = expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }
        val totalBills = bills.sumOf { it.amount }.coerceAtLeast(wizardObligations)

        val taxableIncome = (totalIncome - 12000.0).coerceAtLeast(0.0) 
        val estimatedTax = calculateEstimatedTax(taxableIncome)
        val netBalance = totalIncome - totalSpending - estimatedTax
        
        object {
            val income = totalIncome
            val spending = totalSpending
            val bills = totalBills
            val tax = estimatedTax
            val net = netBalance
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SharkBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Living Dashboard", style = SharkTypography.headlineLarge, color = SharkWhite)
            Text("Financial Survival Engine v1.2", style = SharkTypography.labelMedium, color = SharkSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 1. Benchmark Battle ─────────────────────
            Box {
                BenchmarkBattleCard(expenses, metrics.income, metrics.net)
                
                if (showBenchmarkCoach) {
                    CoachMark(
                        text = "Your savings rate vs Wall Street benchmarks.",
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 40.dp),
                        onDismiss = { showBenchmarkCoach = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

            // ── 2. HEALTH SCORE ──────────────────────────────────────────────
            HealthScoreCard(expenses, bills, goals)

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. DONUT WITH DRILL-DOWN ──────────────────────────────────────
            SharkSectionHeader("SPENDING DISTRIBUTION")
            Spacer(modifier = Modifier.height(16.dp))
            Box {
                DonutChartWithDrilldown(expenses, selectedCategory) { selectedCategory = it }
                
                if (showDonutCoach) {
                    CoachMark(
                        text = "Tap to drill down into categories.",
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-20).dp, y = 30.dp),
                        onDismiss = { showDonutCoach = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. FOUR-BAR COMPARISON ───────────────────────────
            SharkSectionHeader("CASH FLOW PILLARS")
            Spacer(modifier = Modifier.height(16.dp))
            FlowBarChart(metrics.income, metrics.spending, metrics.bills, metrics.tax)

            Spacer(modifier = Modifier.height(120.dp))
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Financial Survival Engine",
                description = "Real-time analytics comparing your growth to market benchmarks.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
fun BenchmarkBattleCard(expenses: List<Expense>, income: Double, net: Double) {
    val userGrowth = remember(income, net) { if (income > 0) (net / income) * 100 else 0.0 }
    val sp500Growth = 8.5 
    
    val userPoints = remember(expenses) {
        if (expenses.size < 2) listOf(0.4f, 0.45f, 0.42f, 0.48f, 0.5f)
        else {
            val sorted = expenses.takeLast(10).reversed()
            val max = sorted.maxOf { it.amount }.coerceAtLeast(1.0)
            sorted.map { (it.amount / max).toFloat().coerceIn(0.1f, 1.0f) }
        }
    }
    
    SharkCard {
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("BENCHMARK BATTLE", style = SharkTypography.labelSmall, color = SharkSecondary)
                if (userGrowth > sp500Growth) {
                    Text("BEATING THE WHALES 🐋", style = SharkTypography.labelSmall, color = SharkGold)
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("YOU", style = SharkTypography.labelMedium, color = SharkGold)
                    Text("${String.format(Locale.US, "%.1f", userGrowth)}%", style = SharkTypography.headlineLarge, color = SharkWhite)
                }
                Column {
                    Text("S&P 500", style = SharkTypography.labelMedium, color = SharkSecondary)
                    Text("${sp500Growth}%", style = SharkTypography.headlineLarge, color = SharkWhite)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Box(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spPoints = listOf(0.2f, 0.3f, 0.35f, 0.5f, 0.6f, 0.8f)
                    drawTrendLine(spPoints, SharkSecondary.copy(alpha = 0.3f))
                    drawTrendLine(userPoints, SharkGold)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text("Optimizing capital efficiency in real-time.", style = SharkTypography.labelSmall, color = SharkSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
    drawPath(path = path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
fun HealthScoreCard(expenses: List<Expense>, bills: List<Bill>, goals: List<Goal>) {
    val scoreMetrics = remember(expenses, bills, goals) {
        val totalIncome = expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }
        val totalSpend = expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }

        val savingsRate = if (totalIncome > 0) ((totalIncome - totalSpend) / totalIncome).coerceIn(0.0, 1.0) else 0.0
        val goalsOnPace = if (goals.isNotEmpty()) {
            val activeGoals = goals.filter { !it.isCompleted }
            if (activeGoals.isEmpty()) 1.0
            else (activeGoals.sumOf { it.savedAmount } / activeGoals.sumOf { it.targetAmount }.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
        } else 1.0
        
        val billBurden = if (totalIncome > 0) (1.0 - (bills.sumOf { it.amount } / totalIncome)).coerceIn(0.0, 1.0) else 1.0
        ((savingsRate * 40) + (goalsOnPace * 40) + (billBurden * 20)).toInt().coerceIn(0, 100)
    }

    val scoreColor = when {
        scoreMetrics > 75 -> SharkPositive
        scoreMetrics > 50 -> SharkAmber
        else -> SharkRed
    }

    SharkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                CircularProgressIndicator(
                    progress = { scoreMetrics / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    strokeWidth = 6.dp,
                    trackColor = SharkWhite.copy(alpha = 0.05f)
                )
                Text("$scoreMetrics", style = SharkTypography.headlineMedium, color = SharkWhite)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text("Health Score", style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = SharkWhite)
                Text(
                    when {
                        scoreMetrics > 75 -> "Excellent stability."
                        scoreMetrics > 50 -> "Balanced. Minor leaks found."
                        else -> "Critical. Action required."
                    },
                    style = SharkTypography.labelMedium,
                    color = SharkSecondary
                )
            }
        }
    }
}

@Composable
fun DonutChartWithDrilldown(expenses: List<Expense>, selectedCategory: String?, onCategorySelect: (String?) -> Unit) {
    val categories = remember(expenses) {
        expenses.filter { !SharkIncomeCategories.contains(it.category) }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
    }
    val total = remember(categories) { categories.values.sum().toFloat() }

    SharkCard {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    var startAngle = -90f
                    categories.forEach { (name, amount) ->
                        val sweep = (amount.toFloat() / total.coerceAtLeast(1f)) * 360f
                        val color = getCategoryColor(name)
                        drawArc(
                            color = if (selectedCategory == null || selectedCategory == name) color else color.copy(alpha = 0.2f),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = if (selectedCategory == name) 36f else 24f, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
                if (selectedCategory != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(selectedCategory, style = SharkTypography.labelSmall, color = SharkSecondary)
                        Text(String.format(Locale.US, "$%.0f", categories[selectedCategory] ?: 0.0), style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = SharkWhite)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL", style = SharkTypography.labelSmall, color = SharkSecondary)
                        Text(String.format(Locale.US, "$%.0f", total), style = SharkTypography.headlineMedium, color = SharkWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                categories.keys.forEach { name ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selectedCategory == name) 12.dp else 8.dp)
                            .background(getCategoryColor(name), CircleShape)
                            .clickable { onCategorySelect(if (selectedCategory == name) null else name) }
                    )
                }
            }
        }
    }
}

@Composable
fun FlowBarChart(income: Double, spending: Double, bills: Double, taxes: Double) {
    val maxVal = maxOf(income, spending + bills + taxes, 1.0)

    SharkCard {
        Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            Bar("Income", income, maxVal, SharkGold)
            Bar("Spend", spending, maxVal, SharkPurple)
            Bar("Bills", bills, maxVal, SharkAmber)
            Bar("Taxes", taxes, maxVal, SharkRed)
        }
    }
}

@Composable
fun Bar(label: String, value: Double, max: Double, color: Color) {
    val heightFactor = (value / max).toFloat().coerceIn(0.01f, 1.0f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(String.format(Locale.US, "$%.0f", value), style = SharkTypography.labelSmall, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(28.dp).fillMaxHeight(heightFactor).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(color))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = SharkTypography.labelSmall, color = SharkSecondary)
    }
}
