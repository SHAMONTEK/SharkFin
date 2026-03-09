package com.example.sharkfin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun IncomeSourcesSection(
    uid: String,
    db: FirebaseFirestore,
    incomeSources: List<IncomeSource>,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Income Sources", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, "Add Source", tint = SharkNavy)
            }
        }
        
        if (incomeSources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .glassCard(alpha = 0.05f),
                contentAlignment = Alignment.Center
            ) {
                Text("No income sources set", color = SharkMuted, fontSize = 14.sp)
            }
        } else {
            incomeSources.forEach { source ->
                IncomeSourceCard(uid, db, source)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun IncomeSourceCard(uid: String, db: FirebaseFirestore, source: IncomeSource) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20f, alpha = 0.08f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SharkNavy.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Payments, null, tint = SharkNavy, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(source.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${source.frequency} · \$${String.format("%.2f", source.amount)}", color = SharkMuted, fontSize = 12.sp)
        }

        IconButton(
            onClick = {
                // Log transaction
                val expenseRef = db.collection("users").document(uid).collection("expenses").document()
                val expense = Expense(
                    id = expenseRef.id,
                    title = "Income: ${source.name}",
                    amount = source.amount,
                    category = "Income",
                    createdAt = Date()
                )
                val batch = db.batch()
                batch.set(expenseRef, expense)
                batch.update(db.collection("users").document(uid).collection("incomeSources").document(source.id), "lastReceived", Date())
                batch.commit()
            },
            modifier = Modifier
                .size(36.dp)
                .background(SharkNavy, CircleShape)
        ) {
            Icon(Icons.Default.Check, "Received", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeSourceSheet(uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Monthly") }
    val frequencies = listOf("Weekly", "Biweekly", "Monthly", "One-time")
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SharkBase) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().imePadding()) {
            Text("New Income Source", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            SheetInputField(name, { name = it }, "Source Name", "e.g. Job, Freelance")
            Spacer(Modifier.height(16.dp))
            
            SheetInputField(amount, { amount = it }, "Amount", "0.00", KeyboardType.Decimal)
            Spacer(Modifier.height(16.dp))
            
            Text("Frequency", color = SharkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(frequency, color = Color.White)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    frequencies.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                frequency = freq
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0) {
                        val sourceRef = db.collection("users").document(uid).collection("incomeSources").document()
                        val source = IncomeSource(id = sourceRef.id, name = name, amount = amt, frequency = frequency)
                        sourceRef.set(source).addOnSuccessListener { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkNavy)
            ) {
                Text("Add Income Source", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}