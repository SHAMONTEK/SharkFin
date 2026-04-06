package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.sin

@Composable
fun AiPredictionScreen(
    expenses: List<Expense>,
    incomeSources: List<IncomeSource>,
    bills: List<Bill>
) {
    var selectedScenario by remember { mutableStateOf("Baseline") }
    var showInsight by remember { mutableStateOf(false) }

    val totalIncome = if (incomeSources.isNotEmpty()) incomeSources.sumOf { it.amount } else expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val avgMonthlySpend = expenses.filter { it.category != "Income" }.sumOf { it.amount }.let { if (it > 0) it / 3.0 else 1200.0 } // Mock 3 month avg
    val totalBills = bills.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize().background(SharkBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SharkNavy.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = SharkNavy, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("AI Cash Flow Oracle", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Predicting your financial destiny", color = SharkMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Scenario Selector ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScenarioChip("Baseline", selectedScenario == "Baseline") { selectedScenario = "Baseline" }
                ScenarioChip("Bull Market", selectedScenario == "Bull Market") { selectedScenario = "Bull Market" }
                ScenarioChip("Recession", selectedScenario == "Recession") { selectedScenario = "Recession" }
                ScenarioChip("Side Hustle +20%", selectedScenario == "Side Hustle +20%") { selectedScenario = "Side Hustle +20%" }
            }

            Spacer(Modifier.height(24.dp))

            // ── Prediction Graph ───────────────────────────────────────────
            PredictionGraph(totalIncome, avgMonthlySpend, selectedScenario)

            Spacer(Modifier.height(32.dp))

            // ── AI Insights ────────────────────────────────────────────────
            AiInsightCard(selectedScenario)

            Spacer(Modifier.height(32.dp))

            // ── Future Milestones ──────────────────────────────────────────
            Text("Predicted Milestones", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            MilestoneItem("Emergency Fund Full", "3 months from now", Icons.Default.Shield, SharkGreen)
            MilestoneItem("Debt Free Date", "Oct 2026", Icons.Default.EventAvailable, SharkAmber)
            MilestoneItem("Financial Independence", "14 years from now", Icons.Default.Celebration, SharkNavy)

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun ScenarioChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SharkNavy else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PredictionGraph(income: Double, spend: Double, scenario: String) {
    val multiplier = when(scenario) {
        "Bull Market" -> 1.15
        "Recession" -> 0.85
        "Side Hustle +20%" -> 1.20
        else -> 1.0
    }
    
    val netMonthly = (income * multiplier) - spend
    val points = remember(scenario) {
        List(12) { i -> (netMonthly * (i + 1)).toFloat() }
    }
    val maxVal = points.maxOrNull() ?: 1000f
    val minVal = points.minOrNull() ?: -1000f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .glassCard(alpha = 0.05f)
            .padding(20.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / (points.size - 1)
            val path = Path()
            
            points.forEachIndexed { i, p ->
                val x = i * stepX
                val y = size.height - ((p - minVal) / range * size.height)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = SharkNavy,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            
            // Draw gradient area
            val fillPath = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SharkNavy.copy(alpha = 0.2f), Color.Transparent)
                )
            )
        }
        
        Text(
            "12 Month Net Projection: $${String.format(java.util.Locale.US, "%.0f", points.last())}",
            color = SharkMuted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
fun AiInsightCard(scenario: String) {
    val insight = when(scenario) {
        "Recession" -> "Warning: In a downturn, your runway drops from 180 to 45 days. Build your cash buffer now."
        "Bull Market" -> "Opportunity: High market returns could accelerate your retirement by 4 years if you invest 15% more."
        "Side Hustle +20%" -> "Impact: Adding $500/mo hustle income wipes out your student loans 18 months earlier."
        else -> "Stability: You are currently on track to save $12,400 this year. Consistency is your superpower."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20f, alpha = 0.1f)
            .border(1.dp, SharkNavy.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, null, tint = SharkNavy, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("SHARK ADVICE", color = SharkNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(insight, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun MilestoneItem(title: String, date: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(date, color = SharkMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = SharkMuted, modifier = Modifier.size(16.dp))
    }
}
