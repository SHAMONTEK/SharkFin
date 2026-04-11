package com.example.sharkfin

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharkfin.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoalTrackerScreen(
    uid: String,
    db: FirebaseFirestore,
    expenses: List<Expense>,
    goals: List<Goal>
) {
    var showAddGoal   by remember { mutableStateOf(false) }
    var selectedGoal  by remember { mutableStateOf<Goal?>(null) }

    // Optimized Calculations
    val metrics = remember(goals, expenses) {
        val totalTargeted  = goals.sumOf { it.targetAmount }
        val totalSaved     = goals.sumOf { it.savedAmount }
        val completedCount = goals.count { it.isCompleted }

        val monthlyIncome  = expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }
        val monthlySpend   = expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount }
        val monthlySurplus = (monthlyIncome - monthlySpend).coerceAtLeast(0.0)
        
        object {
            val targeted = totalTargeted
            val saved = totalSaved
            val completed = completedCount
            val surplus = monthlySurplus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Goal Tracker", style = SharkTypography.headlineLarge, color = SharkLabel)
                Text("${goals.size} goals · ${metrics.completed} completed", style = SharkTypography.labelMedium, color = SharkSecondary)
            }
            IconButton(
                onClick = { showAddGoal = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SharkGold.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Add, null, tint = SharkGold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SharkCard {
            Column {
                Text("TOTAL PROGRESS", style = SharkTypography.labelSmall, color = SharkSecondary)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "$${String.format(Locale.US, "%,.2f", metrics.saved)}",
                    style = SharkTypography.displayLarge.copy(fontSize = 32.sp),
                    color = SharkLabel
                )
                Text(
                    "of $${String.format(Locale.US, "%,.2f", metrics.targeted)} targeted",
                    style = SharkTypography.labelSmall,
                    color = SharkSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                val overallFraction = if (metrics.targeted > 0) (metrics.saved / metrics.targeted).toFloat().coerceIn(0f, 1f) else 0f
                GoalProgressBar(fraction = overallFraction, color = SharkGold, height = 8)

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MiniStat("Saved", "$${String.format(Locale.US, "%,.0f", metrics.saved)}", positive = true)
                    MiniStat("Remaining", "$${String.format(Locale.US, "%,.0f", (metrics.targeted - metrics.saved).coerceAtLeast(0.0))}", positive = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (metrics.surplus > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SharkPositive.copy(alpha = 0.05f))
                    .border(0.5.dp, SharkPositive.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = SharkPositive, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "You have $${String.format(Locale.US, "%,.0f", metrics.surplus)}/mo 'Free Cash' to save",
                        color = SharkPositive,
                        style = SharkTypography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flag, null, tint = SharkSecondary.copy(0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No goals yet", style = SharkTypography.bodyMedium, color = SharkSecondary)
                }
            }
        } else {
            val activeGoals    = goals.filter { !it.isCompleted }
            val completedGoals = goals.filter { it.isCompleted }

            if (activeGoals.isNotEmpty()) {
                SharkSectionHeader("ACTIVE")
                activeGoals.forEach { goal ->
                    GoalCard(
                        goal           = goal,
                        monthlySurplus = metrics.surplus,
                        onClick        = { selectedGoal = goal }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (completedGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SharkSectionHeader("COMPLETED")
                completedGoals.forEach { goal ->
                    GoalCard(
                        goal           = goal,
                        monthlySurplus = metrics.surplus,
                        onClick        = { selectedGoal = goal }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showAddGoal) {
        AddGoalSheet(uid = uid, db = db, onDismiss = { showAddGoal = false })
    }

    if (selectedGoal != null) {
        GoalDetailSheet(
            goal     = selectedGoal!!,
            uid      = uid,
            db       = db,
            onDismiss = { selectedGoal = null }
        )
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    monthlySurplus: Double,
    onClick: () -> Unit
) {
    val category   = remember(goal.category) { goalCategories.find { it.name == goal.category } ?: goalCategories.last() }
    val fraction   = remember(goal.savedAmount, goal.targetAmount) { if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f }
    val remaining  = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)

    val paceData = remember(goal.deadline, monthlySurplus, remaining) {
        if (goal.deadline.isNotEmpty()) {
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val deadlineDate = try { sdf.parse(goal.deadline) } catch (e: Exception) { null }

            if (deadlineDate != null) {
                val today        = Calendar.getInstance()
                val deadlineCal  = Calendar.getInstance().apply { time = deadlineDate }
                val monthsLeft   = ((deadlineCal.get(Calendar.YEAR) - today.get(Calendar.YEAR)) * 12 +
                        (deadlineCal.get(Calendar.MONTH) - today.get(Calendar.MONTH))).coerceAtLeast(0)

                val text = when {
                    monthsLeft == 0  -> "Due this month"
                    monthsLeft == 1  -> "1 month left"
                    else             -> "$monthsLeft months left"
                }
                val onPace = monthlySurplus > 0 && (monthlySurplus * monthsLeft) >= remaining
                text to onPace
            } else "" to null
        } else "No deadline" to null
    }

    SharkCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(category.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (goal.isCompleted) Icons.Default.CheckCircle else category.icon,
                    null,
                    tint = if (goal.isCompleted) SharkPositive else category.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.name,
                    color = SharkLabel,
                    style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    goal.category,
                    color = SharkSecondary,
                    style = SharkTypography.labelSmall
                )
            }

            if (paceData.second != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (paceData.second == true) SharkPositive.copy(alpha = 0.1f) else SharkAmber.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (paceData.second == true) "On pace ✓" else "Behind ⚠",
                        color = if (paceData.second == true) SharkPositive else SharkAmber,
                        style = SharkTypography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$${String.format(Locale.US, "%,.2f", goal.savedAmount)} saved",
                color = SharkLabel,
                style = SharkTypography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "of $${String.format(Locale.US, "%,.2f", goal.targetAmount)}",
                color = SharkSecondary,
                style = SharkTypography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        GoalProgressBar(
            fraction = fraction,
            color    = if (goal.isCompleted) SharkPositive else category.color,
            height   = 6
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${(fraction * 100).toInt()}% complete",
                color = if (goal.isCompleted) SharkPositive else category.color,
                style = SharkTypography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (paceData.first.isNotEmpty()) {
                Text(paceData.first, color = SharkSecondary, style = SharkTypography.labelSmall)
            }
        }
    }
}

@Composable
fun GoalProgressBar(fraction: Float, color: Color, height: Int) {
    val animFraction by animateFloatAsState(
        targetValue   = fraction,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label         = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(SharkSurfaceHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animFraction.coerceAtLeast(0f))
                .fillMaxHeight()
                .background(color, CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailSheet(
    goal: Goal,
    uid: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    val category   = remember(goal.category) { goalCategories.find { it.name == goal.category } ?: goalCategories.last() }
    val fraction   = remember(goal.savedAmount, goal.targetAmount) { if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f }
    val remaining  = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)

    var addAmount  by remember { mutableStateOf("") }
    var isSaving   by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SharkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SharkSurfaceHigh) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(category.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(goal.name, style = SharkTypography.headlineMedium, color = SharkLabel)
            Text(goal.category, style = SharkTypography.labelMedium, color = SharkSecondary)
            Spacer(modifier = Modifier.height(24.dp))

            GoalArc(fraction = fraction, color = category.color)

            Spacer(modifier = Modifier.height(24.dp))

            GoalDetailRow("Saved", "$${String.format(Locale.US, "%,.2f", goal.savedAmount)}")
            GoalDetailRow("Target", "$${String.format(Locale.US, "%,.2f", goal.targetAmount)}")
            GoalDetailRow("Remaining", "$${String.format(Locale.US, "%,.2f", remaining)}")
            if (goal.deadline.isNotEmpty()) GoalDetailRow("Deadline", goal.deadline)

            Spacer(modifier = Modifier.height(24.dp))

            if (!goal.isCompleted) {
                SheetInputField(
                    value         = addAmount,
                    onValueChange = { addAmount = it },
                    label         = "ADD TO SAVINGS",
                    placeholder   = "0.00",
                    keyboardType  = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = addAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            isSaving = true
                            val newSaved    = goal.savedAmount + amount
                            val isNowDone   = newSaved >= goal.targetAmount

                            db.collection("users").document(uid)
                                .collection("goals").document(goal.id)
                                .update(mapOf(
                                    "savedAmount"  to newSaved,
                                    "isCompleted"  to isNowDone
                                ))
                                .addOnSuccessListener { isSaving = false; onDismiss() }
                                .addOnFailureListener { isSaving = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = SharkGold, contentColor = SharkBg),
                    enabled  = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(color = SharkBg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Add Savings", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        db.collection("users").document(uid)
                            .collection("goals").document(goal.id)
                            .update("isCompleted", true)
                            .addOnSuccessListener { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SharkGold),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, SharkGold.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Complete", fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SharkPositive.copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SharkPositive, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Goal completed! 🎉", color = SharkPositive, style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        db.collection("users").document(uid)
                            .collection("goals").document(goal.id)
                            .update("isCompleted", false)
                            .addOnSuccessListener { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SharkSecondary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, SharkCardBorder)
                ) {
                    Text("Reopen Goal", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    db.collection("users").document(uid)
                        .collection("goals").document(goal.id)
                        .delete()
                        .addOnSuccessListener { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Goal", color = SharkRed, style = SharkTypography.labelMedium)
            }
        }
    }
}

@Composable
fun GoalArc(fraction: Float, color: Color) {
    val animFraction by animateFloatAsState(
        targetValue   = fraction,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label         = "arc"
    )

    Box(
        modifier         = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val radius      = (size.minDimension / 2f) - strokeWidth
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(size.width / 2 - radius, size.height / 2 - radius)

            drawArc(
                color       = SharkSurfaceHigh,
                startAngle  = 135f,
                sweepAngle  = 270f,
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color       = color,
                startAngle  = 135f,
                sweepAngle  = 270f * animFraction,
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(fraction * 100).toInt()}%",
                color      = SharkLabel,
                style      = SharkTypography.headlineLarge.copy(fontSize = 28.sp)
            )
            Text("saved", style = SharkTypography.labelSmall, color = SharkSecondary)
        }
    }
}

@Composable
fun GoalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = SharkTypography.labelMedium, color = SharkSecondary)
        Text(value, style = SharkTypography.labelMedium.copy(fontWeight = FontWeight.Bold), color = SharkLabel)
    }
    HorizontalDivider(color = SharkCardBorder, thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    uid: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    var name         by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var deadline     by remember { mutableStateOf("") }
    var selectedCat  by remember { mutableStateOf(goalCategories[0]) }
    var isSaving     by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SharkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SharkSurfaceHigh) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text("New Goal", style = SharkTypography.headlineMedium, color = SharkLabel)
            Spacer(modifier = Modifier.height(24.dp))

            Text("CATEGORY", style = SharkTypography.labelSmall, color = SharkSecondary)
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goalCategories.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            val isSelected = selectedCat == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SharkGold.copy(alpha = 0.15f) else SharkBg.copy(0.3f))
                                    .border(0.5.dp, if (isSelected) SharkGold else SharkCardBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedCat = cat }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(cat.icon, null, tint = if (isSelected) SharkGold else SharkSecondary, modifier = Modifier.size(14.dp))
                                    Text(cat.name, color = if (isSelected) SharkGold else SharkSecondary, style = SharkTypography.labelSmall)
                                }
                            }
                        }
                        if (row.size < 3) Spacer(Modifier.weight((3-row.size).toFloat()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SheetInputField(name,         { name = it },         "GOAL NAME",   "e.g. Emergency Fund")
            SheetInputField(targetAmount, { targetAmount = it }, "TARGET AMOUNT","0.00", KeyboardType.Decimal)
            SheetInputField(deadline,     { deadline = it },     "DEADLINE (OPTIONAL)", "MM/DD/YYYY")

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val parsedTarget = targetAmount.toDoubleOrNull()
                    if (name.isNotBlank() && parsedTarget != null && parsedTarget > 0) {
                        isSaving = true
                        val goal = Goal(
                            name         = name.trim(),
                            targetAmount = parsedTarget,
                            savedAmount  = 0.0,
                            category     = selectedCat.name,
                            deadline     = deadline.trim(),
                            colorHex     = String.format("#%06X", (0xFFFFFF and selectedCat.color.value.toInt()))
                        )
                        db.collection("users").document(uid).collection("goals").add(goal)
                            .addOnSuccessListener { isSaving = false; onDismiss() }
                            .addOnFailureListener { isSaving = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SharkGold, contentColor = SharkBg),
                enabled  = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(color = SharkBg, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Create Goal", fontWeight = FontWeight.Bold)
            }
        }
    }
}
