package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

// ── Mood ──────────────────────────────────────────────────────────────────────
// Removed Redeclaration of SharkMood as it is now in aicoachresponce.kt

// ── Bubble ────────────────────────────────────────────────────────────────────
data class FloatingBubble(
    val id: Long,
    val amount: Double,
    val isIncome: Boolean,
    var label: String = ""
)

// ── Insight lines (one per day, cycles) ───────────────────────────────────────
private val insightLines = listOf(
    "Every dollar you keep today is power tomorrow.",
    "Small wins stack. Don't underestimate today.",
    "You set this goal. Shark's watching.",
    "Stay in the zone and the streak stays alive.",
    "One clean day changes the whole week.",
    "The budget card is your shield. Use it.",
    "Discipline today. Options tomorrow.",
    "Shark doesn't judge. But the numbers do.",
    "You've come too far to slip now.",
    "Make today's balance worth bragging about."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uid: String,
    db: FirebaseFirestore,
    displayName: String,
    accountType: String,
    expenses: List<Expense>,
    incomeSources: List<IncomeSource>,
    bills: List<Bill> = emptyList(),
    goals: List<Goal> = emptyList(),
    recurringBills: List<RecurringBill> = emptyList(),
    onExpenseClick: (Expense) -> Unit,
    onAddIncomeClick: () -> Unit,
    onFeatureClick: ((String) -> Unit)? = null,
    onAddExpense: (() -> Unit)? = null
) {
    val firstName = displayName.split(" ").firstOrNull() ?: "User"
    val scope     = rememberCoroutineScope()

    // ── Financials ──
    val income  = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val spent   = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val balance = income - spent
    val score   = calcMoneyScore(income, spent)

    // AI Coach state
    var chatMessages  by remember { mutableStateOf(listOf<SharkChatMessage>()) }
    var isListening   by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var textInputVal  by remember { mutableStateOf("") }
    var currentSession by remember { mutableStateOf(SharkAgentSession.IDLE) }

    fun isToday(timestamp: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance()
        cal2.timeInMillis = timestamp
        return cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
                cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
    }

    val financialState = SharkFinancialState(
        dailyBudget     = if (incomeSources.isNotEmpty())
            incomeSources.sumOf { it.amount } / 30.0
        else income / 30.0,
        dailySpentSoFar = expenses
            .filter { isToday(it.createdAtDate.time) && it.category != "Income" }
            .sumOf { it.amount },
        currentStreak   = 0,  // TODO: load from Firestore users/{uid}/streak
        goalPercent     = 50, // TODO: load from user goal settings
        balance         = balance,
        moneyScore      = score,
        paydayInDays    = null,
        knownRecurring  = if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills,
        upcomingBills   = bills.filter { !it.isPaid },
        activeGoals     = goals.filter { !it.isCompleted },
        expenses        = expenses
    )

    fun performFullReset() {
        // 1. Wipe Expenses
        db.collection("users").document(uid).collection("expenses").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit()
        }
        // 2. Reset Streak
        db.collection("users").document(uid).update("streak", 0)
        // 3. Reset Session
        currentSession = SharkAgentSession.IDLE
    }

    fun updateStartingBalance(amount: Double) {
        // Log a base income transaction to set the starting balance
        val initialIncome = Expense(
            title = "Starting Balance",
            amount = amount,
            category = "Income",
            note = "Set by AI Agent",
            createdAt = Date()
        )
        db.collection("users").document(uid).collection("expenses").add(initialIncome)
    }

    fun processVoiceInput(input: String) {
        chatMessages = chatMessages + SharkChatMessage(input, isShark = false)

        val parsed   = AICoachNLP.parse(input, if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills, currentSession)
        val response = AICoachResponse.generate(parsed, financialState)

        chatMessages = chatMessages + SharkChatMessage(response.message, isShark = true)

        // AGENT ACTIONS
        if (response.performReset) {
            performFullReset()
            currentSession = SharkAgentSession.AWAITING_SETUP_BALANCE
        } else if (currentSession == SharkAgentSession.AWAITING_SETUP_BALANCE && parsed.amount != null) {
            updateStartingBalance(parsed.amount)
            currentSession = SharkAgentSession.IDLE
        }

        // Log normal transactions to Firestore
        if (response.logTransaction && parsed.amount != null && currentSession == SharkAgentSession.IDLE) {
            val expenseData = hashMapOf(
                "title"     to (parsed.merchantHint ?: parsed.category),
                "amount"    to parsed.amount,
                "category"  to parsed.category,
                "note"      to parsed.rawInput,
                "createdAt" to Date()
            )
            db.collection("users").document(uid)
                .collection("expenses")
                .add(expenseData)
        }

        if (response.calendarNote != null) {
            val noteData = hashMapOf(
                "text"      to response.calendarNote,
                "date"      to (response.calendarDate ?: System.currentTimeMillis()),
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("users").document(uid).collection("calendarNotes").add(noteData)
        }
    }

    // ── Streak ──
    var streak by remember { mutableStateOf(0) }
    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            streak = doc.getLong("streak")?.toInt() ?: 0
        }
    }

    // ── Daily budget ──
    val goalPct        = 0.50
    val payPeriodDays  = 14
    val spendablePool  = income * (1.0 - goalPct)
    val dailyBudget    = if (payPeriodDays > 0) (spendablePool / payPeriodDays).coerceAtLeast(0.0) else 0.0
    val todayRemaining = (dailyBudget - spent).coerceAtLeast(0.0)

    // ── Mood ──
    val sharkMood = when {
        todayRemaining <= 0           -> SharkMood.SAD // Using SAD from shared Enum
        todayRemaining >= dailyBudget -> SharkMood.HAPPY
        else                          -> SharkMood.NEUTRAL
    }

    // ── Lottie ──
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.cute_shark_animation)
    )
    var hasEntered by remember { mutableStateOf(false) }

    val entranceProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying   = !hasEntered,
        iterations  = 1,
        clipSpec    = LottieClipSpec.Frame(0, 24)
    )
    LaunchedEffect(entranceProgress) {
        if (entranceProgress >= 1f) hasEntered = true
    }

    val moodClip = when (sharkMood) {
        SharkMood.HAPPY   -> LottieClipSpec.Frame(72, 96)
        SharkMood.SAD     -> LottieClipSpec.Frame(24, 72) // UPSET changed to SAD
        SharkMood.NEUTRAL -> LottieClipSpec.Frame(96, 137)
        else              -> LottieClipSpec.Frame(96, 137)
    }
    val moodProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying   = hasEntered,
        iterations  = LottieConstants.IterateForever,
        clipSpec    = moodClip
    )
    val lottieProgress = if (!hasEntered) entranceProgress else moodProgress

    // ── Arc color ──
    val arcColor = when (sharkMood) {
        SharkMood.HAPPY   -> SharkGreen
        SharkMood.NEUTRAL -> SharkAmber
        SharkMood.SAD     -> SharkRed // SAD replaces UPSET
        else              -> SharkAmber
    }

    // ── Arc animation ──
    val animatedArc by animateFloatAsState(
        targetValue   = (score / 100f) * 300f,
        animationSpec = tween(1300, easing = EaseOutCubic),
        label         = "arc"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.06f,
        targetValue   = 0.18f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "glowAlpha"
    )

    // ── Insight ──
    val insightIndex = ((System.currentTimeMillis() / 86400000) % insightLines.size).toInt()
    val insightText  = insightLines[insightIndex]

    // ── Bubbles ──
    var bubbles       by remember { mutableStateOf(listOf<FloatingBubble>()) }
    var editingBubble by remember { mutableStateOf<FloatingBubble?>(null) }
    var editLabel     by remember { mutableStateOf("") }

    // ── Quick type dialogs ──
    var showIncomeInput by remember { mutableStateOf(false) }
    var showSpendInput  by remember { mutableStateOf(false) }

    fun launchBubble(bubble: FloatingBubble) {
        bubbles = bubbles + bubble
        scope.launch {
            delay(3000)
            val b = bubbles.find { it.id == bubble.id } ?: return@launch
            db.collection("users").document(uid).collection("expenses").add(
                mapOf(
                    "title"     to b.label.ifBlank { if (b.isIncome) "Income" else "Expense" },
                    "amount"    to b.amount,
                    "category"  to if (b.isIncome) "Income" else "Other",
                    "note"      to "",
                    "createdAt" to Date()
                )
            )
            bubbles = bubbles.filter { it.id != bubble.id }
        }
    }

    // ── UI ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBlack)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // Streak number
            Text(
                text       = "$streak",
                color      = Color.White,
                fontSize   = 56.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 56.sp
            )

            Spacer(Modifier.height(2.dp))

            // Shark + halo
            Box(
                modifier         = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(arcColor.copy(alpha = glowAlpha), Color.Transparent)
                            )
                        )
                )
                Canvas(Modifier.size(220.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r  = size.minDimension / 2
                    drawCircle(Color.White.copy(alpha = 0.06f), r, style = Stroke(12f, cap = StrokeCap.Round))
                    drawArc(arcColor, -90f, animatedArc, false, style = Stroke(12f, cap = StrokeCap.Round))
                    if (animatedArc > 0) {
                        val rad = Math.toRadians((-90 + animatedArc).toDouble())
                        drawCircle(arcColor, 7f, Offset((cx + r * cos(rad)).toFloat(), (cy + r * sin(rad)).toFloat()))
                    }
                }
                LottieAnimation(
                    composition = composition,
                    progress    = { lottieProgress },
                    modifier    = Modifier.size(185.dp)
                )
            }

            // Daily insight from Shark
            val dailyInsight = remember(expenses, bills) {
                AICoachResponse.generateDailyInsight(financialState)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text      = dailyInsight,
                color     = SharkMuted,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(18.dp))

            // Daily budget card
            DailyBudgetCard(firstName, accountType, dailyBudget, todayRemaining, arcColor)

            Spacer(Modifier.height(24.dp))

            // ── Three-button layout: Income | Mic | Spend ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // LEFT — Income button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D2A18))
                            .border(1.5.dp, SharkGreen.copy(alpha = 0.4f), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { showTextInput = true },
                                    onLongPress = { /* TODO: hold-to-scroll amount */ }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = SharkGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Income", color = SharkMuted, fontSize = 11.sp)
                }

                // CENTER — Microphone button (smaller)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isListening) SharkGreen else Color(0xFF111111))
                            .border(1.dp, if (isListening) SharkGreen else Color(0xFF2A2A2A), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { showTextInput = !showTextInput },
                                    onPress     = {
                                        isListening = true
                                        tryAwaitRelease()
                                        isListening = false
                                        // TODO: wire up Android SpeechRecognizer result here
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                            null,
                            tint     = if (isListening) Color.Black else SharkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Shark", color = SharkMuted, fontSize = 11.sp)
                }

                // RIGHT — Spend button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A0D0D))
                            .border(1.5.dp, SharkRed.copy(alpha = 0.4f), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { showTextInput = true },
                                    onLongPress = { /* TODO: hold-to-scroll amount */ }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, null, tint = SharkRed, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Spent", color = SharkMuted, fontSize = 11.sp)
                }
            }

            // Chat bubbles
            if (chatMessages.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    chatMessages.takeLast(4).forEach { msg ->
                        SharkChatBubble(
                            message = msg.text,
                            isShark = msg.isShark
                        )
                    }
                }
            }

            // Text input box (shows on double-tap of mic)
            if (showTextInput) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value         = textInputVal,
                        onValueChange = { textInputVal = it },
                        modifier      = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder   = { Text("Tell Shark...", color = SharkMuted, fontSize = 13.sp) },
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = Color(0xFF111111),
                            unfocusedContainerColor = Color(0xFF111111),
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor             = SharkGreen
                        ),
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInputVal.isNotBlank()) {
                                processVoiceInput(textInputVal.trim())
                                textInputVal  = ""
                                showTextInput = false
                            }
                        })
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (textInputVal.isNotBlank()) SharkGreen else Color(0xFF111111))
                            .clickable {
                                if (textInputVal.isNotBlank()) {
                                    processVoiceInput(textInputVal.trim())
                                    textInputVal  = ""
                                    showTextInput = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null,
                            tint     = if (textInputVal.isNotBlank()) Color.Black else SharkMuted,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Quick Feature Access
            QuickFeatureRow(onFeatureClick)

            Spacer(Modifier.height(32.dp))

            // Recent Activity Section
            if (expenses.isNotEmpty()) {
                RecentActivitySection(expenses.take(5), onExpenseClick)
            }

            // Income Sources Section
            if (incomeSources.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                IncomeSourcesSection(incomeSources)
            }

            Spacer(Modifier.height(120.dp))
        }

        // Floating bubbles
        bubbles.forEach { bubble ->
            FloatingBubbleView(bubble = bubble, onTap = {
                editingBubble = bubble
                editLabel     = bubble.label
            })
        }

        // Edit bubble dialog
        editingBubble?.let { b ->
            AlertDialog(
                onDismissRequest = { editingBubble = null },
                containerColor   = Color(0xFF111111),
                title = { Text("Name this?", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value         = editLabel,
                        onValueChange = { editLabel = it },
                        placeholder   = { Text("Coffee, gas, rent...", color = SharkMuted) },
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            focusedBorderColor   = SharkGreen,
                            unfocusedBorderColor = SharkMuted
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        bubbles = bubbles.map { if (it.id == b.id) it.copy(label = editLabel) else it }
                        editingBubble = null
                    }) { Text("Save", color = SharkGreen) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        bubbles = bubbles.filter { it.id != b.id }
                        editingBubble = null
                    }) { Text("Delete", color = SharkRed) }
                }
            )
        }

        if (showIncomeInput) QuickAmountDialog(true, { showIncomeInput = false
            if (it > 0) launchBubble(FloatingBubble(System.currentTimeMillis(), it, true)) }, { showIncomeInput = false })

        if (showSpendInput) QuickAmountDialog(false, { showSpendInput = false
            if (it > 0) launchBubble(FloatingBubble(System.currentTimeMillis(), it, false)) }, { showSpendInput = false })
    }
}

