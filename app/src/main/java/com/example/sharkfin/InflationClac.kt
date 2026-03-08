package com.example.sharkfin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

@Composable
fun InflationCalcScreen() {
    var showTutorial by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("1000") }
    var years by remember { mutableStateOf("10") }
    var rate by remember { mutableStateOf("3.5") }

    val p = amount.toDoubleOrNull() ?: 0.0
    val t = years.toDoubleOrNull() ?: 0.0
    val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0
    val futureValue = p * (1 + r).pow(t)
    val purchasingPower = p / (1 + r).pow(t)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Inflation Calculator", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Plan for future value", color = SharkMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.glassCard(alpha = 0.1f).padding(20.dp)) {
                Column {
                    Text("In $years years, \$$amount will be worth:", color = SharkMuted, fontSize = 12.sp)
                    Text("\$${String.format("%,.2f", purchasingPower)}", color = Color(0xFFef4444), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("In today's purchasing power", color = SharkMuted, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SheetInputField(amount, { amount = it }, "Current Amount (\$)", "1000")
            Spacer(modifier = Modifier.height(12.dp))
            SheetInputField(years, { years = it }, "Time Period (Years)", "10")
            Spacer(modifier = Modifier.height(12.dp))
            SheetInputField(rate, { rate = it }, "Avg. Inflation Rate (%)", "3.5")

            Spacer(modifier = Modifier.height(32.dp))
            Text("To maintain the same lifestyle, you will need \$${String.format("%,.2f", futureValue)} in $years years.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Time & Value",
                description = "Inflation erodes cash value over time. Use this tool to calculate how much today's money will actually buy you in the future based on variable inflation rates.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}