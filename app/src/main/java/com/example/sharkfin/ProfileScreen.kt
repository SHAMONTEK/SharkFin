package com.example.sharkfin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
            .background(SharkBlack)
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
                    .background(SharkSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharkGreen
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (userEmail.isNotEmpty()) {
                Text(userEmail, fontSize = 13.sp, color = SharkMuted)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .background(SharkGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = accountType.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharkGreen,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── Account Section ────────────────────────────────────────────
        SharkSectionHeader("ACCOUNT")
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
            ProfileClickableRow(
                label = "Export Data (CSV)",
                icon = Icons.Default.FileDownload,
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
}

@Composable
fun ProfileToggleRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = SharkBlack,
                checkedTrackColor  = SharkGreen,
                uncheckedThumbColor = SharkMuted,
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
        Icon(icon, null, tint = SharkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = SharkMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SharkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = Color.White, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = SharkMuted)
    }
}