// ── Daily Budget Card ─────────────────────────────────────────────────────────
@Composable
private fun DailyBudgetCard(firstName: String, accountType: String, dailyBudget: Double, todayRemaining: Double, arcColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D1F12))
            .border(1.dp, arcColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text(firstName.uppercase(), color = SharkMuted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(accountType, color = SharkMuted.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Normal)
                }
                Text("DAILY BUDGET", color = SharkMuted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                val todayRemainingText = if (todayRemaining.isInfinite() || todayRemaining.isNaN()) "0.00" else String.format("%.2f", todayRemaining)
                val dailyBudgetText = if (dailyBudget.isInfinite() || dailyBudget.isNaN()) "0" else String.format("%.0f", dailyBudget)
                Text("$${todayRemainingText}", color = arcColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("of $${dailyBudgetText}", color = SharkMuted, fontSize = 13.sp)
            }
        }
    }
}

// ── Hold Scroll Button ────────────────────────────────────────────────────────
@Composable
private fun HoldScrollButton(label: String, isIncome: Boolean, onRelease: (Double) -> Unit, onDoubleTap: () -> Unit) {
    var holdAmount by remember { mutableStateOf(0.0) }
    var isHolding  by remember { mutableStateOf(false) }
    val color      = if (isIncome) SharkGreen else Color.White

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(1.5.dp, color.copy(alpha = 0.4f), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart  = { isHolding = true },
                        onDragEnd    = { if (holdAmount > 0) onRelease(holdAmount); holdAmount = 0.0; isHolding = false },
                        onDragCancel = { holdAmount = 0.0; isHolding = false },
                        onVerticalDrag = { _, drag -> holdAmount = (holdAmount + (-drag / 10.0)).coerceAtLeast(0.0) }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (isIncome) Icons.Default.Add else Icons.Default.Remove, null, tint = color, modifier = Modifier.size(22.dp))
                if (isHolding && holdAmount > 0)
                    Text("$${holdAmount.roundToInt()}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = SharkMuted, fontSize = 12.sp)
    }
}

