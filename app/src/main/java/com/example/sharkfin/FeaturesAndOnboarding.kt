package com.example.sharkfin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Feature Definition ────────────────────────────────────────────────────
data class SharkFeature(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val available: Boolean,
    val description: String,
    val tag: String = ""
)

// ─── Features Screen ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpensesSettingsScreen(uid: String, db: com.google.firebase.firestore.FirebaseFirestore) {
    var rent by remember { mutableStateOf("") }
    var carNote by remember { mutableStateOf("") }
    var gas by remember { mutableStateOf("") }
    var subs by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).collection("recurringBills").get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val key = doc.getString("key")
                    val amount = doc.getDouble("amount")?.toString() ?: ""
                    when (key) {
                        "rent" -> rent = amount
                        "car_note" -> carNote = amount
                        "gas_bill" -> gas = amount
                        "subscriptions" -> subs = amount
                    }
                }
            }
    }

    fun saveBill(key: String, label: String, amountStr: String) {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val data = mapOf(
            "key" to key,
            "label" to label,
            "amount" to amount,
            "category" to when(key) {
                "rent" -> "Housing"
                "car_note" -> "Transit"
                "gas_bill" -> "Utilities"
                else -> "Subscriptions"
            }
        )
        db.collection("users").document(uid).collection("recurringBills").document(key).set(data)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(80.dp))
        Text("Fixed Expenses", color = SharkLabel, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Set your monthly commitments", color = SharkSecondary, fontSize = 14.sp)

        Spacer(Modifier.height(32.dp))

        SharkCard {
            SheetInputField(rent, { rent = it; saveBill("rent", "Rent", it) }, "MONTHLY RENT", "0.00", KeyboardType.Decimal)
            Spacer(Modifier.height(16.dp))
            SheetInputField(carNote, { carNote = it; saveBill("car_note", "Car Note", it) }, "CAR NOTE", "0.00", KeyboardType.Decimal)
            Spacer(Modifier.height(16.dp))
            SheetInputField(gas, { gas = it; saveBill("gas_bill", "Gas Bill", it) }, "ESTIMATED GAS", "0.00", KeyboardType.Decimal)
            Spacer(Modifier.height(16.dp))
            SheetInputField(subs, { subs = it; saveBill("subscriptions", "Subscriptions", it) }, "TOTAL SUBSCRIPTIONS", "0.00", KeyboardType.Decimal)
        }

        Spacer(Modifier.height(40.dp))
        Text("These values update your 'Spendable Today' math on the home screen automatically.", 
            color = SharkSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun HeroStripCard() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 0.8f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SharkSurface, SharkBg)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SharkGold.copy(alpha = 0.4f),
                        Color.Transparent,
                        SharkGold.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SharkGold.copy(alpha = glowAlpha), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "LIVE · SYNCED",
                        color         = SharkGold,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Financial Engine",
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Income → Expenses → Goals → Bills",
                    color    = SharkMuted,
                    fontSize = 12.sp
                )
            }

            MiniBarViz()
        }
    }
}

@Composable
fun MiniBarViz() {
    val inf = rememberInfiniteTransition(label = "bars")

    val h1 by inf.animateFloat(0.4f, 1.0f, infiniteRepeatable(tween(900,   0,   EaseInOutSine), RepeatMode.Reverse), "b1")
    val h2 by inf.animateFloat(0.7f, 1.0f, infiniteRepeatable(tween(1100, 200,  EaseInOutSine), RepeatMode.Reverse), "b2")
    val h3 by inf.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(800,  400,  EaseInOutSine), RepeatMode.Reverse), "b3")

    Row(
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier              = Modifier
            .height(40.dp)
            .width(44.dp)
    ) {
        listOf(h1 to SharkGold, h2 to SharkGold.copy(alpha = 0.6f), h3 to SharkGold.copy(alpha = 0.35f))
            .forEach { (h, color) ->
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight(h)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(color)
                )
            }
    }
}

