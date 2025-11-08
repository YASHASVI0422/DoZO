package com.campus.dozo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Test Firebase connection
        Log.d("SplashActivity", "Testing Firebase connection...")
        testFirebaseConnection()

        // Delay for 2 seconds then check if user is logged in
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            val intent = if (currentUser != null) {
                Log.d("SplashActivity", "User logged in: ${currentUser.email}")
                Intent(this, MainActivity::class.java)
            } else {
                Log.d("SplashActivity", "No user logged in")
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 2000)
    }

    private fun testFirebaseConnection() {
        // Try to read from Firestore
        db.collection("test")
            .limit(1)
            .get()
            .addOnSuccessListener {
                Log.d("SplashActivity", "✅ Firebase connected successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("SplashActivity", "❌ Firebase connection failed", e)
            }
    }
}