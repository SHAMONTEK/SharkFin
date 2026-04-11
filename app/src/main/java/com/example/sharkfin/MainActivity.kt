package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.example.sharkfin.ui.theme.SharkFinTheme
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        auth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            if (user != null) {
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                setContent {
                    SharkFinTheme {
                        AuthScreen(
                            onLogin = { email, password ->
                                performLogin(email, password)
                            },
                            onSignup = { name, email, password, accountType ->
                                performSignup(name, email, password, accountType)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun performLogin(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            showFeedback("Email and password cannot be empty")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnFailureListener {
                showFeedback("Login failed: ${it.message}")
            }
    }

    private fun performSignup(name: String, email: String, password: String, accountType: String) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showFeedback("All fields are required")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user!!
                    val newAccountId = db.collection("accounts").document().id
                    val batch = db.batch()

                    val account = Account(
                        accountId = newAccountId,
                        type = accountType,
                        name = "${name}'s ${accountType} Account",
                        createdByUid = firebaseUser.uid
                    )
                    batch.set(db.collection("accounts").document(newAccountId), account)

                    val user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        displayName = name,
                        accountType = accountType,
                        primaryAccountId = newAccountId
                    )
                    batch.set(db.collection("users").document(firebaseUser.uid), user)

                    val membership = Membership(
                        uid = firebaseUser.uid,
                        role = "OWNER"
                    )
                    batch.set(
                        db.collection("accounts")
                            .document(newAccountId)
                            .collection("members")
                            .document(firebaseUser.uid),
                        membership
                    )

                    batch.commit()
                        .addOnSuccessListener {
                            val firstName = name.split(" ")[0]
                            val intent = Intent(this, SplashActivity::class.java)
                            intent.putExtra("WELCOME_NAME", firstName)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showFeedback("Signup failed: ${e.message}")
                        }
                } else {
                    showFeedback("Signup failed: ${task.exception?.message}")
                }
            }
    }

    private fun showFeedback(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
