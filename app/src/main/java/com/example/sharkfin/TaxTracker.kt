package com.example.sharkfin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaxTrackerScreen(expenses: List<Expense>) {
    var showTutorial by remember { mutableStateOf(true) }
    
    // Auto-calculate income from expense ledger
    val trackedIncome = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    
    var annualIncomeInput by remember { mutableStateOf("") }
    var deductions by remember { mutableStateOf("") }

    // Use tracked income if input is empty
    val incomeVal = if (annualIncomeInput.isEmpty()) trackedIncome else (annualIncomeInput.toDoubleOrNull() ?: 0.0)
    val deductVal = deductions.toDoubleOrNull() ?: 0.0
    val taxableIncome = (incomeVal - deductVal).coerceAtLeast(0.0)

    val estimatedTax = calculateEstimatedTax(taxableIncome)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Tax Estimator", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("FY 2024 Estimates", color = SharkMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.glassCard(alpha = 0.1f).padding(20.dp)) {
                Column {
                    Text("Estimated Tax Liability", color = SharkMuted, fontSize = 12.sp)
                    Text("\$${String.format("%,.2f", estimatedTax)}", color = Color(0xFFef4444), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Effective Rate: ${if(incomeVal > 0) String.format("%.1f%%", (estimatedTax/incomeVal)*100) else "0%"}", color = SharkGreen, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SheetInputField(
                if(annualIncomeInput.isEmpty() && trackedIncome > 0) String.format("%.0f", trackedIncome) else annualIncomeInput,
                { annualIncomeInput = it },
                "Annual Gross Income",
                "Enter amount"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SheetInputField(deductions, { deductions = it }, "Total Deductions", "401k, HSA, etc.")

            Spacer(modifier = Modifier.height(32.dp))
            Text("Tax Brackets (Single)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            TaxBracketRow("10%", "\$0 - \$11k", taxableIncome > 0)
            TaxBracketRow("12%", "\$11k - \$44k", taxableIncome > 11000)
            TaxBracketRow("22%", "\$44k - \$95k", taxableIncome > 44725)
            TaxBracketRow("24%", "\$95k+", taxableIncome > 95375)
            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Tax Transparency",
                description = "Input your gross income and deductions to see an estimate of your tax liability. We've pre-filled your income based on your tracked transactions.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
fun TaxBracketRow(rate: String, range: String, active: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(range, color = if(active) Color.White else SharkMuted, fontSize = 14.sp)
        Text(rate, color = if(active) SharkGreen else SharkMuted, fontWeight = FontWeight.Bold)
    }
}
