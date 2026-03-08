package com.example.sharkfin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun ProfileSettingsScreen(displayName: String, onSave: (String) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    
    var showTutorial by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf(displayName) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            profilePicUrl = doc.getString("profilePicUrl")
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            val ref = storage.reference.child("profile_pics/$uid.jpg")
            ref.putFile(it).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    db.collection("users").document(uid).update("profilePicUrl", url)
                    profilePicUrl = url
                    isUploading = false
                }
            }.addOnFailureListener {
                isUploading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Edit Profile", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .glassCard(cornerRadius = 50f, alpha = 0.1f), 
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUrl != null) {
                        AsyncImage(
                            model = profilePicUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(name.firstOrNull()?.toString() ?: "S", color = SharkGreen, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    if (isUploading) {
                        CircularProgressIndicator(color = SharkGreen, modifier = Modifier.size(40.dp))
                    }
                }
                IconButton(
                    onClick = { launcher.launch("image/*") }, 
                    modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).glassCard(cornerRadius = 16f, alpha = 0.5f)
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            SheetInputField(name, { name = it }, "Display Name", "Enter your name")
            Spacer(modifier = Modifier.height(24.dp))
            SheetInputField(uid.take(8), { }, "User ID", "Read only", enabled = false)

            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { onSave(name) }, 
                modifier = Modifier.fillMaxWidth().height(54.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = SharkGreen), 
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Profile", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Your Identity",
                description = "Personalize your SharkFin experience. Changing your display name updates how you appear across all shared financial reports and trackers.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}
