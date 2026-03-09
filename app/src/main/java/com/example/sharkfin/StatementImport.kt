package com.example.sharkfin

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

@Composable
fun ImportStatementScreen(
    uid: String,
    db: FirebaseFirestore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    isImporting = true
                    errorMsg = null
                    importStatus = "Identifying file type..."
                    try {
                        val fileName = getFileName(context, it)
                        val expenses = if (fileName?.lowercase()?.endsWith(".pdf") == true) {
                            importStatus = "Parsing PDF..."
                            parsePdf(context, it)
                        } else {
                            importStatus = "Parsing CSV..."
                            parseCsv(context, it)
                        }

                        if (expenses.isEmpty()) {
                            errorMsg = "No valid transactions found in file."
                        } else {
                            importStatus = "Uploading ${expenses.size} transactions..."
                            uploadExpenses(db, uid, expenses)
                            importStatus = "Successfully imported ${expenses.size} transactions!"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Import failed: ${e.localizedMessage}"
                    } finally {
                        isImporting = false
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBase)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(56.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text("Import Statement", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "SUPPORTED FORMATS",
            color = SharkMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "CSV: Date, Description, Amount\nPDF: Cash App and standard bank statements.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(48.dp))

        if (isImporting) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = SharkNavy)
                Spacer(Modifier.height(16.dp))
                Text(importStatus ?: "Processing...", color = SharkNavy, fontSize = 14.sp)
            }
        } else {
            Button(
                onClick = { launcher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/pdf")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FileUpload, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Select CSV or PDF File", color = Color.White, fontWeight = FontWeight.Bold)
            }

            errorMsg?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = SharkRed, fontSize = 13.sp)
            }

            importStatus?.let {
                if (!isImporting && errorMsg == null) {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = SharkNavy, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name
}

private suspend fun parseCsv(context: Context, uri: Uri): List<Expense> = withContext(Dispatchers.IO) {
    val expenses = mutableListOf<Expense>()
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line = reader.readLine()
            if (line != null && (line.contains("Date", ignoreCase = true) || line.contains("Description", ignoreCase = true))) {
                line = reader.readLine()
            }

            while (line != null) {
                if (line.isBlank()) {
                    line = reader.readLine()
                    continue
                }
                
                val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                if (parts.size >= 3) {
                    try {
                        val dateStr = parts[0]
                        val description = parts[1]
                        val amountVal = parts[2].replace("$", "").replace(",", "").trim().toDouble()
                        
                        val date = try {
                            dateFormat.parse(dateStr)
                        } catch (e: Exception) {
                            null
                        }

                        val category = if (amountVal > 0) "Income" else "Other"
                        
                        expenses.add(
                            Expense(
                                id = "",
                                title = description,
                                amount = abs(amountVal),
                                category = category,
                                note = "Imported from CSV",
                                createdAt = date ?: Date()
                            )
                        )
                    } catch (e: Exception) { }
                }
                line = reader.readLine()
            }
        }
    }
    expenses
}

