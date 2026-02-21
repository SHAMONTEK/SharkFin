package com.example.sharkfin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                // User is signed in, go to WelcomeActivity
                startActivity(Intent(this, WelcomeActivity::class.java))
            } else {
                // No user is signed in, go to MainActivity for login/signup
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish() // Finish SplashActivity so user can't go back to it
        }, 1500) // 1.5 second delay
    }
}