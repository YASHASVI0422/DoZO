package com.campus.dozo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campus.dozo.databinding.ActivitySignupBinding
import com.campus.dozo.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInputs(name, email, phone, password, confirmPassword)) {
                signupUser(name, email, phone, password)
            }
        }

        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun validateInputs(
        name: String, email: String, phone: String,
        password: String, confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) { binding.etName.error = "Name required"; return false }
        if (email.isEmpty()) { binding.etEmail.error = "Email required"; return false }
        if (phone.isEmpty()) { binding.etPhone.error = "Phone required"; return false }
        if (password.isEmpty()) { binding.etPassword.error = "Password required"; return false }
        if (password.length < 6) { binding.etPassword.error = "Password too short"; return false }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords don't match"; return false
        }
        return true
    }

    private fun signupUser(name: String, email: String, phone: String, password: String) {
        binding.btnSignup.isEnabled = false
        binding.btnSignup.text = "Creating account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    createUserProfile(userId, name, email, phone)
                } else {
                    binding.btnSignup.isEnabled = true
                    binding.btnSignup.text = "Sign Up"
                    Toast.makeText(
                        this,
                        "Signup failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * ✅ Creates a Firestore document with all expected fields.
     */
    private fun createUserProfile(userId: String, name: String, email: String, phone: String) {
        val userMap = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "bio" to "Hey there! I'm using Dozo.",
            "tasksPosted" to 0,
            "tasksCompleted" to 0,
            "rating" to 0.0
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("Signup", "✅ User profile created successfully")
                Toast.makeText(this, "Account created 🎉", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSignup.isEnabled = true
                binding.btnSignup.text = "Sign Up"
                Log.e("Signup", "❌ Failed to save user: ${e.message}")
                Toast.makeText(
                    this,
                    "Error saving user: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
