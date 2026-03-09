package com.example.sharkfin

// ═══════════════════════════════════════════════════════════════════════
// HomeScreen.kt
// Drop-in replacement for the HomeScreen composable in WelcomeActivity.kt
//
// Design: Cash App energy. Black. Big number. Green only. No borders.
// Features:
//   • Money Score (0–100) with breathing arc — center stage
//   • Balance counts up on load
//   • Score label: Thriving / Steady / Watchful / Stretched / Critical
//   • Two stat chips: Income vs Spent
//   • Quick action row: Add, Bills, Goals, Visuals
//   • Recent transactions — flat, no cards
// ═══════════════════════════════════════════════════════════════════════

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


val SharkRed   = Color(0xFFFF3B30)

@Composable
fun HomeScreen(
    uid             : String,
    db              : FirebaseFirestore,
    displayName     : String,
    accountType     : String,
    expenses        : List<Expense>,
    incomeSources   : List<IncomeSource>,
    onExpenseClick  : (Expense) -> Unit,
    onAddIncomeClick: () -> Unit,
    onFeatureClick  : ((String) -> Unit)? = null,
    onAddExpense    : (() -> Unit)? = null
) {
    // ── CALCULATIONS ──────────────────────────────────────────────────
    val income   = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val spent    = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val balance  = income - spent
    val score    = calcMoneyScore(income, spent)
    val scoreLabel = when {
        score >= 80 -> "Thriving"
        score >= 60 -> "Steady"
        score >= 40 -> "Watchful"
        score >= 20 -> "Stretched"
        else        -> "Critical"
    }
    val scoreColor = when {
        score >= 60 -> SharkGreen
        score >= 35 -> Color(0xFFF5A623)
        else        -> SharkRed
    }

    // ── ANIMATED VALUES ───────────────────────────────────────────────
    val animBalance by animateFloatAsState(
        targetValue   = balance.toFloat(),
        animationSpec = tween(1100, easing = EaseOutExpo),
        label         = "bal"
    )
    val animScore by animateIntAsState(
        targetValue   = score,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "score"
    )
    val animArc by animateFloatAsState(
        targetValue   = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1300, easing = EaseOutCubic),
        label         = "arc"
    )

    // Breathing
    val breath = rememberInfiniteTransition(label = "b")
    val breathScale by breath.animateFloat(
        initialValue  = 0.97f,
        targetValue   = 1.03f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "bs"
    )
    val glowAlpha by breath.animateFloat(
        initialValue  = 0.06f,
        targetValue   = 0.16f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "ga"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(64.dp))

        // ── GREETING ──────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Text(
                "Hey, ${displayName.split(" ").first()}",
                color      = SharkMuted,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── MONEY SCORE CIRCLE ────────────────────────────────────────
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(scoreColor.copy(alpha = glowAlpha), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            // Arc canvas
            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .scale(breathScale)
            ) {
                val cx    = size.width / 2f
                val cy    = size.height / 2f
                val r     = size.minDimension / 2f
                val sw    = 14f

                // Track
                drawCircle(
                    color  = Color.White.copy(alpha = 0.06f),
                    radius = r,
                    center = Offset(cx, cy),
                    style  = Stroke(sw)
                )

                // Fill arc
                if (animArc > 0f) {
                    drawArc(
                        brush      = Brush.sweepGradient(
                            listOf(scoreColor.copy(0.5f), scoreColor, scoreColor),
                            center = Offset(cx, cy)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animArc,
                        useCenter  = false,
                        topLeft    = Offset(cx - r, cy - r),
                        size       = Size(r * 2, r * 2),
                        style      = Stroke(sw, cap = StrokeCap.Round)
                    )

                    // Tip dot
                    val tipAngle = Math.toRadians((-90.0 + 360.0 * animArc))
                    val tipR     = r
                    drawCircle(
                        color  = scoreColor,
                        radius = sw / 2f + 2f,
                        center = Offset(
                            cx + tipR * cos(tipAngle).toFloat(),
                            cy + tipR * sin(tipAngle).toFloat()
                        )
                    )
                }
            }

            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.scale(breathScale)
            ) {
                // Score badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "$animScore",
                        color      = scoreColor,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "· $scoreLabel",
                        color      = scoreColor.copy(alpha = 0.7f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Big balance
                Text(
                    formatAmt(animBalance),
                    color         = Color.White,
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-2).sp
                )

                Text(
                    "MONEY SCORE",
                    color         = SharkMuted,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        // ── STAT CHIPS ────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                modifier = Modifier.weight(1f),
                label    = "Income",
                value    = "+\$${String.format("%.0f", income)}",
                color    = SharkGreen
            )
            StatChip(
                modifier = Modifier.weight(1f),
                label    = "Spent",
                value    = "-\$${String.format("%.0f", spent)}",
                color    = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── QUICK ACTIONS ─────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickAction("Add",     Icons.Default.Add,       SharkGreen) { onAddExpense?.invoke() }
            QuickAction("Bills",   Icons.Default.Receipt,   Color.White) { onFeatureClick?.invoke("Bill Tracker") }
            QuickAction("Goals",   Icons.Default.Flag,      Color.White) { onFeatureClick?.invoke("Goal Tracker") }
            QuickAction("Visuals", Icons.Default.BarChart,  Color.White) { onFeatureClick?.invoke("Visual Models") }
        }

        Spacer(Modifier.height(44.dp))

        // ── RECENT TRANSACTIONS ───────────────────────────────────────
        if (expenses.isNotEmpty()) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Recent",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "See all",
                    color    = SharkGreen,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            expenses.take(6).forEach { expense ->
                FlatTransactionRow(expense, onExpenseClick)
            }
        } else {
            // Empty state
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No transactions yet", color = SharkMuted, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to add your first one",
                        color    = SharkMuted.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(110.dp))
    }
}

