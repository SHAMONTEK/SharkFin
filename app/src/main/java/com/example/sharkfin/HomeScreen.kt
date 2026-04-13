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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

// ── Tile Model ───────────────────────────────────────────────────────────────
data class EnvironmentTile(
    val id          : String,
    val name        : String,
    val icon        : ImageVector,
    val accentColor : Color,
    val iconBg      : Color,
    val available   : Boolean,
    val tag         : String  = "",
    val tagBg       : Color   = Color.Transparent,
    val tagText     : Color   = Color.Transparent,
    val liveLabel   : String  = "",
    val liveValue   : String  = "—",
    val liveSubValue: String  = "",
    val liveSubColor: Color   = SharkSecondary,
    val trendPoints : List<Float> = emptyList()
)

@Composable
fun HomeScreen(
    uid             : String,
    db              : com.google.firebase.firestore.FirebaseFirestore,
    displayName     : String,
    accountType     : String,
    expenses        : List<Expense>,
    incomeSources   : List<IncomeSource>,
    bills           : List<Bill>          = emptyList(),
    goals           : List<Goal>          = emptyList(),
    debts           : List<Debt>          = emptyList(),
    recurringBills  : List<RecurringBill> = emptyList(),
    discoveryData   : Map<String, Any>?   = null,
    onFeatureClick  : (String) -> Unit
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val haptic    = LocalHapticFeedback.current
    val incomeCategories = SharkIncomeCategories

    // ── FINANCIAL DATA PREP ───────────────────────────────────────────────────
    fun isThisMonth(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { time = date }
        return cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    val wizardIncome = (discoveryData?.get("monthlyIncome") as? Double) ?: 0.0
    val wizardObligations = (discoveryData?.get("monthlyObligations") as? Double) ?: 0.0
    
    // Better Income Logic: Max of Planned Sources, Wizard Input, or Actual Logged Income this month
    val loggedMonthlyIncome = expenses.filter { isThisMonth(it.createdAtDate) && it.category in incomeCategories }.sumOf { it.amount }
    val totalIncome = listOf(incomeSources.sumOf { it.amount }, wizardIncome, loggedMonthlyIncome).maxOrNull() ?: 0.0
    
    val totalSpent = expenses.filter { it.category !in incomeCategories }.sumOf { it.amount }
    val totalIncomeAllTime = expenses.filter { it.category in incomeCategories }.sumOf { it.amount }
    
    // If no income has ever been logged, but the user says they have a balance, 
    // we should trust the Wizard's implied balance or at least show what's there.
    val balance = if (totalIncomeAllTime > 0 || totalSpent > 0) {
        totalIncomeAllTime - totalSpent
    } else {
        wizardIncome // Fallback to wizard income if no transactions exist yet
    }

    fun isToday(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { time = date }
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    val spentToday = expenses.filter { isToday(it.createdAtDate) && it.category !in incomeCategories }.sumOf { it.amount }
    val dailyBudget = if (totalIncome > 0) totalIncome / 30.0 else (balance / 30.0).coerceAtLeast(0.0)
    val totalObligations = bills.sumOf { it.amount } + debts.sumOf { it.minimumPayment } + recurringBills.sumOf { it.amount }
    val maxObligations = totalObligations.coerceAtLeast(wizardObligations)

    // Protection logic: If user has a large balance but no income, we allow spending based on balance runway
    val spendableTodayMax = if (totalIncome > 0) {
        ((dailyBudget - (maxObligations / 30.0)) * 0.20).coerceAtLeast(5.0) // Floor of $5 if they have income
    } else if (balance > 0) {
        (balance / 30.0) * 0.10 // 10% of daily balance if no monthly income set
    } else 0.0

    val spendableLeft = (spendableTodayMax - spentToday).coerceAtLeast(0.0)
    val passiveIncome = expenses.filter { it.category == "Passive Income" }.sumOf { it.amount }
    val avgDailyBurn = if (expenses.isNotEmpty() && totalSpent > 0) totalSpent / 30.0 else (maxObligations / 30.0).coerceAtLeast(10.0)

    val score = calcMoneyScore(
        income = totalIncome, spent = totalSpent, bills = bills,
        goals = goals, debts = debts, avgDailyBurn = avgDailyBurn,
        balance = balance, passiveIncome = passiveIncome
    )

    val firstName = displayName.split(" ").firstOrNull()?.ifBlank { "Shark" } ?: "Shark"

    // ── AI AGENT STATE ───────────────────────────────────────────────────────
    val sharkAssistant = remember {
        SharkAssistant(
            apiKey = "AIzaSyAPeEbQEPyln6zSP3irUtOPM3XjjpmuIY0",
            uid = uid,
            db = db
        )
    }

    var chatOpen by remember { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf(listOf<SharkChatMessage>()) }
    var isListening by remember { mutableStateOf(false) }
    var liveTranscript by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var textInputVal by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val financialState = SharkFinancialState(
        dailyBudget = dailyBudget,
        dailySpentSoFar = spentToday,
        currentStreak = 0,
        goalPercent = 50,
        balance = balance,
        moneyScore = score,
        paydayInDays = null,
        knownRecurring = if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills,
        upcomingBills = bills.filter { !it.isPaid },
        activeGoals = goals.filter { !it.isCompleted },
        expenses = expenses,
        averageDailyBurn = avgDailyBurn,
        totalDebt = debts.sumOf { it.currentBalance }
    )

    LaunchedEffect(financialState) {
        sharkAssistant.updateSystemContext(financialState)
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    fun processChatInput(input: String) {
        if (input.isBlank()) return
        val currentInput = input.trim()
        textInputVal = "" // Clear immediately to prevent double-sends
        
        scope.launch {
            isThinking = true
            chatMessages = chatMessages + SharkChatMessage(currentInput, isShark = false)

            try {
                sharkAssistant.processInput(currentInput, financialState).collect { response ->
                    chatMessages = chatMessages + SharkChatMessage(response.message, isShark = true)
                    if (response.navigateTo != null) onFeatureClick(response.navigateTo)
                }
            } catch (e: Exception) {
                chatMessages = chatMessages + SharkChatMessage("Turbulence in the deep. Try that again.", isShark = true)
            } finally {
                isThinking = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // ── SPEECH RECOGNITION SETUP ─────────────────────────────────────────────
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { 
            isListening = true 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        override fun onBeginningOfSpeech() { }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { 
            isListening = false 
        }
        override fun onError(error: Int) { 
            isListening = false 
            liveTranscript = ""
            // Handle error silently or with a subtle vibration
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) { 
                val text = matches[0]
                processChatInput(text) 
            }
            liveTranscript = ""
            isListening = false
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) { 
                liveTranscript = matches[0] 
                // Don't overwrite the user's manual typing if they are in the middle of it
                if (textInputVal.isEmpty() || isListening) {
                    textInputVal = matches[0]
                }
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { 
            speechRecognizer.startListening(recognizerIntent) 
        } else {
            Toast.makeText(context, "Microphone permission needed for Sharkie", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleListening() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                // Add a 200ms delay to avoid picking up the tap sound ("first/second taps")
                scope.launch {
                    delay(250)
                    speechRecognizer.startListening(recognizerIntent)
                }
            }
        }
    }

    // ── SHARK MOOD & LOTTIE ──────────────────────────────────────────────────
    val runwayDays = if (avgDailyBurn > 0) (balance / avgDailyBurn).toInt().coerceAtLeast(0) else 999
    val sharkMood = when {
        isThinking      -> SharkMood.CURIOUS
        runwayDays < 7  -> SharkMood.HUNGRY
        score >= 70     -> SharkMood.HAPPY
        spendableLeft <= 0 -> SharkMood.CONCERNED
        else            -> SharkMood.NEUTRAL
    }

    var showProfileSettings by remember { mutableStateOf(false) }
    var tempDisplayName by remember { mutableStateOf(displayName) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cute_shark_animation))
    val moodClip = when (sharkMood) {
        SharkMood.HAPPY   -> LottieClipSpec.Frame(72, 96)
        SharkMood.SAD, SharkMood.HUNGRY -> LottieClipSpec.Frame(24, 72)
        SharkMood.NEUTRAL -> LottieClipSpec.Frame(96, 137)
        SharkMood.CURIOUS -> LottieClipSpec.Frame(137, 160)
        else              -> LottieClipSpec.Frame(96, 137)
    }
    val lottieProgress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever, clipSpec = moodClip)

    // ── TOOLKIT TILES ────────────────────────────────────────────────────────
    val nextBill = bills.filter { !it.isPaid }.minByOrNull { it.dayOfMonth }
    val topGoal = goals.filter { !it.isCompleted }.maxByOrNull { if(it.targetAmount > 0) it.savedAmount/it.targetAmount else 0.0 }
    val savingsRate = if(totalIncome > 0) ((totalIncome - totalSpent) / totalIncome * 100).toInt() else 0

    val toolkitTiles = listOf(
        EnvironmentTile("Expense Tracker", "Expense Tracker", Icons.Default.AccountBalanceWallet, SharkGold, SharkGoldGlow, true, liveLabel = "TODAY'S SPEND", liveValue = "$${String.format(Locale.US, "%.2f", spentToday)}", liveSubValue = if(spentToday > spendableTodayMax) "Over limit" else "On track", trendPoints = listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.6f, 0.8f)),
        EnvironmentTile("Import Statement", "Import Statement", Icons.Default.FileUpload, SharkGold, SharkGoldGlow, true, tag = "NEW", tagBg = SharkGoldGlow, tagText = SharkGold, liveLabel = "LAST IMPORT", liveValue = "None yet", trendPoints = listOf(0.1f, 0.1f, 0.5f, 0.1f, 0.9f)),
        EnvironmentTile("Bill Tracker", "Bill Tracker", Icons.Default.Receipt, SharkAmber, Color(0x1AEE944B), true, liveLabel = "NEXT DUE", liveValue = nextBill?.let { "$${it.amount.toInt()}" } ?: "All clear", liveSubValue = nextBill?.let { "${it.name} · ${ordinal(it.dayOfMonth)}" } ?: "No bills", trendPoints = listOf(0.2f, 0.8f, 0.1f, 0.9f, 0.3f)),
        EnvironmentTile("Goal Tracker", "Goal Tracker", Icons.Default.Flag, SharkTeal, Color(0x1A4BA3E8), true, liveLabel = "TOP GOAL", liveValue = topGoal?.let { "${(it.savedAmount/it.targetAmount*100).toInt()}%" } ?: "Set goal", liveSubValue = topGoal?.name ?: "No active goals", trendPoints = listOf(0.1f, 0.2f, 0.4f, 0.7f, 0.85f)),
        EnvironmentTile("Visual Models", "Visual Models", Icons.Default.BarChart, SharkPurple, Color(0x1AAF52DE), true, liveLabel = "SAVINGS RATE", liveValue = "$savingsRate%", liveSubValue = "vs 20% target", trendPoints = listOf(0.5f, 0.6f, 0.4f, 0.8f, 0.9f)),
        EnvironmentTile("Tax Tracker", "Tax Tracker", Icons.Default.Description, SharkRed, Color(0x1AE05C5C), true, tag = "BETA", tagBg = Color(0x1AE05C5C), tagText = SharkRed, liveLabel = "EST. OWED", liveValue = "$${(totalIncome * 0.22).toInt()}", trendPoints = listOf(0.8f, 0.8f, 0.8f, 0.8f, 0.8f)),
        EnvironmentTile("Dividend Tracker", "Dividend Tracker", Icons.Default.Payments, SharkGold, SharkGoldGlow, true, tag = "NEW", tagBg = SharkGoldGlow, tagText = SharkGold, liveLabel = "PASSIVE / MO", liveValue = "$${String.format(Locale.US, "%.2f", passiveIncome)}", trendPoints = listOf(0.1f, 0.25f, 0.4f, 0.6f, 0.8f)),
        EnvironmentTile("Freedom Runway", "Freedom Runway", Icons.Default.FlightTakeoff, SharkGold, SharkGoldGlow, true, liveLabel = "RUNWAY", liveValue = if(runwayDays < 999) "$runwayDays days" else "Infinite", trendPoints = listOf(0.2f, 0.3f, 0.5f, 0.6f, 0.7f)),
        EnvironmentTile("Settings", "Settings", Icons.Default.Settings, SharkSecondary, SharkSurface, true, liveLabel = "PROFILE", liveValue = firstName, liveSubValue = accountType)
    )

    fun onTileClick(id: String) {
        if (id == "Settings") {
            showProfileSettings = true
        } else {
            onFeatureClick(id)
        }
    }

    val scoreColor = when { score >= 70 -> SharkGold; score >= 40 -> SharkAmber; else -> SharkRed }
    val animatedScore by animateFloatAsState(score.toFloat(), tween(1000), label = "score")
    val arcSweep by animateFloatAsState((score / 100f) * 240f, tween(1200, easing = EaseOutCubic), label = "arc")

    Box(Modifier.fillMaxSize().background(SharkBg)) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 100.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 60.dp, bottom = 10.dp)) {
                        Text("SHARKFIN: FINANCE PORTFOLIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = SharkGold.copy(0.6f))
                        Text("Hey, $firstName", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SharkLabel, letterSpacing = (-0.5).sp)

                        Spacer(Modifier.height(16.dp))
                        // ── NEW TEXT INPUT ENTRY BAR ──
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(SharkSurface)
                                .border(0.5.dp, SharkCardBorder, RoundedCornerShape(25.dp))
                                .clickable { 
                                    chatOpen = true 
                                    scope.launch { 
                                        delay(300)
                                        focusRequester.requestFocus() 
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = SharkGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Ask Sharkie anything...", color = SharkTertiary, fontSize = 14.sp)
                            }
                        }
                    }
                }

                item {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SharkCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                        Canvas(Modifier.fillMaxSize()) {
                                            drawArc(scoreColor.copy(0.1f), 150f, 240f, false, style = Stroke(6f, cap = StrokeCap.Round))
                                            drawArc(scoreColor, 150f, arcSweep, false, style = Stroke(6f, cap = StrokeCap.Round))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${animatedScore.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                                            Text("SCORE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SharkSecondary)
                                        }
                                    }
                                    Column {
                                        Text("Money health", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
                                        Text(if(score >= 70) "Excellent" else "Watch spend", fontSize = 11.sp, color = SharkSecondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("BALANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SharkSecondary, letterSpacing = 1.sp)
                                    Text("$${String.format(Locale.US, "%,.2f", balance)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = SharkBg, thickness = 1.dp)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                                Column {
                                    Text("SPENDABLE TODAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SharkSecondary)
                                    Text("$${String.format(Locale.US, "%,.2f", spendableLeft)}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if(spendableLeft > 0) SharkGold else SharkRed)
                                    Text("keeps 80% after bills", fontSize = 10.sp, color = SharkTertiary)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetMiniBar("IN", 1f, SharkGold, "$${totalIncome.toInt()}")
                                    WidgetMiniBar("OUT", (totalSpent/totalIncome.coerceAtLeast(1.0)).toFloat(), SharkAmber, "$${totalSpent.toInt()}")
                                }
                            }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("YOUR TOOLKIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharkSecondary, letterSpacing = 1.5.sp)
                        Text("See all", fontSize = 13.sp, color = SharkGold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onFeatureClick("Features") })
                    }
                }

                items(toolkitTiles.chunked(2)) { pair ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), Arrangement.spacedBy(12.dp)) {
                        pair.forEach { tile ->
                            ToolkitTile(tile, Modifier.weight(1f)) { if(tile.available) onTileClick(tile.id) }
                        }
                        if(pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

    val suggestedPrompts = listOf(
        "Summarize my recent imports",
        "When is my next bill due?",
        "Can I afford to spend $50?",
        "How do I improve my score?",
        "How much spent on food today?",
        "Analyze my spending trajectory"
    )

    // ── CHAT OVERLAY ─────────────────────────────────────────────────────
    AnimatedVisibility(
        chatOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(SharkBg.copy(0.95f))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp)
            ) {
                // Sophisticated Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SharkSurface)
                                .border(1.dp, SharkGold.copy(0.3f), CircleShape),
                            Alignment.Center
                        ) {
                            LottieAnimation(composition, { lottieProgress }, modifier = Modifier.size(44.dp))
                        }
                        Column {
                            Text("Sharkie AI", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = SharkLabel, letterSpacing = (-0.5).sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(6.dp).background(if(isThinking || isListening) SharkGold else Color(0xFF4CAF50), CircleShape))
                                Text(
                                    if(isListening) "Listening..." else if (isThinking) "Calculating..." else "Online",
                                    color = if(isListening) SharkGold else SharkSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { chatOpen = false },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SharkSurface)
                    ) {
                        Icon(Icons.Default.Close, null, tint = SharkLabel, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = SharkCardBorder.copy(0.5f), thickness = 0.5.dp)
                
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp), 
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
                                Text(
                                    "THE DEEP KNOWS ALL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SharkGold.copy(0.4f),
                                    letterSpacing = 3.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "How can I sharpen your finances today?",
                                    color = SharkLabel,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(32.dp))
                                
                                Text("SUGGESTED PROMPTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SharkTertiary, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(16.dp))
                                
                                suggestedPrompts.forEach { prompt ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(SharkSurface)
                                            .border(1.dp, SharkCardBorder, RoundedCornerShape(16.dp))
                                            .clickable { 
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                processChatInput(prompt) 
                                            }
                                            .padding(18.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Bolt, null, tint = SharkGold, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text(prompt, color = SharkLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(chatMessages) { msg ->
                            ChatBubble(msg)
                        }
                    }
                }

                // Input Bar Container with Glassmorphism / Glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 120.dp) // Raised even higher as requested
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(24.dp, RoundedCornerShape(30.dp), spotColor = SharkGold),
                        color = SharkSurface.copy(0.9f),
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.5.dp, Brush.sweepGradient(listOf(SharkGold, SharkAmber, SharkGold)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = textInputVal,
                                onValueChange = { textInputVal = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                placeholder = { Text("Command Sharkie...", color = SharkTertiary, fontSize = 15.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = SharkLabel,
                                    unfocusedTextColor = SharkLabel,
                                    cursorColor = SharkGold
                                ),
                                singleLine = false,
                                maxLines = 4,
                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send,
                                    capitalization = KeyboardCapitalization.Sentences,
                                    keyboardType = KeyboardType.Text
                                ),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (textInputVal.isNotBlank()) {
                                        val capturedText = textInputVal
                                        textInputVal = ""
                                        processChatInput(capturedText)
                                    }
                                })
                            )

                            // Unified Action Button with Robust Effect
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = if (isListening) Brush.verticalGradient(listOf(SharkRed, Color(0xFF990000)))
                                                else if (textInputVal.isNotBlank()) Brush.verticalGradient(listOf(SharkGold, SharkAmber))
                                                else Brush.verticalGradient(listOf(SharkSurfaceHigh, SharkSurface))
                                    )
                                    .border(1.dp, if(isListening || textInputVal.isNotBlank()) SharkWhite.copy(0.3f) else SharkCardBorder, CircleShape)
                                    .clickable {
                                        if (textInputVal.isNotBlank()) {
                                            val capturedText = textInputVal
                                            textInputVal = ""
                                            processChatInput(capturedText)
                                        } else {
                                            toggleListening()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isThinking) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SharkBg, strokeWidth = 3.dp)
                                } else {
                                    val icon = when {
                                        textInputVal.isNotBlank() -> Icons.AutoMirrored.Filled.Send
                                        isListening -> Icons.Default.Stop
                                        else -> Icons.Default.Mic
                                    }
                                    Icon(
                                        icon, 
                                        null, 
                                        tint = if (textInputVal.isNotBlank() || isListening) SharkBg else SharkGold, 
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PROFILE SETTINGS MODAL
    if (showProfileSettings) {
        AlertDialog(
            onDismissRequest = { showProfileSettings = false },
            containerColor = SharkSurface,
            title = { Text("Profile Settings", color = SharkLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Change your display name so Sharkie knows who to bark at.", color = SharkSecondary, fontSize = 14.sp)
                    OutlinedTextField(
                        value = tempDisplayName,
                        onValueChange = { tempDisplayName = it },
                        label = { Text("Display Name", color = SharkGold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SharkGold,
                            unfocusedBorderColor = SharkCardBorder,
                            focusedTextColor = SharkLabel,
                            unfocusedTextColor = SharkLabel
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    db.collection("users").document(uid).update("displayName", tempDisplayName)
                    showProfileSettings = false 
                }) {
                    Text("SAVE", color = SharkGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSettings = false }) {
                    Text("CANCEL", color = SharkSecondary)
                }
            }
        )
    }

        // BOTTOM NAV
        Box(Modifier.align(Alignment.BottomCenter)) {
            SFBottomNav(
                onMicTap = { 
                    if (!chatOpen) chatOpen = true
                    toggleListening() 
                },
                onMicHold = { 
                    if (!chatOpen) chatOpen = true
                    toggleListening() 
                },
                isListening = isListening,
                micScale = 1f,
                onFeatureClick = { feature ->
                    chatOpen = false
                    onFeatureClick(feature)
                },
                onHomeClick = {
                    chatOpen = false
                }
            )
        }
    }
}

@Composable
fun ChatBubble(msg: SharkChatMessage) {
    val isShark = msg.isShark
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + 
                slideInHorizontally(initialOffsetX = { if (isShark) -20 else 20 }) +
                expandVertically(expandFrom = Alignment.Top),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp), 
            horizontalAlignment = if(isShark) Alignment.Start else Alignment.End
        ) {
            if (isShark) {
                Row(
                    verticalAlignment = Alignment.Top, 
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SharkSurface)
                            .border(1.dp, SharkGold.copy(0.4f), CircleShape),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.Waves, null, tint = SharkGold, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(0.dp, 20.dp, 20.dp, 20.dp))
                                .background(SharkSurfaceHigh)
                                .shadow(8.dp, RoundedCornerShape(0.dp, 20.dp, 20.dp, 20.dp), spotColor = SharkGold.copy(0.2f))
                                .border(0.5.dp, SharkCardBorder, RoundedCornerShape(0.dp, 20.dp, 20.dp, 20.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                msg.text, 
                                color = SharkLabel, 
                                fontSize = 15.sp, 
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            SimpleDateFormat("h:mm a", Locale.US).format(Date(msg.timestamp)),
                            fontSize = 9.sp,
                            color = SharkTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth(0.85f).align(Alignment.End)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp, 20.dp, 0.dp, 20.dp))
                            .background(Brush.linearGradient(listOf(SharkGold, SharkAmber)))
                            .border(0.5.dp, SharkWhite.copy(0.2f), RoundedCornerShape(20.dp, 20.dp, 0.dp, 20.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            msg.text, 
                            color = SharkBg, 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.Bold, 
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Delivered",
                        fontSize = 9.sp,
                        color = SharkTertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ToolkitTile(tile: EnvironmentTile, modifier: Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, label = "tile_scale")

    Box(
        modifier = modifier
            .alpha(if (tile.available) 1f else 0.4f)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(SharkSurface)
            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
                )
            }
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(tile.accentColor))
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tile.iconBg), Alignment.Center) {
                    Icon(tile.icon, null, tint = tile.accentColor, modifier = Modifier.size(20.dp))
                }
                if(tile.tag.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(tile.tagBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tile.tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = tile.tagText)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SharkLabel, modifier = Modifier.weight(1f))
                if (tile.trendPoints.isNotEmpty()) MiniTrendLine(tile.trendPoints, tile.accentColor, Modifier.size(38.dp, 16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SharkBg.copy(0.5f)).padding(8.dp)) {
                Column {
                    Text(tile.liveLabel, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SharkSecondary)
                    Text(tile.liveValue, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
                    if(tile.liveSubValue.isNotEmpty()) Text(tile.liveSubValue, fontSize = 10.sp, color = SharkSecondary)
                }
            }
        }
    }
}

@Composable
fun MiniTrendLine(points: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i * (width / (points.size - 1))
            val y = height - (p * height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun WidgetMiniBar(label: String, fill: Float, color: Color, amount: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SharkSecondary, modifier = Modifier.width(20.dp))
        Box(Modifier.width(80.dp).height(6.dp).clip(CircleShape).background(SharkBg)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fill.coerceIn(0f, 1f)).background(color, CircleShape))
        }
        Text(amount, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
    }
}

@Composable
fun SFBottomNav(
    onMicTap: () -> Unit, 
    onMicHold: () -> Unit, 
    isListening: Boolean, 
    micScale: Float, 
    onFeatureClick: (String) -> Unit,
    onHomeClick: () -> Unit = {}
) {
    Box(Modifier.fillMaxWidth().background(SharkSurface).padding(bottom = 20.dp)) {
        HorizontalDivider(Modifier.align(Alignment.TopCenter), color = SharkCardBorder)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), Arrangement.SpaceAround, Alignment.CenterVertically) {
            NavIcon(Icons.Default.Home, "Home", true) { onHomeClick() }
            NavIcon(Icons.Default.GridView, "Features", false) { onFeatureClick("Features") }
            Box(contentAlignment = Alignment.Center) {
                // Pulse effect when listening
                if (isListening) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                        label = "scale"
                    )
                    Box(Modifier.size(70.dp).scale(pulseScale).background(SharkGold.copy(0.2f), CircleShape))
                }
                
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(if (isListening) SharkRed else SharkGold)
                        .clickable { onMicTap() },
                    Alignment.Center
                ) {
                    Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null, tint = SharkBg, modifier = Modifier.size(32.dp))
                }
            }
            NavIcon(Icons.AutoMirrored.Filled.ReceiptLong, "Activity", false) { onFeatureClick("Activity") }
            NavIcon(Icons.Default.Person, "Profile", false) { onFeatureClick("Profile") }
        }
    }
}

@Composable
fun NavIcon(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, null, tint = if(active) SharkGold else SharkTertiary, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = if(active) SharkGold else SharkTertiary, fontWeight = if(active) FontWeight.Bold else FontWeight.Normal)
    }
}