@Composable
fun FeatureCard(
    feature: SharkFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                drawRoundRect(
                    color        = SharkSurface,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                )
                drawLine(
                    color       = feature.color.copy(alpha = 0.7f),
                    start       = Offset(0f, 20.dp.toPx()),
                    end         = Offset(0f, size.height - 20.dp.toPx()),
                    strokeWidth = 3f,
                    cap         = StrokeCap.Round
                )
            }
            .border(
                width = 0.5.dp,
                color = SharkCardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {
                pressed = false
                onClick()
            }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .background(feature.color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        feature.icon,
                        null,
                        tint     = feature.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (feature.tag.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(feature.color.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            feature.tag,
                            color         = feature.color,
                            fontSize      = 9.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                feature.name,
                color      = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                feature.description,
                color      = SharkMuted,
                fontSize   = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun ComingSoonRow(feature: SharkFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SharkSurface)
            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .background(feature.color.copy(alpha = 0.07f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    feature.icon,
                    null,
                    tint     = feature.color.copy(alpha = 0.4f),
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    feature.name,
                    color      = Color.White.copy(alpha = 0.4f),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    feature.description,
                    color    = SharkMuted.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        Icon(
            Icons.Default.Lock,
            null,
            tint     = SharkMuted.copy(alpha = 0.3f),
            modifier = Modifier.size(14.dp)
        )
    }
}

data class OnboardStep(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val body: String,
    val isBillSetup: Boolean = false
)

val onboardingStepsList = listOf(
    OnboardStep(
        icon  = Icons.Default.AccountBalanceWallet,
        color = SharkGreen,
        title = "Your money, live.",
        body  = "Every dollar you add instantly updates your balance, goals, and bill projections — no refresh needed."
    ),
    OnboardStep(
        icon  = Icons.Default.Receipt,
        color = Color(0xFFf59e0b),
        title = "Let's set your bills.",
        body  = "Sharkfin works best when it knows your commitments. Pick your common monthly bills to start.",
        isBillSetup = true
    ),
    OnboardStep(
        icon  = Icons.Default.Flag,
        color = Color(0xFF06b6d4),
        title = "Goals with a pace check.",
        body  = "Set a target and a deadline. SharkFin tells you if you're on pace or falling behind based on your real spending."
    ),
    OnboardStep(
        icon  = Icons.Default.BarChart,
        color = Color(0xFF8b5cf6),
        title = "See the full picture.",
        body  = "Donut charts, trend lines, and comparison bars built from your actual data — not demo numbers."
    ),
    OnboardStep(
        icon  = Icons.Default.Psychology,
        color = Color(0xFFec4899),
        title = "AI Coach is coming.",
        body  = "Soon you'll just say \"My rent is \$1,250 on the 1st\" and SharkFin will handle the rest. No forms. Just talk."
    )
)

@Composable
fun OnboardingScreen(
    uid: String,
    db: com.google.firebase.firestore.FirebaseFirestore,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val step        = onboardingStepsList[currentStep]
    val isLast      = currentStep == onboardingStepsList.size - 1

    val alpha by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(400),
        label         = "step_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onboardingStepsList.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index == currentStep) 28.dp else 8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index == currentStep) step.color
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .alpha(alpha)
                    .drawBehind {
                        drawCircle(
                            color  = step.color.copy(alpha = 0.12f),
                            radius = size.minDimension / 2f + 20f
                        )
                        drawCircle(
                            color  = step.color.copy(alpha = 0.06f),
                            radius = size.minDimension / 2f + 40f
                        )
                    }
                    .background(step.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    step.icon,
                    null,
                    tint     = step.color,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(48.dp))

            Text(
                step.title,
                color         = Color.White,
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Bold,
                textAlign     = TextAlign.Center,
                letterSpacing = (-0.5).sp,
                modifier      = Modifier.alpha(alpha)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                step.body,
                color      = SharkMuted,
                fontSize   = 15.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
                modifier   = Modifier
                    .alpha(alpha)
                    .padding(horizontal = 8.dp)
            )

            if (step.isBillSetup) {
                Spacer(Modifier.height(24.dp))
                QuickBillSetupGrid(uid, db)
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    TextButton(onClick = {
                        db.collection("users").document(uid)
                            .update("onboarded", true)
                            .addOnSuccessListener { onFinish() }
                            .addOnFailureListener { onFinish() }
                    }) {
                        Text(
                            "Skip",
                            color    = SharkMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Spacer(Modifier.width(60.dp))
                }

                Button(
                    onClick = {
                        if (isLast) {
                            db.collection("users").document(uid)
                                .update("onboarded", true)
                                .addOnSuccessListener { onFinish() }
                                .addOnFailureListener { onFinish() }
                        } else {
                            currentStep++
                        }
                    },
                    shape  = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SharkGold),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        if (isLast) "Let's go 🦈" else "Next",
                        color      = SharkBg,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        modifier   = Modifier.padding(horizontal = 16.dp)
                    )
                    if (!isLast) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            null,
                            tint     = SharkBg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