private suspend fun parsePdf(context: Context, uri: Uri): List<Expense> = withContext(Dispatchers.IO) {
    val expenses = mutableListOf<Expense>()
    val dateFormatShort = SimpleDateFormat("MMM d yyyy", Locale.US)
    val dateFormatLong = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    
    // Patterns based on Cash App and standard statement formats
    val yearPattern = Pattern.compile("(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE)
    val datePatternShort = Pattern.compile("^([A-Z][a-z]{2})\\s+(\\d{1,2})(?:\\s+|$)")
    val amountPattern = Pattern.compile("([+-]?\\s*\\$?[\\d,]+\\.\\d{2})\\s*$")
    val feePattern = Pattern.compile("\\$?\\d+\\.\\d{2}\\s*$") // Used to strip fee from description

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val reader = PdfReader(inputStream)
        val pdfDoc = PdfDocument(reader)
        
        var statementYear = Calendar.getInstance().get(Calendar.YEAR)
        var currentTxDate: String? = null
        var currentTxDesc = StringBuilder()

        for (i in 1..pdfDoc.numberOfPages) {
            val text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))
            
            // Try to find the year in the page text (e.g., "January 2026")
            val yearMatcher = yearPattern.matcher(text)
            if (yearMatcher.find()) {
                statementYear = yearMatcher.group(2)?.toIntOrNull() ?: statementYear
            }
            
            val lines = text.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Check for standard MM/DD/YYYY line first (non-Cash App format)
                val txPatternLong = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?)\\s+(-?\\$?[\\d,]+\\.\\d{2})")
                val mLong = txPatternLong.matcher(trimmed)
                if (mLong.find()) {
                    try {
                        val dateStr = mLong.group(1) ?: ""
                        val description = mLong.group(2) ?: "Transaction"
                        val amountStr = mLong.group(3) ?: "0.00"
                        val amountVal = amountStr.replace("$", "").replace(",", "").toDouble()
                        val date = dateFormatLong.parse(dateStr)
                        expenses.add(Expense("", description.trim(), abs(amountVal), if (amountVal > 0) "Income" else "Other", "Imported from PDF", date ?: Date()))
                        continue // Line processed
                    } catch (e: Exception) {}
                }

                // Cash App format: Starts with "Jan 1" or similar
                val dateMatcher = datePatternShort.matcher(trimmed)
                if (dateMatcher.find()) {
                    // Save previous if any? (Usually tx ends with amount line, so this is just safety)
                    currentTxDate = "${dateMatcher.group(1)} ${dateMatcher.group(2)}"
                    currentTxDesc = StringBuilder(trimmed.substring(dateMatcher.end()).trim())
                } else if (currentTxDate != null) {
                    currentTxDesc.append(" ").append(trimmed)
                }

                // If we're tracking a tx, check if this line contains the final amount
                if (currentTxDate != null) {
                    val amtMatcher = amountPattern.matcher(trimmed)
                    if (amtMatcher.find()) {
                        val amountFullStr = amtMatcher.group(1) ?: ""
                        val isIncome = amountFullStr.contains("+")
                        val cleanAmount = amountFullStr.replace("+", "").replace("-", "").replace("$", "").replace(",", "").trim()
                        val amountVal = cleanAmount.toDoubleOrNull() ?: 0.0
                        
                        try {
                            val date = dateFormatShort.parse("$currentTxDate $statementYear")
                            var finalDesc = currentTxDesc.toString()
                            
                            // Clean up description by removing amount and potential fee
                            val lastAmtIndex = finalDesc.lastIndexOf(amountFullStr)
                            if (lastAmtIndex != -1) {
                                finalDesc = finalDesc.substring(0, lastAmtIndex).trim()
                            }
                            
                            val feeMatcher = feePattern.matcher(finalDesc)
                            if (feeMatcher.find()) {
                                val lastFeeIndex = finalDesc.lastIndexOf(feeMatcher.group())
                                if (lastFeeIndex != -1) {
                                    finalDesc = finalDesc.substring(0, lastFeeIndex).trim()
                                }
                            }
                            
                            expenses.add(
                                Expense(
                                    id = "",
                                    title = finalDesc.ifEmpty { "Transaction" },
                                    amount = amountVal,
                                    category = if (isIncome) "Income" else "Other",
                                    note = "Imported from PDF",
                                    createdAt = date ?: Date()
                                )
                            )
                        } catch (e: Exception) { }
                        
                        // Reset for next transaction
                        currentTxDate = null
                        currentTxDesc = StringBuilder()
                    }
                }
            }
        }
        pdfDoc.close()
    }
    expenses
}

private suspend fun uploadExpenses(db: FirebaseFirestore, uid: String, expenses: List<Expense>) = withContext(Dispatchers.IO) {
    val collection = db.collection("users").document(uid).collection("expenses")
    expenses.chunked(500).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { expense ->
            val docRef = collection.document()
            batch.set(docRef, expense)
        }
        com.google.android.gms.tasks.Tasks.await(batch.commit())
    }
}