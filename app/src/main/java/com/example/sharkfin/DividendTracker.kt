package com.example.sharkfin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class DividendPayout(
    val id: String = "",
    val symbol: String = "",
    val amount: Double = 0.0,
    val exDate: Date = Date(),
    val payDate: Date = Date(),
    val isReceived: Boolean = false
)

@Composable
fun DividendTrackerScreen(
    uid: String, 
    db: FirebaseFirestore,
    portfolio: List<PortfolioAsset> = emptyList()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var payouts by remember { mutableStateOf(listOf<DividendPayout>()) }
    
    // Listen for dividend payouts from Firestore
    LaunchedEffect(uid) {
        db.collection("users").document(uid)
            .collection("dividends")
            .addSnapshotListener { snapshot, _ ->
                payouts = snapshot?.documents?.mapNotNull { doc ->
                    val exDate = doc.getTimestamp("exDate")?.toDate() ?: Date()
                    val payDate = doc.getTimestamp("payDate")?.toDate() ?: Date()
                    DividendPayout(
                        id = doc.id,
                        symbol = doc.getString("symbol") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        exDate = exDate,
                        payDate = payDate,
                        isReceived = doc.getBoolean("isReceived") ?: false
                    )
                } ?: emptyList()
            }
    }

    val annualYield = payouts.sumOf { it.amount }
    val portfolioYield = portfolio.sumOf { it.quantity * it.averageCost * 0.03 } // Estimating 3% for assets without logged dividends
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Dividend Terminal", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Track upcoming payouts and yield performance", color = SharkMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f).glassCard(20f, 0.08f).padding(16.dp)) {
                    Column {
                        Text("Annual Yield", color = SharkMuted, fontSize = 11.sp)
                        Text("$${String.format(Locale.US, "%.2f", annualYield)}", color = SharkNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Est. +$${String.format(Locale.US, "%.2f", portfolioYield/12)} /mo", color = SharkGreen, fontSize = 10.sp)
                    }
                }
                Box(modifier = Modifier.weight(1f).glassCard(20f, 0.08f).padding(16.dp)) {
                    Column {
                        Text("Next Payout", color = SharkMuted, fontSize = 11.sp)
                        val next = payouts.filter { it.payDate.after(Date()) }.minByOrNull { it.payDate }
                        Text(next?.symbol ?: "---", color = SharkAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (next != null) {
                            val days = ((next.payDate.time - Date().time) / (1000 * 60 * 60 * 24)).toInt()
                            Text("In $days days", color = SharkMuted, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Projection Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .glassCard(24f, 0.04f)
                    .padding(16.dp)
            ) {
                Column {
                    Text("Yield Projection (12M)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Mock projection bars
                        val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                        months.forEachIndexed { index, month ->
                            val heightFactor = (0.3f + (index % 4) * 0.2f)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(heightFactor)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if(index == 5) SharkNavy else SharkNavy.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(month, color = SharkMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("X-Dividend Calendar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(payouts.sortedBy { it.payDate }) { payout ->
                    DividendPayoutCard(payout)
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = SharkNavy,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, "Add Payout")
        }
    }

    if (showAddDialog) {
        AddDividendDialog(
            onDismiss = { showAddDialog = false },
            onSave = { symbol, amount, exDate, payDate ->
                val newPayout = mapOf(
                    "symbol" to symbol.uppercase(),
                    "amount" to amount,
                    "exDate" to exDate,
                    "payDate" to payDate,
                    "isReceived" to false
                )
                db.collection("users").document(uid).collection("dividends").add(newPayout)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DividendPayoutCard(payout: DividendPayout) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24f, alpha = 0.06f)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SharkNavy.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, null, tint = SharkNavy, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(payout.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Ex-Date: ${df.format(payout.exDate)}", color = SharkMuted, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("+$${String.format(Locale.US, "%.2f", payout.amount)}", color = SharkGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Payable: ${df.format(payout.payDate)}", color = SharkMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AddDividendDialog(onDismiss: () -> Unit, onSave: (String, Double, Date, Date) -> Unit) {
    var symbol by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    // In a real app, we'd use a date picker
    val exDate = Date()
    val payDate = Date()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SharkSurface,
        title = { Text("Log Dividend Payout", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Stock Symbol") },
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Total Payout ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(symbol, amount.toDoubleOrNull() ?: 0.0, exDate, payDate) },
                colors = ButtonDefaults.buttonColors(containerColor = SharkNavy)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SharkMuted) }
        }
    )
}
