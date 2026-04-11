package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.example.sharkfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ActivityScreen(
    expenses: List<Expense>,
    bills: List<Bill>,
    uid: String,
    db: FirebaseFirestore
) {
    val totalIn by remember(expenses) {
        derivedStateOf { expenses.filter { SharkIncomeCategories.contains(it.category) }.sumOf { it.amount } }
    }
    val totalOut by remember(expenses) {
        derivedStateOf { expenses.filter { !SharkIncomeCategories.contains(it.category) }.sumOf { it.amount } }
    }
    val net by remember(totalIn, totalOut) {
        derivedStateOf { totalIn - totalOut }
    }

    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activity", style = SharkTypography.headlineLarge, color = SharkLabel)
            IconButton(onClick = { /* Filter functionality can be added here */ }) {
                Icon(Icons.Default.FilterList, null, tint = SharkLabel)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryPill("Total In", totalIn, SharkPositive, Modifier.weight(1f))
            SummaryPill("Total Out", totalOut, SharkNegative, Modifier.weight(1f))
            SummaryPill("Net", net, if (net >= 0) SharkPositive else SharkGold, Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))
        SharkSectionHeader("RECENT TRANSACTIONS")
        Spacer(Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            EmptyActivityState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(expenses, key = { _, expense -> expense.id }) { index, expense ->
                    val state = remember { MutableTransitionState(false) }.apply { targetState = true }

                    AnimatedVisibility(
                        visibleState = state,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = (index % 10) * 50)) +
                                slideInVertically(animationSpec = tween(500, delayMillis = (index % 10) * 50)) { it / 2 }
                    ) {
                        TransactionRow(
                            expense = expense,
                            onClick = { editingExpense = expense }
                        )
                    }
                }
            }
        }
    }

    editingExpense?.let { expense ->
        EditExpenseSheet(
            expense = expense,
            uid = uid,
            db = db,
            onDismiss = { editingExpense = null }
        )
    }
}

@Composable
fun SummaryPill(label: String, amount: Double, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(SharkSurface, RoundedCornerShape(22.dp))
            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(22.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = SharkTypography.labelMedium, color = SharkSecondary, fontSize = 10.sp)
        Text(
            String.format(Locale.US, "$%.0f", amount),
            style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun TransactionRow(expense: Expense, onClick: () -> Unit) {
    val categoryColor = remember(expense.category) { getCategoryColor(expense.category) }
    val isIncome = remember(expense.category) { SharkIncomeCategories.contains(expense.category) }
    val dateStr = remember(expense.createdAtDate) { SimpleDateFormat("MMM dd, yyyy", Locale.US).format(expense.createdAtDate) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = expense.category.take(1).uppercase(),
                style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = categoryColor
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = SharkLabel
            )
            Text(
                text = dateStr,
                style = SharkTypography.labelMedium,
                color = SharkSecondary,
                fontSize = 10.sp
            )
        }

        Text(
            text = String.format(Locale.US, (if (isIncome) "+" else "-") + "$%.2f", expense.amount),
            style = SharkTypography.bodyLarge.copy(fontFamily = DMMonoFontFamily),
            color = if (isIncome) SharkPositive else SharkGold
        )
    }
}