@Composable
private fun QuickFeatureRow(onFeatureClick: ((String) -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickFeatureCard("Visual Models", Icons.AutoMirrored.Filled.TrendingUp, SharkNavy, Modifier.weight(1f)) {
            onFeatureClick?.invoke("Visual Models")
        }
        QuickFeatureCard("Bill Tracker", Icons.AutoMirrored.Filled.TrendingUp, SharkAmber, Modifier.weight(1f)) {
            onFeatureClick?.invoke("Bill Tracker")
        }
    }
}

@Composable
private fun QuickFeatureCard(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentActivitySection(expenses: List<Expense>, onExpenseClick: (Expense) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Recent Activity", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SharkMuted, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(16.dp))
        expenses.forEach { expense ->
            FlatTransactionRow(expense, onExpenseClick)
        }
    }
}

@Composable
private fun IncomeSourcesSection(sources: List<IncomeSource>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Income Sources", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        sources.forEach { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(source.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(source.frequency, color = SharkMuted, fontSize = 11.sp)
                }
                Text("$${String.format("%.0f", source.amount)}", color = SharkNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Floating Bubble ───────────────────────────────────────────────────────────
@Composable
private fun FloatingBubbleView(bubble: FloatingBubble, onTap: () -> Unit) {
    val offsetY = remember { Animatable(640f) }
    LaunchedEffect(bubble.id) {
        offsetY.animateTo(180f, tween(2800, easing = EaseOutCubic))
    }
    val color = if (bubble.isIncome) SharkGreen else Color.White
    val label = buildString {
        if (bubble.label.isNotBlank()) append("${bubble.label} · ")
        append("${if (bubble.isIncome) "+" else "-"}$${bubble.amount.roundToInt()}")
    }

    Box(Modifier.fillMaxSize().offset { IntOffset(0, offsetY.value.roundToInt()) }, Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onTap() }
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(label, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Quick Amount Dialog ───────────────────────────────────────────────────────
@Composable
private fun QuickAmountDialog(isIncome: Boolean, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var text  by remember { mutableStateOf("") }
    val color = if (isIncome) SharkGreen else SharkRed
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF111111),
        title = { Text(if (isIncome) "Amount received" else "Amount spent", color = Color.White) },
        text = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder   = { Text("0.00", color = SharkMuted) },
                prefix        = { Text("$", color = color) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = color, unfocusedBorderColor = SharkMuted
                )
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.toDoubleOrNull() ?: 0.0) }) { Text("Log it", color = color) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SharkMuted) } }
    )
}

