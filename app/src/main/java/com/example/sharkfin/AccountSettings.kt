package com.example.sharkfin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AccountSettingsScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    var showTutorial by remember { mutableStateOf(true) }
    
    var cloudSync by remember { mutableStateOf(true) }
    var biometricLock by remember { mutableStateOf(false) }
    var emailReports by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            cloudSync = doc.getBoolean("cloudSync") ?: true
            biometricLock = doc.getBoolean("biometricLock") ?: false
            emailReports = doc.getBoolean("emailReports") ?: true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Account Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))
            
            SettingToggle("Cloud Sync", "Keep data synced across devices", cloudSync) {
                cloudSync = it
                db.collection("users").document(uid).update("cloudSync", it)
            }
            
            SettingToggle("Biometric Lock", "Use Fingerprint/FaceID to open", biometricLock) {
                biometricLock = it
                db.collection("users").document(uid).update("biometricLock", it)
            }
            
            SettingToggle("Email Reports", "Weekly financial summary", emailReports) {
                emailReports = it
                db.collection("users").document(uid).update("emailReports", it)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Danger Zone", color = Color(0xFFef4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { /* Logic to wipe collections */ }, 
                modifier = Modifier.fillMaxWidth().height(50.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444).copy(alpha = 0.1f)), 
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Delete All Data", color = Color(0xFFef4444))
            }
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Security & Privacy",
                description = "Manage your data synchronization and security preferences. We recommend enabling Biometric Lock for maximum protection of your financial records.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SharkMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange, 
            colors = SwitchDefaults.colors(checkedThumbColor = SharkGreen, checkedTrackColor = SharkGreen.copy(alpha = 0.3f))
        )
    }
}
