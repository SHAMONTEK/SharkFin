package com.example.sharkfin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharkfin.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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
    var accountType by remember { mutableStateOf("Individual") }
    val context = LocalContext.current

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            cloudSyncEnabled = doc.getBoolean("cloudSync") ?: true
            biometricsEnabled = doc.getBoolean("biometricLock") ?: false
            accountType = doc.getString("accountType") ?: "Individual"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        
        // Profile Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SharkSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    style = SharkTypography.displayLarge.copy(fontSize = 32.sp),
                    color = SharkGold
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(userName, style = SharkTypography.headlineLarge, color = SharkTextPrimary)
            Text(userEmail, style = SharkTypography.bodyMedium, color = SharkTextMuted)
            Spacer(Modifier.height(12.dp))
            Surface(
                color = SharkGoldGlow,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = accountType.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = SharkTypography.labelMedium,
                    color = SharkGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        SharkSectionHeader("ACCOUNT")
        Spacer(Modifier.height(12.dp))
        SharkCard {
            Column {
                ProfileToggleRow(
                    label = "Cloud Sync",
                    icon = Icons.Default.CloudSync,
                    checked = cloudSyncEnabled,
                    onCheckedChange = { 
                        cloudSyncEnabled = it
                        db.collection("users").document(uid).update("cloudSync", it)
                    }
                )
                Divider(color = SharkBorderSubtle, thickness = 1.dp)
                ProfileToggleRow(
                    label = "Biometric Lock",
                    icon = Icons.Default.Fingerprint,
                    checked = biometricsEnabled,
                    onCheckedChange = { 
                        biometricsEnabled = it
                        db.collection("users").document(uid).update("biometricLock", it)
                    }
                )
                Divider(color = SharkBorderSubtle, thickness = 1.dp)
                ProfileClickableRow(
                    label = "Export Data (CSV)",
                    icon = Icons.Default.FileDownload,
                    onClick = { /* Export Logic */ }
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        SharkSectionHeader("APP")
        Spacer(Modifier.height(12.dp))
        SharkCard {
            Column {
                ProfileInfoRow(
                    label = "Version",
                    value = "1.0.0", // BuildConfig.VERSION_NAME equivalent
                    icon = Icons.Default.Info
                )
                Divider(color = SharkBorderSubtle, thickness = 1.dp)
                ProfileClickableRow(
                    label = "Privacy Policy",
                    icon = Icons.Default.Security,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sharkfin.app/privacy"))
                        context.startActivity(intent)
                    }
                )
                Divider(color = SharkBorderSubtle, thickness = 1.dp)
                ProfileClickableRow(
                    label = "Terms of Service",
                    icon = Icons.Default.Gavel,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sharkfin.app/terms"))
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        
        SharkButtonSecondary(
            text = "Sign Out",
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onSignOut()
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ProfileToggleRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = SharkTypography.bodyLarge, color = SharkTextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SharkBase,
                checkedTrackColor = SharkGold,
                uncheckedThumbColor = SharkTextMuted,
                uncheckedTrackColor = SharkSurfaceHigh
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
        Text(label, style = SharkTypography.bodyLarge, color = SharkTextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = SharkTextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = SharkTypography.bodyLarge, color = SharkTextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = SharkTypography.bodyMedium, color = SharkTextMuted)
    }
}
