package com.example.sharkfin

import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharkfin.ui.theme.SharkTypography
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    uid: String,
    db: FirebaseFirestore,
    displayName: String,
    accountType: String,
    expenses: List<Expense>,
    incomeSources: List<IncomeSource>,
    onExpenseClick: (Expense) -> Unit,
    onAddIncomeClick: () -> Unit,
    onFeatureClick: ((String) -> Unit)? = null,
    onAddExpense: (() -> Unit)? = null
) {
    val firstName = displayName.split(" ").firstOrNull() ?: "User"

    val income = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val spent = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val balance = income - spent
    val score = calcMoneyScore(income, spent)

    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(durationMillis = 1100, easing = EaseOutExpo)
    )
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
    )
    val animatedArc by animateFloatAsState(
        targetValue = (animatedScore / 100f) * 360f,
        animationSpec = tween(durationMillis = 1300, easing = EaseOutCubic)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scoreColor = when {
        animatedScore >= 60 -> SharkNavy
        animatedScore >= 35 -> SharkAmber
        else -> SharkRed
    }
    val scoreLabel = when {
        animatedScore >= 80 -> "Thriving"
        animatedScore >= 60 -> "Steady"
        animatedScore >= 35 -> "Watchful"
        animatedScore >= 10 -> "Stretched"
        else -> "Critical"
    }

    var showScoreDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBase)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Hey, $firstName",
            color = SharkMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(280.dp)
                .clickable { showScoreDetails = true },
            contentAlignment = Alignment.Center
        ) {
            // Subtle glow background
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(scoreColor.copy(alpha = glowAlpha), Color.Transparent),
                            center = Offset(110f, 110f),
                            radius = 200f
                        )
                    )
            )

            // Breathing circle with arc
            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .scale(breathScale)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 2

                // Track circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = radius,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )

                // Fill arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(scoreColor, scoreColor.copy(alpha = 0.8f)),
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedArc,
                    useCenter = false,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )

                // Tip dot
                if (animatedArc > 0) {
                    val angleRad = Math.toRadians((-90 + animatedArc).toDouble())
                    val dotX = centerX + (radius * cos(angleRad)).toFloat()
                    val dotY = centerY + (radius * sin(angleRad)).toFloat()
                    drawCircle(
                        color = scoreColor,
                        radius = 8f,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // Center content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatAmt(animatedBalance),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MONEY SCORE",
                    color = SharkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = scoreLabel,
                    color = scoreColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Stat chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniStat("Income", "+$${income.toInt()}", SharkNavy)
            MiniStat("Spent", "-$${spent.toInt()}", Color.White)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickAction("Add", Icons.Default.Add, SharkNavy, onClick = onAddExpense)
            QuickAction("Bills", Icons.Default.Receipt, Color.White, onClick = { onFeatureClick?.invoke("Bill Tracker") })
            QuickAction("Goals", Icons.Default.Flag, Color.White, onClick = { onFeatureClick?.invoke("Goal Tracker") })
            QuickAction("Visuals", Icons.Default.BarChart, Color.White, onClick = { onFeatureClick?.invoke("Visual Models") })
        }

        Spacer(modifier = Modifier.height(44.dp))

        if (expenses.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses.take(5)) { expense ->
                    FlatTransactionRow(expense = expense, onClick = { onExpenseClick(expense) })
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions yet",
                    color = SharkMuted,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(110.dp)) // Nav clearance
    }

    if (showScoreDetails) {
        ScoreDetailsSheet(
            score = animatedScore,
            scoreLabel = scoreLabel,
            scoreColor = scoreColor,
            income = income,
            spent = spent,
            onDismiss = { showScoreDetails = false }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = SharkMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.clickable { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (label == "Add") SharkNavy else Color.White.copy(alpha = 0.06f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (label == "Add") Color.White else color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = SharkMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FlatTransactionRow(expense: Expense, onClick: (Expense) -> Unit) {
    val isIncome = expense.category == "Income"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(expense) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                expense.category.take(1).uppercase(),
                color = if (isIncome) SharkNavy else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(expense.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(expense.category, color = SharkMuted, fontSize = 11.sp)
        }

        Text(
            "${if (isIncome) "+" else "-"}\$${String.format("%.2f", expense.amount)}",
            color = if (isIncome) SharkNavy else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreDetailsSheet(
    score: Int,
    scoreLabel: String,
    scoreColor: Color,
    income: Double,
    spent: Double,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SharkBase,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Money Score Details",
                style = SharkTypography.headlineLarge
            )
            Text(
                text = "$score - $scoreLabel",
                color = scoreColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your score reflects your financial health based on savings rate.",
                color = SharkMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text("Income: $${income.toInt()}", color = SharkNavy)
                Text("Spent: $${spent.toInt()}", color = Color.White)
                Text("Savings Rate: ${((income - spent) / income * 100).toInt()}%", color = SharkMuted)
            }
            Text(
                text = "Tap to improve: Set goals, track bills, or import statements for accurate insights.",
                color = SharkMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun calcMoneyScore(income: Double, spent: Double): Int {
    if (income <= 0) return 30
    val ratio = spent / income
    val savingsRate = (income - spent) / income * 100
    var score = 50
    score += when {
        savingsRate >= 20 -> 40
        savingsRate >= 10 -> 25
        savingsRate >= 0 -> 10
        else -> -25
    }
    score -= when {
        ratio > 1.2 -> 20
        ratio > 1.0 -> 12
        ratio > 0.9 -> 5
        else -> 0
    }
    return score.coerceIn(0, 100)
}

fun formatAmt(amount: Float): String {
    val absAmt = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return if (absAmt < 1000) "$sign$${String.format("%.2f", absAmt)}" else "$sign$${absAmt.toInt()}"
}
