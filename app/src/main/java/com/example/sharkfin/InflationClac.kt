package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.pow

@Composable
fun InflationCalcScreen() {
    var showTutorial by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("1000") }
    var yearsText by remember { mutableStateOf("10") }
    var rateText by remember { mutableStateOf("3.5") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toIntOrNull() ?: 1
    val rate = (rateText.toDoubleOrNull() ?: 0.0) / 100.0
    
    val purchasingPower = if (rate > -1.0) amount / (1 + rate).pow(years.toDouble()) else amount

    Box(modifier = Modifier.fillMaxSize().background(SharkBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Text("Inflation Erosion", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("The invisible tax on your savings", color = SharkMuted, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // ── Visual Erosion Card ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24f, alpha = 0.1f)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PURCHASING POWER IN $years YEARS", color = SharkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "$${String.format(Locale.US, "%,.2f", purchasingPower)}",
                        color = SharkRed,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Loss of value: $${String.format(Locale.US, "%,.2f", amount - purchasingPower)}",
                        color = SharkMuted,
                        fontSize = 13.sp
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Erosion Trend Line
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val points = List(years + 1) { i -> (amount / (1 + rate).pow(i)).toFloat() }
                            val maxVal = amount.toFloat().coerceAtLeast(1f)
                            val stepX = size.width / years.toFloat().coerceAtLeast(1f)
                            
                            val path = Path()
                            points.forEachIndexed { i, p ->
                                val x = i * stepX
                                val y = size.height - (p / maxVal * size.height)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            
                            drawPath(
                                path = path,
                                color = SharkRed,
                                style = Stroke(width = 6f, cap = StrokeCap.Round)
                            )
                            
                            // Highlight final point
                            val finalY = size.height - (points.last() / maxVal * size.height)
                            drawCircle(SharkRed, radius = 8f, center = Offset(size.width, finalY))
                            drawCircle(Color.White, radius = 4f, center = Offset(size.width, finalY))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Input Controls ────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SheetInputField(amountText, { amountText = it }, "Current Savings ($)", "1000")
                SheetInputField(yearsText, { yearsText = it }, "Years into Future", "10")
                SheetInputField(rateText, { rateText = it }, "Expected Inflation (%)", "3.5")
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── AI Insight ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SharkNavy.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = SharkNavy, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "To keep the same standard of living, you'll need \$${String.format(Locale.US, "%,.0f", amount * (1 + rate).pow(years))} in $years years. Investing in assets that beat inflation is key.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Wealth Erosion",
                description = "See how much your cash is actually worth over time. This tool visualizes the impact of inflation on your long-term purchasing power.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}