// ── Kept from original ────────────────────────────────────────────────────────
fun calcMoneyScore(income: Double, spent: Double): Int {
    if (income <= 0) return 30
    val ratio       = spent / income
    val savingsRate = (income - spent) / income * 100
    var score       = 50
    score += when { savingsRate >= 20 -> 40; savingsRate >= 10 -> 25; savingsRate >= 0 -> 10; else -> -25 }
    score -= when { ratio > 1.2 -> 20; ratio > 1.0 -> 12; ratio > 0.9 -> 5; else -> 0 }
    return score.coerceIn(0, 100)
}

fun formatAmt(amount: Float): String {
    val absAmt = abs(amount)
    if (absAmt.isInfinite() || absAmt.isNaN()) return "$0.00"
    val sign   = if (amount < 0) "-" else ""
    return if (absAmt < 1000) "$sign$${String.format("%.2f", absAmt)}" else "$sign$${absAmt.toInt()}"
}

@Composable
fun FlatTransactionRow(expense: Expense, onClick: (Expense) -> Unit) {
    val isIncome = expense.category == "Income"
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick(expense) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(Color.White.copy(alpha = 0.06f), CircleShape), Alignment.Center) {
            Text(expense.category.take(1).uppercase(), color = if (isIncome) SharkGreen else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(expense.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(expense.category, color = SharkMuted, fontSize = 11.sp)
        }
        val amountText = if (expense.amount.isInfinite() || expense.amount.isNaN()) "0.00" else String.format("%.2f", expense.amount)
        Text(
            "${if (isIncome) "+" else "-"}$${amountText}",
            color = if (isIncome) SharkGreen else Color.White,
            fontSize = 14.sp, fontWeight = FontWeight.Bold
        )
    }
}