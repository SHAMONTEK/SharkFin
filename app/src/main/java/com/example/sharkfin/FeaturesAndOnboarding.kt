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
@Composable
fun FeaturesScreen(onFeatureClick: (String) -> Unit) {

    val allFeatures = listOf(
        SharkFeature(
            name        = "Expense Tracker",
            icon        = Icons.Default.AccountBalanceWallet,
            color       = SharkGreen,
            available   = true,
            description = "Log income & spending in real time",
            tag         = ""
        ),
        SharkFeature(
            name        = "Bill Tracker",
            icon        = Icons.Default.Receipt,
            color       = Color(0xFFf59e0b),
            available   = true,
            description = "Calendar view with 2-week reminders",
            tag         = ""
        ),
        SharkFeature(
            name        = "Goal Tracker",
            icon        = Icons.Default.Flag,
            color       = Color(0xFF06b6d4),
            available   = true,
            description = "Set targets, track pace, hit deadlines",
            tag         = ""
        ),
        SharkFeature(
            name        = "Visual Models",
            icon        = Icons.Default.BarChart,
            color       = Color(0xFF8b5cf6),
            available   = true,
            description = "Donut, trend line & comparison charts",
            tag         = ""
        ),
        SharkFeature(
            name        = "Tax Tracker",
            icon        = Icons.Default.Description,
            color       = Color(0xFFef4444),
            available   = true,
            description = "2024 brackets, effective rate estimator",
            tag         = "BETA"
        ),
        SharkFeature(
            name        = "Inflation Calc",
            icon        = Icons.Default.Calculate,
            color       = Color(0xFF06b6d4),
            available   = true,
            description = "See how your money loses value over time",
            tag         = "BETA"
        ),
        SharkFeature(
            name        = "Stock/Forex",
            icon        = Icons.Default.CurrencyExchange,
            color       = SharkGreen,
            available   = true,
            description = "Live market watch — demo mode",
            tag         = "DEMO"
        ),
        SharkFeature(
            name        = "Account Settings",
            icon        = Icons.Default.Settings,
            color       = Color(0xFF6b7280),
            available   = true,
            description = "Security, notifications, preferences",
            tag         = ""
        ),
        SharkFeature(
            name        = "AI Coach",
            icon        = Icons.Default.Psychology,
            color       = Color(0xFF8b5cf6),
            available   = false,
            description = "Talk to SharkFin — it updates everything",
            tag         = "SOON"
        ),
        SharkFeature(
            name        = "Investment Tracker",
            icon        = Icons.Default.TrendingUp,
            color       = SharkGreen,
            available   = false,
            description = "Portfolio tracking & growth modeling",
            tag         = "SOON"
        ),
        SharkFeature(
            name        = "AI Prediction",
            icon        = Icons.Default.AutoGraph,
            color       = Color(0xFFec4899),
            available   = false,
            description = "Forecast your balance 6 months out",
            tag         = "SOON"
        ),
        SharkFeature(
            name        = "Shared Accounts",
            icon        = Icons.Default.People,
            color       = Color(0xFFf59e0b),
            available   = false,
            description = "Joint & family account collaboration",
            tag         = "SOON"
        )
    )

    val available   = allFeatures.filter { it.available }
    val comingSoon  = allFeatures.filter { !it.available }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(56.dp))

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(400)) + slideInVertically(tween(400)) { -20 }
        ) {
            Column {
                Text(
                    "Features",
                    color      = Color.White,
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "${available.size} active · ${comingSoon.size} coming soon",
                    color    = SharkMuted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(tween(500, 100)) { 30 }
        ) {
            HeroStripCard()
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "YOUR TOOLKIT",
            color         = SharkMuted,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))

        available.chunked(2).forEachIndexed { rowIndex, row ->
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(400, delayMillis = 150 + rowIndex * 60)) +
                        slideInVertically(tween(400, 150 + rowIndex * 60)) { 40 }
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { feature ->
                        FeatureCard(
                            feature  = feature,
                            modifier = Modifier.weight(1f),
                            onClick  = { onFeatureClick(feature.name) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "IN DEVELOPMENT",
            color         = SharkMuted,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))

        comingSoon.forEach { feature ->
            ComingSoonRow(feature = feature)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(100.dp))
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
                    colors = listOf(Color(0xFF0d2d1f), Color(0xFF091a13))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SharkGreen.copy(alpha = 0.4f),
                        Color.Transparent,
                        SharkGreen.copy(alpha = 0.1f)
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
                            .background(SharkGreen.copy(alpha = glowAlpha), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "LIVE · SYNCED",
                        color         = SharkGreen,
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
        listOf(h1 to SharkGreen, h2 to SharkGreen.copy(alpha = 0.6f), h3 to SharkGreen.copy(alpha = 0.35f))
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
                    color        = feature.color.copy(alpha = 0.06f),
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = feature.color.copy(alpha = 0.12f),
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
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
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
    val body: String
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
        title = "Bills that warn you.",
        body  = "Add a bill once. SharkFin puts it on the calendar, reminds you 2 weeks out, and deducts it from your projected balance."
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
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0a1a14), SharkBlack),
                    radius = 1400f
                )
            )
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
                    colors = ButtonDefaults.buttonColors(containerColor = step.color),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        if (isLast) "Let's go 🦈" else "Next",
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        modifier   = Modifier.padding(horizontal = 16.dp)
                    )
                    if (!isLast) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            null,
                            tint     = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