// ─── Stat Chip ────────────────────────────────────────────────────────────
@Composable
fun StatChip(modifier: Modifier, label: String, value: String, color: Color) {
    Column(
        modifier            = modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, color = SharkMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    }
}

// ─── Quick Action ─────────────────────────────────────────────────────────
@Composable
fun QuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier         = Modifier
                .size(58.dp)
                .background(
                    if (label == "Add") SharkGreen else Color.White.copy(alpha = 0.06f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint     = if (label == "Add") Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = SharkMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

// ─── Flat Transaction Row ─────────────────────────────────────────────────
@Composable
fun FlatTransactionRow(expense: Expense, onClick: (Expense) -> Unit) {
    val isIncome = expense.category == "Income"

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick(expense) }
            .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle
        Box(
            modifier         = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                expense.category.take(1).uppercase(),
                color      = if (isIncome) SharkGreen else Color.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(expense.title,    color = Color.White,   fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(expense.category, color = SharkMuted,    fontSize = 11.sp)
        }

        Text(
            "${if (isIncome) "+" else "-"}\$${String.format("%.2f", expense.amount)}",
            color      = if (isIncome) SharkGreen else Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }

    // Divider
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
    )
}

// ─── Money Score Calculator ───────────────────────────────────────────────
fun calcMoneyScore(income: Double, spent: Double): Int {
    if (income <= 0) return 30
    val ratio       = spent / income
    val savingsRate = ((income - spent) / income * 100).coerceIn(-100.0, 100.0)
    var score       = 50
    score += when {
        savingsRate >= 20 -> 40
        savingsRate >= 10 -> 25
        savingsRate >= 0  -> 10
        else              -> -25
    }
    score -= when {
        ratio > 1.2 -> 20
        ratio > 1.0 -> 12
        ratio > 0.9 -> 5
        else        -> 0
    }
    return score.coerceIn(0, 100)
}

// ─── Format balance ───────────────────────────────────────────────────────
fun formatAmt(amount: Float): String {
    val abs = abs(amount)
    val prefix = if (amount < 0) "-\$" else "\$"
    return if (abs >= 1000) "$prefix${String.format("%.0f", abs)}"
    else "$prefix${String.format("%.2f", abs)}"
}
