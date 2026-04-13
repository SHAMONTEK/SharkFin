package com.example.sharkfin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

// ─── Simple message model ─────────────────────────────────────────────────────
data class SharkMessage(
    val id     : Long    = System.currentTimeMillis(),
    val text   : String,
    val isUser : Boolean
)

// ─── Color palette ────────────────────────────────────────────────────────────
private val BG        = Color(0xFFF2F2F7)
private val CARD      = Color(0xFFFFFFFF)
private val SEP       = Color(0xFFE5E5EA)
private val LABEL     = Color(0xFF1C1C1E)
private val SECONDARY = Color(0xFF8E8E93)
private val TERTIARY  = Color(0xFFC7C7CC)
private val GROUPED   = Color(0xFFF2F2F7)
private val GREEN     = Color(0xFF27500A)
private val GREEN_MID = Color(0xFF3B6D11)
private val GREEN_LT  = Color(0xFFEAF3DE)
private val GREEN_BDR = Color(0xFFC0DD97)

// ─── SharkieChatScreen ────────────────────────────────────────────────────────
@Composable
fun SharkieChatScreen(
    uid            : String,
    db             : FirebaseFirestore,
    displayName    : String,
    financialState : SharkFinancialState,
    recurringBills : List<RecurringBill> = emptyList(),
    onClose        : () -> Unit
) {
    val scope    = rememberCoroutineScope()
    val context  = LocalContext.current
    val haptic   = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val firstName = displayName.split(" ").firstOrNull()?.ifBlank { "there" } ?: "there"
    val bills     = if (recurringBills.isEmpty()) defaultRecurringBills else recurringBills

    // ── State ─────────────────────────────────────────────────────────────────
    var messages    by remember {
        mutableStateOf(
            listOf(
                SharkMessage(
                    text   = "Hey $firstName! Talk or type — I'll understand either. What's going on?",
                    isUser = false
                )
            )
        )
    }
    var textInput       by remember { mutableStateOf("") }
    var isThinking      by remember { mutableStateOf(false) }
    var isListening     by remember { mutableStateOf(false) }
    var liveTranscript  by remember { mutableStateOf("") }
    var awaitingConfirm by remember { mutableStateOf<ParsedTransaction?>(null) }
    var session         by remember { mutableStateOf(SharkAgentSession.IDLE) }

    // ── Auto-scroll ───────────────────────────────────────────────────────────
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // ── Log parsed transaction to Firestore ───────────────────────────────────
    fun logTransaction(parsed: ParsedTransaction) {
        if (parsed.amount != null) {
            db.collection("users").document(uid).collection("expenses").add(
                hashMapOf(
                    "title"     to (parsed.merchantHint ?: parsed.category),
                    "amount"    to parsed.amount,
                    "category"  to parsed.category,
                    "note"      to parsed.rawInput,
                    "createdAt" to Date()
                )
            )
        }
    }

    // ── Core send function — uses local NLP engine ────────────────────────────
    fun send(input: String) {
        if (input.isBlank()) return
        keyboard?.hide()

        val userMsg = SharkMessage(text = input.trim(), isUser = true)
        messages    = messages + userMsg
        isThinking  = true

        scope.launch {
            delay(600) // natural thinking pause

            // Handle confirmation flow
            if (awaitingConfirm != null) {
                val lower = input.lowercase()
                when {
                    lower.contains("yes") || lower.contains("yeah") ||
                            lower.contains("confirm") || lower.contains("do it") -> {
                        logTransaction(awaitingConfirm!!)
                        messages = messages + SharkMessage(
                            text   = "Done. Logged it. 🦈",
                            isUser = false
                        )
                        awaitingConfirm = null
                    }
                    lower.contains("no") || lower.contains("cancel") ||
                            lower.contains("stop") || lower.contains("nah") -> {
                        messages = messages + SharkMessage(
                            text   = "Cancelled. What else?",
                            isUser = false
                        )
                        awaitingConfirm = null
                    }
                    else -> {
                        // Treat as new input
                        val parsed   = AICoachNLP.parse(input.trim(), bills, session)
                        val response = AICoachResponse.generate(parsed, financialState)
                        messages = messages + SharkMessage(text = response.message, isUser = false)
                        if (response.logTransaction && parsed.amount != null) logTransaction(parsed)
                        if (parsed.needsConfirm || response.askFollowUp != null) awaitingConfirm = parsed
                    }
                }
                isThinking = false
                return@launch
            }

            // Normal NLP parse
            val parsed   = AICoachNLP.parse(input.trim(), bills, session)
            val response = AICoachResponse.generate(parsed, financialState)

            messages = messages + SharkMessage(text = response.message, isUser = false)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            // Confirm before logging?
            if (parsed.needsConfirm || response.askFollowUp != null) {
                awaitingConfirm = parsed
            } else if (response.logTransaction && parsed.amount != null) {
                logTransaction(parsed)
            }

            isThinking = false
        }
    }

    // ── Speech recognition ────────────────────────────────────────────────────
    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val intent     = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    DisposableEffect(Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(e: Int) { isListening = false; liveTranscript = "" }
            override fun onResults(r: android.os.Bundle?) {
                val result = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!result.isNullOrBlank()) send(result)
                liveTranscript = ""
                isListening    = false
            }
            override fun onPartialResults(p: android.os.Bundle?) {
                liveTranscript = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
            }
            override fun onEvent(t: Int, p: android.os.Bundle?) {}
        })
        onDispose { recognizer.destroy() }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) { isListening = true; recognizer.startListening(intent) }
        else Toast.makeText(context, "Microphone permission needed", Toast.LENGTH_SHORT).show()
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Voice not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            isListening = true
            recognizer.startListening(intent)
        } else {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Lottie ────────────────────────────────────────────────────────────────
    val composition    by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cute_shark_animation))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying   = true,
        iterations  = LottieConstants.IterateForever,
        clipSpec    = if (isListening || isThinking) LottieClipSpec.Frame(72, 96)
        else LottieClipSpec.Frame(96, 137)
    )

    // ── Animations ────────────────────────────────────────────────────────────
    val inf      = rememberInfiniteTransition(label = "wave")
    val wave     by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "w"
    )
    val micScale by animateFloatAsState(
        if (isListening) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ms"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // ROOT
    // ─────────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CARD)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARD)
                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lottie shark avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GREEN_LT)
                        .border(1.dp, GREEN_BDR, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress    = { lottieProgress },
                        modifier    = Modifier.size(38.dp)
                    )
                }

                Column {
                    Text("Sharkie", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LABEL)
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { i ->
                            val h = if (isListening || isThinking) (wave * (5f + i * 2f)).coerceIn(2f, 14f) else 2f
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(h.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(GREEN_MID.copy(alpha = if (isListening || isThinking) 1f else 0.3f))
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                isListening -> "Listening..."
                                isThinking  -> "Thinking..."
                                else        -> "Ready"
                            },
                            fontSize = 12.sp,
                            color    = GREEN_MID
                        )
                    }
                }
            }

            // Close
            IconButton(
                onClick  = onClose,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SECONDARY)
            }
        }

        HorizontalDivider(color = SEP, thickness = 0.5.dp)

        // ── Messages list ─────────────────────────────────────────────────────
        LazyColumn(
            state           = listState,
            modifier        = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding  = PaddingValues(0.dp, 14.dp, 0.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }

            // Typing indicator
            if (isThinking) {
                item { TypingDots() }
            }

            // Live transcript pill while listening
            if (liveTranscript.isNotEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(GROUPED)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(liveTranscript, fontSize = 13.sp, color = LABEL)
                        }
                    }
                }
            }
        }

        // ── Quick suggestion chips — only before first user message ───────────
        if (messages.none { it.isUser }) {
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "How long can I survive?",
                    "What's my score?",
                    "Am I on track today?"
                ).forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(GREEN_LT)
                            .border(0.5.dp, GREEN_BDR, RoundedCornerShape(16.dp))
                            .clickable { send(chip) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(chip, fontSize = 11.sp, color = GREEN, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        HorizontalDivider(color = SEP, thickness = 0.5.dp)

        // ── Input row — ALWAYS VISIBLE ────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .background(CARD)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text field
            TextField(
                value         = textInput,
                onValueChange = { textInput = it },
                modifier      = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder   = {
                    Text(
                        "Type or hold mic to talk...",
                        color    = TERTIARY,
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = GROUPED,
                    unfocusedContainerColor = GROUPED,
                    focusedTextColor        = LABEL,
                    unfocusedTextColor      = LABEL,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = GREEN
                ),
                maxLines        = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            send(textInput.trim())
                            textInput = ""
                        }
                    }
                )
            )

            // Send button — visible when text present
            if (textInput.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GREEN)
                        .clickable {
                            send(textInput.trim())
                            textInput = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint               = GREEN_LT,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }

            // Mic button — hold to speak
            Box(
                modifier = Modifier
                    .scale(micScale)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isListening) GREEN_MID else GREEN)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                startListening()
                                tryAwaitRelease()
                                if (isListening) {
                                    recognizer.stopListening()
                                    isListening = false
                                }
                            },
                            onTap = { startListening() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint               = GREEN_LT,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── iMessage bubble ──────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(msg: SharkMessage) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = 18.dp,
                        topEnd      = 18.dp,
                        bottomStart = if (msg.isUser) 18.dp else 4.dp,
                        bottomEnd   = if (msg.isUser) 4.dp else 18.dp
                    )
                )
                .background(if (msg.isUser) GREEN else Color(0xFFF1EFE8))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text       = msg.text,
                fontSize   = 14.sp,
                lineHeight = 20.sp,
                color      = if (msg.isUser) GREEN_LT else LABEL
            )
        }
    }
}

// ─── Typing dots ──────────────────────────────────────────────────────────────
@Composable
private fun TypingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .background(Color(0xFFF1EFE8))
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val a by inf.animateFloat(
                        initialValue  = 0.3f,
                        targetValue   = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(600, delayMillis = i * 180),
                            RepeatMode.Reverse
                        ),
                        label = "d$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(a)
                            .background(TERTIARY, CircleShape)
                    )
                }
            }
        }
    }
}