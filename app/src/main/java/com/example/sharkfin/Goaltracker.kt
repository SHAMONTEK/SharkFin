package com.example.sharkfin

// ─── Imports ───────────────────────────────────────────────────────────────
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
// ─── Goal Tracker Screen ───────────────────────────────────────────────────
@Composable
fun GoalTrackerScreen(
    uid: String,
    db: FirebaseFirestore,
    expenses: List<Expense>,
    goals: List<Goal>
) {
    var showAddGoal   by remember { mutableStateOf(false) }
    var selectedGoal  by remember { mutableStateOf<Goal?>(null) }

    // ── DERIVED STATS ─────────────────────────────────────────────────────
    val totalTargeted  = goals.sumOf { it.targetAmount }
    val totalSaved     = goals.sumOf { it.savedAmount }
    val completedCount = goals.count { it.isCompleted }

    val monthlyIncome  = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val monthlySpend   = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val monthlySurplus = (monthlyIncome - monthlySpend).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // ── HEADER ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Goal Tracker", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("${goals.size} goals · $completedCount completed", color = SharkMuted, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SharkNavy, CircleShape)
                    .clickable { showAddGoal = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add Goal", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── SUMMARY CARD ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SharkSurfaceHigh, SharkSurface)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text("Total Progress", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "\$${String.format("%.2f", totalSaved)}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "of \$${String.format("%.2f", totalTargeted)} targeted",
                    color = SharkMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val overallFraction = if (totalTargeted > 0) (totalSaved / totalTargeted).toFloat().coerceIn(0f, 1f) else 0f
                GoalProgressBar(fraction = overallFraction, color = SharkNavy, height = 8)

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MiniStat("Saved",     "+\$${String.format("%.0f", totalSaved)}",    positive = true)
                    MiniStat("Remaining", "-\$${String.format("%.0f", (totalTargeted - totalSaved).coerceAtLeast(0.0))}", positive = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── MONTHLY SURPLUS PILL ──────────────────────────────────────────
        if (monthlySurplus > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SharkNavy.copy(alpha = 0.08f))
                    .border(1.dp, SharkNavy.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = SharkNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "You have \$${String.format("%.0f", monthlySurplus)}/mo available to save",
                        color = SharkNavy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── GOAL CARDS ────────────────────────────────────────────────────
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .glassCard(alpha = 0.05f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flag, null, tint = SharkMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No goals yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tap + to set your first goal", color = SharkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            val activeGoals    = goals.filter { !it.isCompleted }
            val completedGoals = goals.filter { it.isCompleted }

            if (activeGoals.isNotEmpty()) {
                Text("Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                activeGoals.forEach { goal ->
                    GoalCard(
                        goal           = goal,
                        monthlySurplus = monthlySurplus,
                        onClick        = { selectedGoal = goal }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (completedGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Completed", color = SharkMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                completedGoals.forEach { goal ->
                    GoalCard(
                        goal           = goal,
                        monthlySurplus = monthlySurplus,
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

// ─── Goal Card ─────────────────────────────────────────────────────────────
@Composable
fun GoalCard(
    goal: Goal,
    monthlySurplus: Double,
    onClick: () -> Unit
) {
    val category   = goalCategories.find { it.name == goal.category } ?: goalCategories.last()
    val fraction   = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining  = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)

    val deadlineText: String
    val onPace: Boolean?

    if (goal.deadline.isNotEmpty()) {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val deadlineDate = try { sdf.parse(goal.deadline) } catch (e: Exception) { null }

        if (deadlineDate != null) {
            val today        = Calendar.getInstance()
            val deadlineCal  = Calendar.getInstance().apply { time = deadlineDate }
            val monthsLeft   = ((deadlineCal.get(Calendar.YEAR) - today.get(Calendar.YEAR)) * 12 +
                    (deadlineCal.get(Calendar.MONTH) - today.get(Calendar.MONTH))).coerceAtLeast(0)

            deadlineText = when {
                monthsLeft == 0  -> "Due this month"
                monthsLeft == 1  -> "1 month left"
                else             -> "$monthsLeft months left"
            }

            onPace = monthlySurplus > 0 && (monthlySurplus * monthsLeft) >= remaining
        } else {
            deadlineText = ""
            onPace = null
        }
    } else {
        deadlineText = "No deadline"
        onPace = null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(
                cornerRadius = 20f,
                alpha = if (goal.isCompleted) 0.04f else 0.08f
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        category.color.copy(alpha = if (goal.isCompleted) 0.08f else 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (goal.isCompleted) Icons.Default.CheckCircle else category.icon,
                    null,
                    tint = if (goal.isCompleted) SharkNavy else category.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.name,
                    color = if (goal.isCompleted) SharkMuted else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    goal.category,
                    color = SharkMuted,
                    fontSize = 11.sp
                )
            }

            if (onPace != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (onPace) SharkNavy.copy(alpha = 0.15f)
                            else SharkAmber.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (onPace) "On pace ✓" else "Behind ⚠",
                        color = if (onPace) SharkNavy else SharkAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "\$${String.format("%.2f", goal.savedAmount)} saved",
                color = if (goal.isCompleted) SharkMuted else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "of \$${String.format("%.2f", goal.targetAmount)}",
                color = SharkMuted,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        GoalProgressBar(
            fraction = fraction,
            color    = if (goal.isCompleted) SharkMuted else category.color,
            height   = 6
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${(fraction * 100).toInt()}% complete",
                color = if (goal.isCompleted) SharkMuted else category.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (deadlineText.isNotEmpty()) {
                Text(deadlineText, color = SharkMuted, fontSize = 11.sp)
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
            .clip(RoundedCornerShape((height / 2).dp))
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animFraction.coerceAtLeast(0f))
                .fillMaxHeight()
                .clip(RoundedCornerShape((height / 2).dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f))
                    )
                )
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
    val category   = goalCategories.find { it.name == goal.category } ?: goalCategories.last()
    val fraction   = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining  = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)

    var addAmount  by remember { mutableStateOf("") }
    var isSaving   by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SharkBase,
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).width(40.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
        }
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
                    .background(category.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(goal.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(goal.category, color = SharkMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))

            GoalArc(fraction = fraction, color = category.color)

            Spacer(modifier = Modifier.height(20.dp))

            GoalDetailRow("Saved",     "\$${String.format("%.2f", goal.savedAmount)}")
            GoalDetailRow("Target",    "\$${String.format("%.2f", goal.targetAmount)}")
            GoalDetailRow("Remaining", "\$${String.format("%.2f", remaining)}")
            if (goal.deadline.isNotEmpty()) GoalDetailRow("Deadline", goal.deadline)

            Spacer(modifier = Modifier.height(20.dp))

            if (!goal.isCompleted) {
                SheetInputField(
                    value         = addAmount,
                    onValueChange = { addAmount = it },
                    label         = "Add to savings",
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = SharkNavy),
                    enabled  = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Add Savings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        db.collection("users").document(uid)
                            .collection("goals").document(goal.id)
                            .update("isCompleted", true)
                            .addOnSuccessListener { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SharkNavy),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, SharkNavy.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Complete", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SharkNavy.copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SharkNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Goal completed! 🎉", color = SharkNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        db.collection("users").document(uid)
                            .collection("goals").document(goal.id)
                            .update("isCompleted", false)
                            .addOnSuccessListener { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SharkMuted),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, SharkMuted.copy(alpha = 0.3f))
                ) {
                    Text("Reopen Goal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    db.collection("users").document(uid)
                        .collection("goals").document(goal.id)
                        .delete()
                        .addOnSuccessListener { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, null, tint = SharkRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Goal", color = SharkRed, fontSize = 13.sp)
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
            val strokeWidth = 14f
            val radius      = (size.minDimension / 2f) - strokeWidth
            val topLeft     = androidx.compose.ui.geometry.Offset(
                size.width / 2f - radius,
                size.height / 2f - radius
            )
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

            drawArc(
                color       = Color.White.copy(alpha = 0.07f),
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
                color      = Color.White,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text("saved", color = SharkMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun GoalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SharkMuted, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
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
        containerColor   = SharkBase,
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).width(40.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
        }
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
            Text("New Goal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Category", color = SharkMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            goalCategories.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cat ->
                        val isSelected = selectedCat == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) cat.color.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(cat.icon, null, tint = if (isSelected) cat.color else SharkMuted, modifier = Modifier.size(13.dp))
                                Text(cat.name, color = if (isSelected) cat.color else SharkMuted, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            SheetInputField(name,         { name = it },         "Goal Name",   "e.g. Emergency Fund, New Laptop")
            Spacer(modifier = Modifier.height(12.dp))
            SheetInputField(targetAmount, { targetAmount = it }, "Target Amount","0.00", KeyboardType.Decimal)
            Spacer(modifier = Modifier.height(12.dp))
            SheetInputField(deadline,     { deadline = it },     "Deadline (optional)", "MM/DD/YYYY")

            Spacer(modifier = Modifier.height(28.dp))

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

                        db.collection("users").document(uid)
                            .collection("goals")
                            .add(goal)
                            .addOnSuccessListener { isSaving = false; onDismiss() }
                            .addOnFailureListener { isSaving = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SharkNavy),
                enabled  = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Create Goal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}