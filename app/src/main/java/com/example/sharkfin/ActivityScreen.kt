package com.example.sharkfin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    db: FirebaseFirestore,
    onAddExpense: () -> Unit = {},
    onEditExpense: (Expense) -> Unit = {}
) {
    val totalIn = expenses.filter { it.category == "Income" }.sumOf { it.amount }
    val totalOut = expenses.filter { it.category != "Income" }.sumOf { it.amount }
    val net = totalIn - totalOut

    var showAddSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBase)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activity", style = SharkTypography.headlineLarge, color = SharkTextPrimary)
            IconButton(onClick = { /* Filter - todo */ }) {
                Icon(Icons.Default.FilterList, null, tint = SharkTextPrimary)
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
            SummaryPill("Net", net, if (net >= 0) SharkPositive else SharkNegative, Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))
        SharkSectionHeader("THIS MONTH")
        Spacer(Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            EmptyActivityState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(expenses) { index, expense ->
                    val delay = index * 40
                    val state = remember { MutableTransitionState(false) }.apply { targetState = true }

                    AnimatedVisibility(
                        visibleState = state,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = delay)) +
                                slideInVertically(animationSpec = tween(500, delayMillis = delay)) { it / 2 }
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

    // Bottom Sheets
    if (showAddSheet) {
        AddExpenseSheet(
            uid = uid,
            db = db,
            onDismiss = { showAddSheet = false }
        )
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
            .background(SharkSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = SharkTypography.labelMedium, color = SharkTextMuted, fontSize = 10.sp)
        Text(
            String.format(Locale.US, "$%.0f", amount),
            style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun TransactionRow(expense: Expense, onClick: () -> Unit) {
    val categoryColor = getCategoryColor(expense.category)
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(expense.createdAt ?: Date())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                color = SharkTextPrimary
            )
            Text(
                text = dateStr,
                style = SharkTypography.labelMedium,
                color = SharkTextMuted,
                fontSize = 10.sp
            )
        }

        Text(
            text = String.format(Locale.US, (if (expense.category == "Income") "+" else "-") + "$%.2f", expense.amount),
            style = SharkTypography.bodyLarge.copy(fontFamily = DMMonoFontFamily),
            color = if (expense.category == "Income") SharkPositive else SharkNegative
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
                tint = SharkTextMuted
            )
            Spacer(Modifier.height(16.dp))
            Text("No transactions yet", style = SharkTypography.headlineMedium, color = SharkTextPrimary)
            Text(
                "Add one from the Snapshot tab",
                style = SharkTypography.bodyMedium,
                color = SharkTextMuted,
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
        containerColor = SharkBase,
        contentColor = SharkTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Transaction", style = SharkTypography.titleLarge)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SharkSurface,
                    unfocusedContainerColor = SharkSurface,
                    focusedTextColor = SharkTextPrimary,
                    unfocusedTextColor = SharkTextPrimary,
                    cursorColor = SharkGreen
                )
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SharkSurface,
                    unfocusedContainerColor = SharkSurface,
                    focusedTextColor = SharkTextPrimary,
                    unfocusedTextColor = SharkTextPrimary,
                    cursorColor = SharkGreen
                )
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
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SharkSurface,
                        unfocusedContainerColor = SharkSurface,
                        focusedTextColor = SharkTextPrimary,
                        unfocusedTextColor = SharkTextPrimary
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    expenseCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
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
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SharkSurface,
                    unfocusedContainerColor = SharkSurface,
                    focusedTextColor = SharkTextPrimary,
                    unfocusedTextColor = SharkTextPrimary,
                    cursorColor = SharkGreen
                )
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
                colors = ButtonDefaults.buttonColors(containerColor = SharkGreen)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
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
        containerColor = SharkBase,
        contentColor = SharkTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Transaction", style = SharkTypography.titleLarge)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SharkSurface,
                    unfocusedContainerColor = SharkSurface,
                    focusedTextColor = SharkTextPrimary,
                    unfocusedTextColor = SharkTextPrimary,
                    cursorColor = SharkGreen
                )
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SharkSurface,
                    unfocusedContainerColor = SharkSurface,
                    focusedTextColor = SharkTextPrimary,
                    unfocusedTextColor = SharkTextPrimary,
                    cursorColor = SharkGreen
                )
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
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SharkSurface,
                        unfocusedContainerColor = SharkSurface,
                        focusedTextColor = SharkTextPrimary,
                        unfocusedTextColor = SharkTextPrimary
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    expenseCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
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
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SharkSurface,
                    unfocusedContainerColor = SharkSurface,
                    focusedTextColor = SharkTextPrimary,
                    unfocusedTextColor = SharkTextPrimary,
                    cursorColor = SharkGreen
                )
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SharkRed,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, SharkRed)
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SharkGreen)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