@Composable
fun EmptyActivityState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ReceiptLong,
                null,
                modifier = Modifier.size(48.dp),
                tint = SharkSecondary.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text("No transactions yet", style = SharkTypography.headlineMedium, color = SharkLabel)
            Text(
                "Add transactions using the AI Assistant\nor the Snapshot tools.",
                style = SharkTypography.bodyMedium,
                color = SharkSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    uid: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(expenseCategories.first().name) }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SharkSurface,
        contentColor = SharkLabel,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SharkSurfaceHigh) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Transaction", style = SharkTypography.headlineMedium, color = SharkLabel)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", color = SharkSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SharkGold,
                    unfocusedBorderColor = SharkCardBorder,
                    cursorColor = SharkGold,
                    focusedTextColor = SharkLabel,
                    unfocusedTextColor = SharkLabel
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount", color = SharkSecondary) },
                prefix = { Text("$", color = SharkGold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SharkGold,
                    unfocusedBorderColor = SharkCardBorder,
                    cursorColor = SharkGold,
                    focusedTextColor = SharkLabel,
                    unfocusedTextColor = SharkLabel
                ),
                shape = RoundedCornerShape(12.dp)
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Category", color = SharkSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SharkGold,
                        unfocusedBorderColor = SharkCardBorder,
                        focusedTextColor = SharkLabel,
                        unfocusedTextColor = SharkLabel
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SharkSurfaceHigh)
                ) {
                    expenseCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, color = SharkLabel) },
                            onClick = {
                                selectedCategory = cat.name
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)", color = SharkSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SharkGold,
                    unfocusedBorderColor = SharkCardBorder,
                    cursorColor = SharkGold,
                    focusedTextColor = SharkLabel,
                    unfocusedTextColor = SharkLabel
                ),
                shape = RoundedCornerShape(12.dp)
            )

            errorMsg?.let {
                Text(it, color = SharkRed, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    scope.launch {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isBlank()) {
                            errorMsg = "Title is required"
                            return@launch
                        }
                        if (amount <= 0) {
                            errorMsg = "Amount must be greater than 0"
                            return@launch
                        }

                        val newExpense = Expense(
                            id = "",
                            title = title.trim(),
                            amount = amount,
                            category = selectedCategory,
                            note = note.trim(),
                            createdAt = Date()
                        )

                        db.collection("users").document(uid)
                            .collection("expenses")
                            .add(newExpense)
                            .addOnSuccessListener {
                                onDismiss()
                            }
                            .addOnFailureListener {
                                errorMsg = "Failed to save"
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGold, contentColor = SharkBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseSheet(
    expense: Expense,
    uid: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(expense.title) }
    var amountStr by remember { mutableStateOf(expense.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var note by remember { mutableStateOf(expense.note) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SharkSurface,
        contentColor = SharkLabel,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SharkSurfaceHigh) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Transaction", style = SharkTypography.headlineMedium, color = SharkLabel)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", color = SharkSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SharkGold,
                    unfocusedBorderColor = SharkCardBorder,
                    cursorColor = SharkGold,
                    focusedTextColor = SharkLabel,
                    unfocusedTextColor = SharkLabel
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount", color = SharkSecondary) },
                prefix = { Text("$", color = SharkGold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SharkGold,
                    unfocusedBorderColor = SharkCardBorder,
                    cursorColor = SharkGold,
                    focusedTextColor = SharkLabel,
                    unfocusedTextColor = SharkLabel
                ),
                shape = RoundedCornerShape(12.dp)
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Category", color = SharkSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SharkGold,
                        unfocusedBorderColor = SharkCardBorder,
                        focusedTextColor = SharkLabel,
                        unfocusedTextColor = SharkLabel
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SharkSurfaceHigh)
                ) {
                    expenseCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, color = SharkLabel) },
                            onClick = {
                                selectedCategory = cat.name
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)", color = SharkSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SharkGold,
                    unfocusedBorderColor = SharkCardBorder,
                    cursorColor = SharkGold,
                    focusedTextColor = SharkLabel,
                    unfocusedTextColor = SharkLabel
                ),
                shape = RoundedCornerShape(12.dp)
            )

            errorMsg?.let {
                Text(it, color = SharkRed, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            db.collection("users").document(uid)
                                .collection("expenses").document(expense.id)
                                .delete()
                                .addOnSuccessListener { onDismiss() }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SharkRed,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, SharkRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (title.isBlank()) {
                                errorMsg = "Title is required"
                                return@launch
                            }
                            if (amount <= 0) {
                                errorMsg = "Amount must be greater than 0"
                                return@launch
                            }

                            val updated = expense.copy(
                                title = title.trim(),
                                amount = amount,
                                category = selectedCategory,
                                note = note.trim()
                            )

                            db.collection("users").document(uid)
                                .collection("expenses").document(expense.id)
                                .set(updated)
                                .addOnSuccessListener { onDismiss() }
                                .addOnFailureListener {
                                    errorMsg = "Failed to update"
                                }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SharkGold, contentColor = SharkBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
