package com.example.sharkfin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    uid: String,
    userEmail: String,
    userName: String,
    onSignOut: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var cloudSyncEnabled by remember { mutableStateOf(true) }
    var biometricsEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var incognitoMode by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf("USD ($)") }
    var accountType by remember { mutableStateOf("Individual") }
    val context = LocalContext.current
    var showWizard by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            cloudSyncEnabled = doc.getBoolean("cloudSync") ?: true
            biometricsEnabled = doc.getBoolean("biometricLock") ?: false
            notificationsEnabled = doc.getBoolean("notifications") ?: true
            incognitoMode = doc.getBoolean("incognito") ?: false
            currency = doc.getString("currency") ?: "USD ($)"
            accountType = doc.getString("accountType") ?: "Individual"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(60.dp))

        // ── Avatar + Name ──────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SharkSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharkGold
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
            if (userEmail.isNotEmpty()) {
                Text(userEmail, fontSize = 13.sp, color = SharkSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .background(SharkGold.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = accountType.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharkGold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── Discovery Section ──────────────────────────────────────────
        SharkSectionHeader("FINANCIAL ENGINE")
        Spacer(Modifier.height(12.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SharkGold.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .border(1.dp, SharkGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .clickable { showWizard = true }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoFixHigh, null, tint = SharkGold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text("Run Financial Discovery", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SharkLabel)
            Text("Update your core numbers for accurate scores", fontSize = 12.sp, color = SharkSecondary, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(28.dp))

        // ── Account Section ────────────────────────────────────────────
        SharkSectionHeader("PREFERENCES")
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SharkSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SharkBorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp)
        ) {
            ProfileToggleRow(
                label = "Cloud Sync",
                icon = Icons.Default.CloudSync,
                checked = cloudSyncEnabled,
                onCheckedChange = {
                    cloudSyncEnabled = it
                    db.collection("users").document(uid).update("cloudSync", it)
                }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileToggleRow(
                label = "Biometric Lock",
                icon = Icons.Default.Fingerprint,
                checked = biometricsEnabled,
                onCheckedChange = {
                    biometricsEnabled = it
                    db.collection("users").document(uid).update("biometricLock", it)
                }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileToggleRow(
                label = "Smart Notifications",
                icon = Icons.Default.NotificationsActive,
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    db.collection("users").document(uid).update("notifications", it)
                }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileToggleRow(
                label = "Incognito Mode",
                icon = Icons.Default.VisibilityOff,
                checked = incognitoMode,
                onCheckedChange = {
                    incognitoMode = it
                    db.collection("users").document(uid).update("incognito", it)
                }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileClickableRow(
                label = "Currency ($currency)",
                icon = Icons.Default.CurrencyExchange,
                onClick = { /* Toggle or Open picker */ }
            )
        }

        Spacer(Modifier.height(28.dp))

        SharkSectionHeader("DATA MANAGEMENT")
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SharkSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SharkBorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp)
        ) {
            ProfileClickableRow(
                label = "Export Data (CSV)",
                icon = Icons.Default.FileDownload,
                onClick = { }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileClickableRow(
                label = "Clear Local Cache",
                icon = Icons.Default.DeleteSweep,
                onClick = { }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── App Section ────────────────────────────────────────────────
        SharkSectionHeader("APP")
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SharkSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SharkBorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp)
        ) {
            ProfileInfoRow(label = "Version", value = "1.2.0", icon = Icons.Default.Info)
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileClickableRow(
                label = "Privacy Policy",
                icon = Icons.Default.Security,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sharkfin.app/privacy"))
                    context.startActivity(intent)
                }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(SharkBorderSubtle))
            ProfileClickableRow(
                label = "Terms of Service",
                icon = Icons.Default.Gavel,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sharkfin.app/terms"))
                    context.startActivity(intent)
                }
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── Sign Out ───────────────────────────────────────────────────
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onSignOut()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SharkRed.copy(alpha = 0.12f)
            )
        ) {
            Icon(Icons.Default.Logout, null, tint = SharkRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", color = SharkRed, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(100.dp))
    }

    if (showWizard) {
        FinancialDiscoveryWizard(uid, db) { showWizard = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinancialDiscoveryWizard(uid: String, db: FirebaseFirestore, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    
    var income by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var targetSavings by remember { mutableStateOf("") }
    var debtTotal by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SharkBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SharkSurface) },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(4) { i ->
                    Box(Modifier.height(4.dp).weight(1f).clip(CircleShape).background(if (i <= pagerState.currentPage) SharkGold else SharkSurface))
                }
            }
            
            Spacer(Modifier.height(32.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> WizardStep(
                        icon = Icons.Default.Payments,
                        title = "What's your total monthly income?",
                        subtitle = "Include all sources after tax for accuracy.",
                        value = income,
                        onValueChange = { income = it }
                    )
                    1 -> WizardStep(
                        icon = Icons.Default.Home,
                        title = "Total monthly obligations?",
                        subtitle = "Rent, car, insurance, and utilities.",
                        value = rent,
                        onValueChange = { rent = it }
                    )
                    2 -> WizardStep(
                        icon = Icons.Default.Savings,
                        title = "Target monthly savings?",
                        subtitle = "We recommend at least 20% of net income.",
                        value = targetSavings,
                        onValueChange = { targetSavings = it }
                    )
                    3 -> WizardStep(
                        icon = Icons.Default.CreditCard,
                        title = "Any outstanding debt?",
                        subtitle = "Total balance of all credit cards & loans.",
                        value = debtTotal,
                        onValueChange = { debtTotal = it }
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 3) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        // SAVE TO FIRESTORE
                        val discoveryData = mapOf(
                            "monthlyIncome" to (income.toDoubleOrNull() ?: 0.0),
                            "monthlyObligations" to (rent.toDoubleOrNull() ?: 0.0),
                            "targetSavings" to (targetSavings.toDoubleOrNull() ?: 0.0),
                            "totalDebt" to (debtTotal.toDoubleOrNull() ?: 0.0),
                            "discoveryCompleted" to true,
                            "lastDiscoveryUpdate" to System.currentTimeMillis()
                        )
                        db.collection("users").document(uid).update(discoveryData)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGold)
            ) {
                Text(if (pagerState.currentPage == 3) "Finish & Recalculate" else "Continue", color = SharkBg, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun WizardStep(icon: ImageVector, title: String, subtitle: String, value: String, onValueChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(80.dp).background(SharkGold.copy(0.1f), CircleShape), Alignment.Center) {
            Icon(icon, null, tint = SharkGold, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, color = SharkLabel, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = SharkSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = SharkLabel),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("$", fontSize = 32.sp, color = SharkGold) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SharkGold,
                unfocusedBorderColor = SharkSurface,
                cursorColor = SharkGold
            )
        )
    }
}

@Composable
fun ProfileToggleRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = SharkLabel, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = SharkBg,
                checkedTrackColor  = SharkGold,
                uncheckedThumbColor = SharkSecondary,
                uncheckedTrackColor = SharkSurface
            )
        )
    }
}

@Composable
fun ProfileClickableRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = SharkLabel, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = SharkSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = SharkLabel, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = SharkSecondary)
    }
}