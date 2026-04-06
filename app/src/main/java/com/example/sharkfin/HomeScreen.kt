package com.example.sharkfin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ── Input Mode State ─────────────────────────────────────────────────────────
sealed class InputMode {
    data object Voice : InputMode()
    data object Text : InputMode()
}

// ── Bubble ────────────────────────────────────────────────────────────────────
data class FloatingBubble(
    val id: Long,
    val amount: Double,
    val isIncome: Boolean,
    var label: String = ""
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
    portfolio: List<PortfolioAsset> = emptyList(),
    bills: List<Bill> = emptyList(),
    goals: List<Goal> = emptyList(),
    debts: List<Debt> = emptyList(),
    recurringBills: List<RecurringBill> = emptyList(),
    onExpenseClick: (Expense) -> Unit,
    onAddIncomeClick: () -> Unit,
    onFeatureClick: ((String) -> Unit)? = null,
    onAddExpense: (() -> Unit)? = null
) {
    val firstName = displayName.split(" ").firstOrNull() ?: "User"
    val scope     = rememberCoroutineScope()
    val context   = LocalContext.current
    val haptic    = LocalHapticFeedback.current

    // ── Financials ──
    val income  = expenses.filter { it.category == "Income" || it.category == "1099 Income" || it.category == "Passive Income" }.sumOf { it.amount }
    val spent   = expenses.filter { it.category != "Income" && it.category != "Passive Income" }.sumOf { it.amount }
    val balance = income - spent
    val score   = calcMoneyScore(income, spent)
    val totalDebt = debts.sumOf { it.currentBalance }

    // Average daily burn (last 30 days or total)
    val avgDailyBurn = if (expenses.isNotEmpty()) {
        val totalSpent = spent
        val firstExpense = expenses.minByOrNull { it.createdAtDate.time }?.createdAtDate?.time ?: System.currentTimeMillis()
        val daysElapsed = ((System.currentTimeMillis() - firstExpense) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        totalSpent / daysElapsed.toDouble()
    } else 0.0

    val runwayDays = if (avgDailyBurn > 0) (balance / avgDailyBurn).toInt().coerceAtLeast(0) else 0

    // Passive Income / Dividends for Snowball
    val passiveIncome = expenses.filter { it.category == "Passive Income" }.sumOf { it.amount }

    // AI Coach state
    var chatMessages  by remember { mutableStateOf(listOf<SharkChatMessage>()) }
    var isListening   by remember { mutableStateOf(false) }
    var liveTranscript by remember { mutableStateOf("") }
    var isThinking    by remember { mutableStateOf(false) }
    var inputMode     by remember { mutableStateOf<InputMode>(InputMode.Voice) }
    var textInputVal  by remember { mutableStateOf("") }
    var currentSession by remember { mutableStateOf(SharkAgentSession.IDLE) }
    var awaitingConfirm by remember { mutableStateOf<ParsedTransaction?>(null) }

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
            .filter { isToday(it.createdAtDate.time) && it.category != "Income" && it.category != "Passive Income" }
            .sumOf { it.amount },
        currentStreak   = 0,
        goalPercent     = 50,
        balance         = balance,
        moneyScore      = score,
        paydayInDays    = null,
        knownRecurring  = if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills,
        upcomingBills   = bills.filter { !it.isPaid },
        activeGoals     = goals.filter { !it.isCompleted },
        expenses        = expenses,
        averageDailyBurn = avgDailyBurn,
        totalDebt       = totalDebt
    )

    fun logParsedTransaction(parsed: ParsedTransaction) {
        if (parsed.amount != null) {
            val expenseData = hashMapOf(
                "title"     to (parsed.merchantHint ?: parsed.category),
                "amount"    to parsed.amount,
                "category"  to parsed.category,
                "note"      to parsed.rawInput,
                "createdAt" to Date()
            )
            db.collection("users").document(uid).collection("expenses").add(expenseData)
        }
    }

    fun performFullReset() {
        db.collection("users").document(uid).collection("expenses").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit()
        }
        db.collection("users").document(uid).update("streak", 0)
        currentSession = SharkAgentSession.IDLE
    }

    fun updateStartingBalance(amount: Double) {
        val initialIncome = Expense(
            title = "Starting Balance",
            amount = amount,
            category = "Income",
            note = "Set by AI Agent",
            createdAt = Date()
        )
        db.collection("users").document(uid).collection("expenses").add(initialIncome)
    }

    fun updateBillAmount(key: String, newAmount: Double) {
        db.collection("users").document(uid).collection("bills").whereEqualTo("name", key).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    snapshot.documents.first().reference.update("amount", newAmount)
                }
            }
    }

    fun processVoiceInput(input: String) {
        if (input.isBlank()) return
        
        scope.launch {
            isThinking = true
            chatMessages = chatMessages + SharkChatMessage(input, isShark = false)
            
            delay(1000)

            // UPGRADE: Confirmation handling logic
            if (awaitingConfirm != null) {
                if (input.lowercase().contains("yes") || input.lowercase().contains("yeah") || input.lowercase().contains("confirm")) {
                    logParsedTransaction(awaitingConfirm!!)
                    chatMessages = chatMessages + SharkChatMessage("Confirmed. Logged it.", isShark = true)
                    awaitingConfirm = null
                } else if (input.lowercase().contains("no") || input.lowercase().contains("cancel") || input.lowercase().contains("stop")) {
                    chatMessages = chatMessages + SharkChatMessage("Cancelled. What else?", isShark = true)
                    awaitingConfirm = null
                } else {
                    // Try to re-parse as a new command if they didn't say yes/no
                    val parsed   = AICoachNLP.parse(input, if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills, currentSession)
                    val response = AICoachResponse.generate(parsed, financialState)
                    chatMessages = chatMessages + SharkChatMessage(response.message, isShark = true)
                    if (response.askFollowUp != null) awaitingConfirm = parsed
                }
                isThinking = false
                return@launch
            }

            val parsed   = AICoachNLP.parse(input, if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills, currentSession)
            val response = AICoachResponse.generate(parsed, financialState)

            chatMessages = chatMessages + SharkChatMessage(response.message, isShark = true)
            isThinking = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            if (response.navigateTo != null && onFeatureClick != null) {
                onFeatureClick(response.navigateTo)
            }

            if (parsed.needsConfirm || response.askFollowUp != null) {
                awaitingConfirm = parsed
            }

            if (response.performReset) {
                performFullReset()
                currentSession = SharkAgentSession.AWAITING_SETUP_BALANCE
            } else if (currentSession == SharkAgentSession.AWAITING_SETUP_BALANCE && parsed.amount != null) {
                updateStartingBalance(parsed.amount)
                currentSession = SharkAgentSession.IDLE
            }

            if (parsed.intent == TransactionIntent.BILL_UPDATE && parsed.recurringKey != null && parsed.amount != null) {
                updateBillAmount(parsed.recurringKey, parsed.amount)
            }

            if (response.logTransaction && parsed.amount != null && !parsed.needsConfirm && currentSession == SharkAgentSession.IDLE && parsed.intent != TransactionIntent.BILL_UPDATE) {
                logParsedTransaction(parsed)
            }
        }
    }

    // ── Speech Recognition Setup ──
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { isListening = false }
        override fun onError(error: Int) {
            isListening = false
            liveTranscript = ""
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check emulator mic settings."
                SpeechRecognizer.ERROR_CLIENT -> "Client side error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing permissions."
                SpeechRecognizer.ERROR_NETWORK -> "Network error."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't hear that."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Shark is busy."
                SpeechRecognizer.ERROR_SERVER -> "Server error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                else -> "Error: $error"
            }
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) { processVoiceInput(matches[0]) }
            liveTranscript = ""
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) { liveTranscript = matches[0] }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { 
            isListening = true
            speechRecognizer.startListening(recognizerIntent) 
        } else {
            Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Voice recognition not supported on this emulator.", Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isListening = true
            speechRecognizer.startListening(recognizerIntent)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Streak ──
    var streak by remember { mutableStateOf(0) }
    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            streak = doc.getLong("streak")?.toInt() ?: 0
        }
    }

    val spentToday = expenses.filter { isToday(it.createdAtDate.time) && it.category != "Income" && it.category != "Passive Income" }.sumOf { it.amount }
    val incomeToday = if (incomeSources.isNotEmpty()) incomeSources.sumOf { it.amount } / 30.0 else income / 30.0
    val todayRemaining = (incomeToday - spentToday).coerceAtLeast(0.0)

    // Simulated S&P 500 performance vs User
    val userGrowth = if (income > 0) (balance / income) * 100 else 0.0
    val sp500Growth = 8.5 // Simulated YTD S&P 500 growth
    val beatingMarket = userGrowth > sp500Growth

    // ── Mood ──
    val sharkMood = when {
        isThinking                    -> SharkMood.CURIOUS
        runwayDays < 7                -> SharkMood.HUNGRY
        beatingMarket                 -> SharkMood.HAPPY
        todayRemaining <= 0           -> SharkMood.CONCERNED
        todayRemaining >= incomeToday * 0.8 -> SharkMood.HAPPY
        else                          -> SharkMood.NEUTRAL
    }

    // ── Lottie ──
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cute_shark_animation))
    var hasEntered by remember { mutableStateOf(false) }
    val entranceProgress by animateLottieCompositionAsState(composition = composition, isPlaying = !hasEntered, iterations = 1, clipSpec = LottieClipSpec.Frame(0, 24))
    LaunchedEffect(entranceProgress) { if (entranceProgress >= 1f) hasEntered = true }

    val moodClip = when (sharkMood) {
        SharkMood.HAPPY   -> LottieClipSpec.Frame(72, 96)
        SharkMood.SAD, SharkMood.HUNGRY -> LottieClipSpec.Frame(24, 72)
        SharkMood.NEUTRAL -> LottieClipSpec.Frame(96, 137)
        SharkMood.CURIOUS -> LottieClipSpec.Frame(137, 160)
        else              -> LottieClipSpec.Frame(96, 137)
    }
    val moodProgress by animateLottieCompositionAsState(composition = composition, isPlaying = hasEntered, iterations = LottieConstants.IterateForever, clipSpec = moodClip)
    val lottieProgress = if (!hasEntered) entranceProgress else moodProgress

    val arcColor = when (sharkMood) {
        SharkMood.HAPPY   -> SharkGreen
        SharkMood.NEUTRAL -> SharkAmber
        SharkMood.SAD, SharkMood.HUNGRY -> SharkRed
        SharkMood.CURIOUS -> SharkNavy
        else              -> SharkAmber
    }

    val targetBgColor = when (sharkMood) {
        SharkMood.HAPPY   -> Color(0xFF001208)
        SharkMood.SAD, SharkMood.HUNGRY -> Color(0xFF120000)
        SharkMood.CURIOUS -> Color(0xFF000812)
        SharkMood.CONCERNED -> Color(0xFF121200)
        else              -> SharkBlack
    }
    val animatedBgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(2000))

    val animatedArc by animateFloatAsState(targetValue = (score / 100f) * 300f, animationSpec = tween(1300, easing = EaseOutCubic))
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.06f, targetValue = 0.22f, animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse))
    val thinkingGlow by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse))

    // ── Bubbles ──
    var bubbles       by remember { mutableStateOf(listOf<FloatingBubble>()) }
    var editingBubble by remember { mutableStateOf<FloatingBubble?>(null) }
    var showIncomeInput by remember { mutableStateOf(false) }
    var showSpendInput  by remember { mutableStateOf(false) }
    var showSimpleGuide by remember { mutableStateOf(false) }
    var showWelcomeReminder by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        showWelcomeReminder = true
    }

    fun launchBubble(bubble: FloatingBubble) {
        bubbles = bubbles + bubble
        scope.launch {
            delay(3000)
            val b = bubbles.find { it.id == bubble.id } ?: return@launch
            db.collection("users").document(uid).collection("expenses").add(
                mapOf("title" to b.label.ifBlank { if (b.isIncome) "Income" else "Expense" }, "amount" to b.amount, "category" to if (b.isIncome) "Income" else "Other", "note" to "", "createdAt" to Date())
            )
            bubbles = bubbles.filter { it.id != bubble.id }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(animatedBgColor)) {
        // Simple Guide Button (Top Left)
        Box(modifier = Modifier.padding(16.dp).padding(top = 24.dp).align(Alignment.TopStart)) {
            IconButton(
                onClick = { showSimpleGuide = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.QuestionMark, null, tint = SharkMuted, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            
            // 1. Freedom Clock (Runway Metric)
            Box(modifier = Modifier.clickable { onFeatureClick?.invoke("Freedom Runway") }) {
                FreedomClock(runwayDays)
            }

            Spacer(Modifier.height(24.dp))

            // Shark + Aura
            Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(240.dp).scale(if (isThinking) thinkingGlow else 1f).background(Brush.radialGradient(listOf(arcColor.copy(alpha = glowAlpha), Color.Transparent))))
                Canvas(Modifier.size(220.dp)) {
                    drawCircle(Color.White.copy(alpha = 0.06f), size.minDimension / 2, style = Stroke(12f, cap = StrokeCap.Round))
                    drawArc(arcColor, -90f, animatedArc, false, style = Stroke(12f, cap = StrokeCap.Round))
                }
                LottieAnimation(composition = composition, progress = { lottieProgress }, modifier = Modifier.size(185.dp))
                
                // Sunglasses overlay if beating market
                if (beatingMarket && sharkMood == SharkMood.HAPPY) {
                    Text("🕶️", fontSize = 40.sp, modifier = Modifier.offset(y = (-10).dp))
                }
            }

            val dailyInsight = remember(expenses, bills) { AICoachResponse.generateDailyInsight(financialState) }
            Spacer(Modifier.height(12.dp))
            Text(text = dailyInsight, color = SharkMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(18.dp))

            DailyBudgetCard(firstName, accountType, incomeToday, todayRemaining, arcColor)

            Spacer(Modifier.height(24.dp))

            // 2. Passive Snowball (Income Tracker)
            PassiveSnowballCard(passiveIncome) { onFeatureClick?.invoke("Passive Snowball") }

            Spacer(Modifier.height(24.dp))

            // 3. Debt Vanish (Trajectory Engine)
            DebtVanishCard(totalDebt) { onFeatureClick?.invoke("Debt Vanish") }

            Spacer(Modifier.height(32.dp))

            // Voice Suggestions
            AnimatedVisibility(visible = !isListening && chatMessages.isEmpty()) {
                Row(modifier = Modifier.padding(bottom = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip("Dashed for 3 hours, made $80") { processVoiceInput("Dashed for 3 hours, made $80") }
                    SuggestionChip("Got $10 dividend") { processVoiceInput("Got $10 dividend") }
                    SuggestionChip("Paid $50 towards student loan") { processVoiceInput("Paid $50 towards student loan") }
                    SuggestionChip("Spent $12 at Starbucks") { processVoiceInput("Spent $12 at Starbucks") }
                }
            }

            // Real-time Transcript Pill
            AnimatedVisibility(visible = isListening || liveTranscript.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(modifier = Modifier.padding(bottom = 24.dp).glassCard(cornerRadius = 24f, alpha = 0.12f).padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoiceWaveformUI()
                        Spacer(Modifier.width(12.dp))
                        Text(text = if (liveTranscript.isEmpty()) "Listening..." else liveTranscript, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Main Controls Area with Smart Input Toggle
            AnimatedContent(
                targetState = inputMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "InputModeSwitch"
            ) { mode ->
                if (mode is InputMode.Voice) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ControlButton(Icons.Default.Add, SharkGreen, "Income") { showIncomeInput = true }
                        }

                        // Mic Button
                        val micScale by animateFloatAsState(targetValue = if (isListening) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(64.dp).scale(micScale).clip(CircleShape).background(if (isListening) SharkNavy else Color(0xFF111111)).border(2.dp, if (isListening) SharkNavy.copy(alpha = 0.5f) else Color(0xFF2A2A2A), CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { inputMode = InputMode.Text },
                                            onPress = {
                                                startListening()
                                                tryAwaitRelease()
                                                if (isListening) { speechRecognizer.stopListening(); isListening = false; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (isListening) Icons.Default.MicNone else Icons.Default.Mic, null, tint = if (isListening) Color.Black else SharkNavy, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(if (isListening) "HOLDING" else "SHARK", color = if (isListening) SharkNavy else SharkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ControlButton(Icons.Default.Remove, SharkRed, "Spent") { showSpendInput = true }
                        }
                    }
                } else {
                    // Text Mode UI
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                        TextInputArea(
                            value = textInputVal,
                            onValueChange = { textInputVal = it },
                            onReturnToVoice = { inputMode = InputMode.Voice },
                            onSend = {
                                if (textInputVal.isNotBlank()) {
                                    processVoiceInput(textInputVal.trim())
                                    textInputVal = ""
                                    // Persistent: stay in Text mode
                                }
                            }
                        )
                    }
                }
            }

            // Chat & Thinking
            if (chatMessages.isNotEmpty() || isThinking) {
                Spacer(Modifier.height(28.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chatMessages.takeLast(4).forEach { msg ->
                        AnimatedVisibility(visible = true, enter = slideInHorizontally { if (msg.isShark) -50 else 50 } + fadeIn()) {
                            SharkChatBubble(message = msg.text, isShark = msg.isShark)
                        }
                    }
                    if (isThinking) { TypingIndicatorUI() }
                }
            }

            Spacer(Modifier.height(32.dp))
            QuickFeatureRow(onFeatureClick)
            Spacer(Modifier.height(32.dp))
            if (expenses.isNotEmpty()) RecentActivitySection(expenses.take(5), onExpenseClick)
            Spacer(Modifier.height(120.dp))
        }

        bubbles.forEach { bubble -> FloatingBubbleView(bubble = bubble, onTap = { editingBubble = bubble }) }
        if (showWelcomeReminder) {
            AlertDialog(
                onDismissRequest = { showWelcomeReminder = false },
                containerColor = SharkSurface,
                title = { Text("Daily Check-in", color = Color.White) },
                text = { Text("Ready to update your progress? Log any income or spending from today to keep your score accurate.", color = SharkMuted) },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showWelcomeReminder = false; showIncomeInput = true }, colors = ButtonDefaults.buttonColors(containerColor = SharkGreen)) {
                            Text("Log Income", color = Color.Black)
                        }
                        Button(onClick = { showWelcomeReminder = false; showSpendInput = true }, colors = ButtonDefaults.buttonColors(containerColor = SharkRed)) {
                            Text("Log Spent", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWelcomeReminder = false }) { Text("Later", color = SharkMuted) }
                }
            )
        }
        if (showIncomeInput) QuickAmountDialog(true, { showIncomeInput = false; if (it > 0) launchBubble(FloatingBubble(System.currentTimeMillis(), it, true)) }, { showIncomeInput = false })
        if (showSpendInput) QuickAmountDialog(false, { showSpendInput = false; if (it > 0) launchBubble(FloatingBubble(System.currentTimeMillis(), it, false)) }, { showSpendInput = false })
        
        if (showSimpleGuide) {
            SimpleHowToDialog(onDismiss = { showSimpleGuide = false })
        }
    }
}

@Composable
fun SimpleHowToDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SharkNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = SharkNavy, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Using Sharkfin is Simple", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                GuideStep(
                    icon = "🎙️",
                    title = "Just Talk to Shark",
                    desc = "Hold the middle button and say \"Spent $12 at Starbucks\" or \"I made $100 today.\""
                )
                GuideStep(
                    icon = "📉",
                    title = "Watch Your Score",
                    desc = "Log everything to see your Money Score go up. A higher score means more financial freedom."
                )
                GuideStep(
                    icon = "❄️",
                    title = "Grow Your Snowball",
                    desc = "Track passive income. When it covers your bills, you've won the game."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SharkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Got it!", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun GuideStep(icon: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = SharkMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun FreedomClock(days: Int) {
    val glowColor = if (days > 30) SharkGreen else if (days > 7) SharkAmber else SharkRed
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("FREEDOM RUNWAY", color = SharkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .glassCard(cornerRadius = 20f, alpha = 0.1f)
                .padding(horizontal = 32.dp, vertical = 12.dp)
                .border(1.dp, glowColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You are safe for $days days",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.drawBehind {
                    drawCircle(glowColor.copy(alpha = 0.1f), radius = size.minDimension)
                }
            )
        }
    }
}

