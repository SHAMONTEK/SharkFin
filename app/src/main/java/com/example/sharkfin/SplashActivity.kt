package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        
        val welcomeName = intent.getStringExtra("WELCOME_NAME")
        val welcomeUserText = findViewById<TextView>(R.id.welcomeUserText)

        if (welcomeName != null) {
            // This is the post-registration welcome splash
            welcomeUserText.text = "Welcome, $welcomeName"
            welcomeUserText.visibility = View.VISIBLE
            findViewById<View>(R.id.splashTagline).visibility = View.GONE
            
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMain()
            }, 2500)
        } else {
            // Standard app launch
            Handler(Looper.getMainLooper()).postDelayed({
                if (auth.currentUser != null) {
                    navigateToMain()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }, 2000)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
