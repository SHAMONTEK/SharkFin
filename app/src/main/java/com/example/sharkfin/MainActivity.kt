package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // This listener will react to login/logout events
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in, so navigate to WelcomeActivity
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // User is signed out, so show the login/signup UI
                setContentView(R.layout.activity_main)
                setupAuthUI()
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

    private fun setupAuthUI() {
        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val typeGroup = findViewById<RadioGroup>(R.id.accountTypeGroup)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        btnSignup.setOnClickListener {
            btnSignup.isEnabled = false
            val email = emailField.text.toString().trim()
            val pass = passwordField.text.toString().trim()

            val accountType = when (typeGroup.checkedRadioButtonId) {
                R.id.radioIndividual -> "INDIVIDUAL"
                R.id.radioJoint -> "JOINT"
                R.id.radioFamily -> "FAMILY"
                R.id.radioBusiness -> "BUSINESS"
                else -> ""
            }

            if (email.isEmpty() || pass.isEmpty() || accountType.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                btnSignup.isEnabled = true
                return@setOnClickListener
            }

            // This will trigger the AuthStateListener on success
            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user!!
                    val newAccountId = db.collection("accounts").document().id
                    val batch = db.batch()

                    val account = Account(newAccountId, accountType, "${email.split("@")[0]}'s $accountType Account", firebaseUser.uid)
                    batch.set(db.collection("accounts").document(newAccountId), account)

                    val user = User(firebaseUser.uid, email, null, accountType, newAccountId)
                    batch.set(db.collection("users").document(firebaseUser.uid), user)

                    val role = when (accountType) {
                        "BUSINESS" -> "ADMIN"
                        "FAMILY" -> "ORGANIZER"
                        else -> "OWNER"
                    }
                    val membership = Membership(firebaseUser.uid, role)
                    batch.set(db.collection("accounts").document(newAccountId).collection("members").document(firebaseUser.uid), membership)

                    batch.commit().addOnFailureListener { e ->
                        Toast.makeText(this, "Database setup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    btnSignup.isEnabled = true
                }
            }
        }

        btnLogin.setOnClickListener {
            btnLogin.isEnabled = false
            val email = emailField.text.toString().trim()
            val pass = passwordField.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = true
                return@setOnClickListener
            }
            
            // This will trigger the AuthStateListener on success
            auth.signInWithEmailAndPassword(email, pass).addOnFailureListener {
                btnLogin.isEnabled = true
                Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
