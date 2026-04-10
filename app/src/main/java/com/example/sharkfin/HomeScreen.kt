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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val wizardIncome = (discoveryData?.get("monthlyIncome") as? Double) ?: 0.0
    val wizardObligations = (discoveryData?.get("monthlyObligations") as? Double) ?: 0.0
    val totalIncome = incomeSources.sumOf { it.amount }.coerceAtLeast(wizardIncome)
    val totalSpent = expenses.filter { it.category !in incomeCategories }.sumOf { it.amount }
    val balance = expenses.filter { it.category in incomeCategories }.sumOf { it.amount } - totalSpent

    fun isToday(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { time = date }
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    val spentToday = expenses.filter { isToday(it.createdAtDate) && it.category !in incomeCategories }.sumOf { it.amount }
    val dailyBudget = if (totalIncome > 0) totalIncome / 30.0 else 0.0
    val totalObligations = bills.sumOf { it.amount } + debts.sumOf { it.minimumPayment } + recurringBills.sumOf { it.amount }
    val maxObligations = totalObligations.coerceAtLeast(wizardObligations)

    val spendableTodayMax = ((dailyBudget - (maxObligations / 30.0)) * 0.20).coerceAtLeast(0.0)
    val spendableLeft = (spendableTodayMax - spentToday).coerceAtLeast(0.0)
    val passiveIncome = expenses.filter { it.category == "Passive Income" }.sumOf { it.amount }
    val avgDailyBurn = if (expenses.isNotEmpty()) totalSpent / 30.0 else (maxObligations / 30.0)

    val score = calcMoneyScore(
        income = totalIncome, spent = totalSpent, bills = bills,
        goals = goals, debts = debts, avgDailyBurn = avgDailyBurn,
        balance = balance, passiveIncome = passiveIncome
    )

    val firstName = displayName.split(" ").firstOrNull()?.ifBlank { "Shark" } ?: "Shark"

    // ── AI AGENT STATE ───────────────────────────────────────────────────────
    var chatOpen by remember { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf(listOf<SharkChatMessage>()) }
    var isListening by remember { mutableStateOf(false) }
    var liveTranscript by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var currentSession by remember { mutableStateOf(SharkAgentSession.IDLE) }
    var awaitingConfirm by remember { mutableStateOf<ParsedTransaction?>(null) }
    var textInputVal by remember { mutableStateOf("") }

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

    fun processVoiceInput(input: String) {
        if (input.isBlank()) return
        scope.launch {
            isThinking = true
            chatMessages = chatMessages + SharkChatMessage(input, isShark = false)
            delay(1000)

            if (awaitingConfirm != null) {
                if (input.lowercase().contains("yes") || input.lowercase().contains("yeah") || input.lowercase().contains("confirm")) {
                    logParsedTransaction(awaitingConfirm!!)
                    chatMessages = chatMessages + SharkChatMessage("Confirmed. Logged it.", isShark = true)
                    awaitingConfirm = null
                } else if (input.lowercase().contains("no") || input.lowercase().contains("cancel")) {
                    chatMessages = chatMessages + SharkChatMessage("Cancelled. What else?", isShark = true)
                    awaitingConfirm = null
                }
                isThinking = false
                return@launch
            }

            val parsed = AICoachNLP.parse(input, financialState.knownRecurring, currentSession)
            val response = AICoachResponse.generate(parsed, financialState)

            chatMessages = chatMessages + SharkChatMessage(response.message, isShark = true)
            isThinking = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            if (response.navigateTo != null) onFeatureClick(response.navigateTo)
            if (parsed.needsConfirm || response.askFollowUp != null) awaitingConfirm = parsed
            if (response.logTransaction && parsed.amount != null && !parsed.needsConfirm) logParsedTransaction(parsed)
        }
    }

    // ── SPEECH RECOGNITION SETUP ─────────────────────────────────────────────
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
        override fun onError(error: Int) { isListening = false; liveTranscript = "" }
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
        if (isGranted) { isListening = true; speechRecognizer.startListening(recognizerIntent) }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            isListening = true; speechRecognizer.startListening(recognizerIntent)
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
                            ToolkitTile(tile, Modifier.weight(1f)) { if(tile.available) onFeatureClick(tile.id) }
                        }
                        if(pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── CHAT OVERLAY ─────────────────────────────────────────────────────
        AnimatedVisibility(chatOpen, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            Box(Modifier.fillMaxSize().background(SharkBg)) {
                Column(Modifier.fillMaxSize().padding(top = 50.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(60.dp), Alignment.Center) {
                                LottieAnimation(composition, { lottieProgress }, modifier = Modifier.fillMaxSize())
                            }
                            Column {
                                Text("Sharkie AI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SharkLabel)
                                Text(if(isListening) "Listening..." else if (isThinking) "Thinking..." else "Ready to help", color = SharkGold, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { chatOpen = false }) { Icon(Icons.Default.Close, null, tint = SharkSecondary) }
                    }
                    HorizontalDivider(color = SharkCardBorder)
                    
                    LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(chatMessages) { msg ->
                            ChatBubble(msg)
                        }
                        if (isThinking) { item { Text("Shark is typing...", color = SharkSecondary, fontSize = 12.sp) } }
                    }

                    // Input Bar
                    Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = textInputVal,
                            onValueChange = { textInputVal = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask anything...", color = SharkTertiary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SharkSurface,
                                unfocusedContainerColor = SharkSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = SharkLabel
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                processVoiceInput(textInputVal)
                                textInputVal = ""
                            })
                        )
                        FloatingActionButton(onClick = { startListening() }, containerColor = SharkGold, shape = CircleShape) {
                            Icon(if(isListening) Icons.Default.Stop else Icons.Default.Mic, null, tint = SharkBg)
                        }
                    }
                }
            }
        }

        // BOTTOM NAV
        Box(Modifier.align(Alignment.BottomCenter)) {
            SFBottomNav(
                onMicTap = { chatOpen = true },
                onMicHold = { chatOpen = true; startListening() },
                isListening = isListening,
                micScale = 1f,
                onFeatureClick = onFeatureClick
            )
        }
    }
}

@Composable
fun ChatBubble(msg: SharkChatMessage) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if(msg.isShark) Alignment.Start else Alignment.End) {
        Box(
            Modifier.clip(RoundedCornerShape(16.dp))
                .background(if(msg.isShark) SharkSurface else SharkGold)
                .padding(12.dp)
        ) {
            Text(msg.text, color = if(msg.isShark) SharkLabel else SharkBg, fontSize = 14.sp)
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
fun SFBottomNav(onMicTap: () -> Unit, onMicHold: () -> Unit, isListening: Boolean, micScale: Float, onFeatureClick: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().background(SharkSurface).padding(bottom = 20.dp)) {
        HorizontalDivider(Modifier.align(Alignment.TopCenter), color = SharkCardBorder)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), Arrangement.SpaceAround, Alignment.CenterVertically) {
            NavIcon(Icons.Default.Home, "Home", true) {}
            NavIcon(Icons.Default.GridView, "Features", false) { onFeatureClick("Features") }
            Box(contentAlignment = Alignment.Center) {
                if (isListening) Box(Modifier.size(74.dp).border(2.dp, SharkGold.copy(0.3f), CircleShape).scale(1.2f))
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(SharkGold)
                        .clickable { onMicTap() }
                        .pointerInput(Unit) { detectTapGestures(onLongPress = { onMicHold() }) },
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, null, tint = SharkBg, modifier = Modifier.size(28.dp))
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
