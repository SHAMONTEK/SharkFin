package com.example.sharkfin

// ─── Imports ───────────────────────────────────────────────────────────────
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    // Firebase Auth instance — handles login/signup/session
    private lateinit var auth: FirebaseAuth

    // Firestore instance — handles saving user data to the database
    private val db = FirebaseFirestore.getInstance()

    // Auth state listener — watches if a user is logged in or not
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Makes the app draw behind the status bar and nav bar (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Get the Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        // This listener fires every time the login state changes
        // — when the app opens, when you log in, when you log out
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            if (user != null) {
                // User is logged in → send them to the home screen
                val intent = Intent(this, WelcomeActivity::class.java)
                // Clear the back stack so they can't press back to get to login
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // No user logged in → show the AuthScreen (login/signup)
                setContent {
                    AuthScreen(
                        onLogin = { email, password ->
                            // User tapped "Access Account"
                            performLogin(email, password)
                        },
                        onSignup = { name, email, password, accountType ->
                            // User tapped "Create Portfolio"
                            // Now includes accountType — no more second screen!
                            performSignup(name, email, password, accountType)
                        }
                    )
                }
            }
        }
    }

    // ── Lifecycle: attach/detach the auth listener ──────────────────────────
    // onStart = app becomes visible → start listening for auth changes
    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    // onStop = app goes to background → stop listening to save resources
    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    // ── Login Logic ─────────────────────────────────────────────────────────
    private fun performLogin(email: String, password: String) {
        // Basic validation — don't even hit Firebase if fields are empty
        if (email.isEmpty() || password.isEmpty()) {
            showFeedback("Email and password cannot be empty")
            return
        }

        // Tell Firebase to sign in with email + password
        auth.signInWithEmailAndPassword(email, password)
            .addOnFailureListener {
                // If it fails, show the error message to the user
                showFeedback("Login failed: ${it.message}")
            }
        // Success is handled automatically by the authStateListener above
        // — it detects the new user and navigates to WelcomeActivity
    }

    // ── Signup Logic ────────────────────────────────────────────────────────
    // Now lives HERE instead of RegisterActivity — one screen, no confusion
    private fun performSignup(name: String, email: String, password: String, accountType: String) {
        // Make sure nothing is blank before hitting Firebase
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showFeedback("All fields are required")
            return
        }

        // Step 1: Create the Firebase Auth account (email + password)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Got the new Firebase user
                    val firebaseUser = task.result?.user!!

                    // Step 2: Generate a new unique ID for this user's account in Firestore
                    val newAccountId = db.collection("accounts").document().id

                    // Step 3: Use a batch write so all 3 saves happen together
                    // (If one fails, none of them save — keeps data consistent)
                    val batch = db.batch()

                    // Build the Account object (the financial account)
                    val account = Account(
                        accountId = newAccountId,
                        type = accountType,                              // e.g. "INDIVIDUAL"
                        name = "${name}'s ${accountType} Account",       // e.g. "Tae's Business Account"
                        createdByUid = firebaseUser.uid
                    )
                    // Queue the account save
                    batch.set(
                        db.collection("accounts").document(newAccountId),
                        account
                    )

                    // Build the User object (their profile)
                    val user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        displayName = name,
                        accountType = accountType,
                        primaryAccountId = newAccountId
                    )
                    // Queue the user profile save
                    batch.set(
                        db.collection("users").document(firebaseUser.uid),
                        user
                    )

                    // Build the Membership object (their role in the account)
                    // New users are always the OWNER of their own account
                    val membership = Membership(
                        uid = firebaseUser.uid,
                        role = "OWNER"
                    )
                    // Queue the membership save (nested under the account)
                    batch.set(
                        db.collection("accounts")
                            .document(newAccountId)
                            .collection("members")
                            .document(firebaseUser.uid),
                        membership
                    )

                    // Step 4: Fire all 3 writes at once
                    batch.commit()
                        .addOnSuccessListener {
                            // All data saved! Show the welcome splash with their first name
                            val firstName = name.split(" ")[0]  // grab just "Tae" from "Tae Knight"
                            val intent = Intent(this, SplashActivity::class.java)
                            intent.putExtra("WELCOME_NAME", firstName)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Firestore save failed — let them know
                            showFeedback("Signup failed: ${e.message}")
                        }
                } else {
                    // Firebase Auth creation failed (bad email format, weak password, etc.)
                    showFeedback("Signup failed: ${task.exception?.message}")
                }
            }
    }

    // ── Snackbar Helper ─────────────────────────────────────────────────────
    // Shows a little popup message at the bottom of the screen
    private fun showFeedback(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}