package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser

        // Safeguard: If user is somehow null, go back to login screen.
        if (firebaseUser == null) {
            goToLogin()
            return
        }

        val welcomeMessage = findViewById<TextView>(R.id.welcomeMessage)
        val accountInfo = findViewById<TextView>(R.id.accountInfo)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        welcomeMessage.text = "Welcome, ${firebaseUser.email}!"

        // Fetch user data and role
        db.collection("users").document(firebaseUser.uid).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null && userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)!!
                    db.collection("accounts").document(user.primaryAccountId)
                        .collection("members").document(user.uid).get()
                        .addOnSuccessListener { membershipDoc ->
                            val role = membershipDoc?.getString("role") ?: "N/A"
                            accountInfo.text = "Role: $role\nType: ${user.accountType}"
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}