@Composable
fun PassiveSnowballCard(amount: Double, onClick: () -> Unit) {
    var projectionAmount by remember { mutableStateOf(amount.toFloat()) }
    
    val level = when {
        projectionAmount >= 100 -> 3
        projectionAmount >= 50  -> 2
        projectionAmount >= 10  -> 1
        else -> 0
    }
    
    val rewardText = when {
        projectionAmount >= 500 -> "You are officially financially independent. 🏆"
        projectionAmount >= 100 -> "Dividends pay for your car insurance & more."
        projectionAmount >= 50  -> "Dividends now cover your monthly phone bill."
        projectionAmount >= 10  -> "Your dividends now pay for Netflix."
        else -> "Build your snowball to cover monthly bills."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24f, alpha = 0.08f)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                    val sizeScale = 0.5f + (level * 0.2f)
                    val infiniteTransition = rememberInfiniteTransition()
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
                    )
                    
                    Box(modifier = Modifier
                        .size((50 * sizeScale).dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Text("❄️", fontSize = (24 * sizeScale).sp, modifier = Modifier.scale(if (projectionAmount > amount.toFloat()) 1.1f else 1.0f))
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("PASSIVE SNOWBALL", color = SharkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("$${String.format(Locale.US, "%.2f", projectionAmount)}", color = if (projectionAmount > amount) SharkGreen else Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(rewardText, color = SharkGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("FREEDOM CALCULATOR", color = SharkMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                if (projectionAmount != amount.toFloat()) {
                    Text("PROJECTION", color = SharkNavy, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Slider(
                value = projectionAmount,
                onValueChange = { projectionAmount = it },
                valueRange = 0f..500f,
                colors = SliderDefaults.colors(
                    thumbColor = SharkNavy,
                    activeTrackColor = SharkNavy,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
            if (projectionAmount != amount.toFloat()) {
                Text(
                    "Tap to set this as a goal",
                    color = SharkMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DebtVanishCard(totalDebt: Double, onClick: () -> Unit) {
    val initialDebt = 10000.0 // Simulated initial debt
    val progress = (1.0 - (totalDebt / initialDebt)).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1500, easing = EaseOutCubic))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24f, alpha = 0.08f)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("DEBT VANISH", color = SharkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("$${String.format(Locale.US, "%,.0f", totalDebt)}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Box(contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
                    )
                    Icon(
                        Icons.Default.Waves, 
                        null, 
                        tint = SharkRed, 
                        modifier = Modifier.size(32.dp).scale(pulse)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).background(Brush.horizontalGradient(listOf(SharkRed, SharkAmber)), CircleShape))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trajectory: Paid in full by Dec 2025", color = SharkMuted, fontSize = 11.sp)
                Text("View Breakdown →", color = SharkRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VoiceWaveformUI() {
    val infiniteTransition = rememberInfiniteTransition()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            val height by infiniteTransition.animateFloat(initialValue = 4f, targetValue = 16f, animationSpec = infiniteRepeatable(tween(400 + (i * 100), easing = LinearEasing), RepeatMode.Reverse))
            Box(Modifier.size(width = 3.dp, height = height.dp).background(SharkNavy, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun TypingIndicatorUI() {
    Row(modifier = Modifier.padding(start = 36.dp).glassCard(cornerRadius = 12f, alpha = 0.08f).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val infiniteTransition = rememberInfiniteTransition()
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 200), RepeatMode.Reverse))
            Box(Modifier.size(6.dp).alpha(alpha).background(SharkMuted, CircleShape))
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = SharkMuted, fontSize = 11.sp)
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(color.copy(alpha = 0.08f)).border(1.5.dp, color.copy(alpha = 0.3f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
    }
    Spacer(Modifier.height(6.dp))
    Text(label, color = SharkMuted, fontSize = 11.sp)
}

@Composable
fun TextInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    onReturnToVoice: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onReturnToVoice() })
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Return to Voice button
        IconButton(onClick = onReturnToVoice, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Mic, contentDescription = "Return to Voice", tint = SharkNavy)
        }
        Spacer(Modifier.width(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
            placeholder = { Text("Tell Shark...", color = SharkMuted, fontSize = 13.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF111111),
                unfocusedContainerColor = Color(0xFF111111),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = SharkNavy
            ),
            maxLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (value.isNotBlank()) SharkNavy else Color(0xFF111111))
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (value.isNotBlank()) Color.Black else SharkMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DailyBudgetCard(firstName: String, accountType: String, dailyBudget: Double, todayRemaining: Double, arcColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0D1F12)).border(1.dp, arcColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 14.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text(firstName.uppercase(), color = SharkMuted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(accountType, color = SharkMuted.copy(alpha = 0.7f), fontSize = 9.sp)
                }
                Text("DAILY BUDGET", color = SharkMuted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Text("$${String.format(Locale.US, "%.2f", todayRemaining)}", color = arcColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("of $${String.format(Locale.US, "%.0f", dailyBudget)}", color = SharkMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun QuickFeatureRow(onFeatureClick: ((String) -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickFeatureCard("Visual Models", Icons.AutoMirrored.Filled.TrendingUp, SharkNavy, Modifier.weight(1f)) { onFeatureClick?.invoke("Visual Models") }
        QuickFeatureCard("Bill Tracker", Icons.AutoMirrored.Filled.TrendingUp, SharkAmber, Modifier.weight(1f)) { onFeatureClick?.invoke("Bill Tracker") }
    }
}

@Composable
private fun QuickFeatureCard(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(60.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).clickable { onClick() }.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
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
        expenses.forEach { FlatTransactionRow(it, onExpenseClick) }
    }
}

@Composable
private fun FloatingBubbleView(bubble: FloatingBubble, onTap: () -> Unit) {
    val offsetY = remember { Animatable(640f) }
    LaunchedEffect(bubble.id) { offsetY.animateTo(180f, tween(2800, easing = EaseOutCubic)) }
    val color = if (bubble.isIncome) SharkGreen else Color.White
    Box(Modifier.fillMaxSize().offset { IntOffset(0, offsetY.value.roundToInt()) }, Alignment.Center) {
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).clickable { onTap() }.padding(horizontal = 18.dp, vertical = 9.dp)) {
            Text("${if (bubble.isIncome) "+" else "-"}$${bubble.amount.roundToInt()}", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickAmountDialog(isIncome: Boolean, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val color = if (isIncome) SharkNavy else SharkRed
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF111111), title = { Text(if (isIncome) "Amount received" else "Amount spent", color = Color.White) }, text = { OutlinedTextField(value = text, onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = { Text("0.00", color = SharkMuted) }, prefix = { Text("$", color = color) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = color, unfocusedBorderColor = SharkMuted)) }, confirmButton = { TextButton(onClick = { onConfirm(text.toDoubleOrNull() ?: 0.0) }) { Text("Log it", color = color) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SharkMuted) } })
}

fun calcMoneyScore(income: Double, spent: Double): Int {
    if (income <= 0) return 50
    val ratio = spent / income
    val savingsRate = (income - spent) / income * 100
    var score = 50
    score += when { savingsRate >= 20 -> 40; savingsRate >= 10 -> 25; savingsRate >= 0 -> 10; else -> -25 }
    score -= when { ratio > 1.2 -> 20; ratio > 1.0 -> 12; ratio > 0.9 -> 5; else -> 0 }
    return score.coerceIn(0, 100)
}

@Composable
fun FlatTransactionRow(expense: Expense, onClick: (Expense) -> Unit) {
    val isIncome = expense.category == "Income" || expense.category == "1099 Income" || expense.category == "Passive Income"
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(expense) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Color.White.copy(alpha = 0.06f), CircleShape), Alignment.Center) {
            Text(expense.category.take(1).uppercase(), color = if (isIncome) SharkNavy else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(expense.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(expense.category, color = SharkMuted, fontSize = 11.sp)
        }
        Text("${if (isIncome) "+" else "-"}$${String.format(Locale.US, "%.2f", expense.amount)}", color = if (isIncome) SharkNavy else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
