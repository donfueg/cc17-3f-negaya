package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        emailEditText = findViewById(R.id.email)
        usernameEditText = findViewById(R.id.contact)
        phoneEditText = findViewById(R.id.phone)
        passwordEditText = findViewById(R.id.password)
        registerButton = findViewById(R.id.register)
        loginTextView = findViewById(R.id.login)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Register button click listener
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && username.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty()) {
                if (isPhoneValid(phone)) {
                    createAccount(email, username, phone, password)
                } else {
                    Toast.makeText(this, "Invalid phone number. Use digits only.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            }
        }

        // Login redirect
        loginTextView.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    // Function to create an account with Firebase Authentication
    private fun createAccount(email: String, username: String, phone: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserToFirestore(userId, username, email, phone, password)
                    } else {
                        Toast.makeText(this, "Failed to retrieve user ID.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to save user data to Firestore
    private fun saveUserToFirestore(userId: String, username: String, email: String, phone: String, password: String) {
        val hashedPassword = hashPassword(password) // Hash the password before saving
        val user = hashMapOf(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "phone" to phone,
            "password" to hashedPassword // Store hashed password
        )

        firestore.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navigateToVerification(phone) // Pass phone number to Verification activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Navigate to verification activity
    private fun navigateToVerification(phone: String) {
        val intent = Intent(this, Verification::class.java).apply {
            putExtra("EXTRA_PHONE", phone) // Pass phone number to Verification activity
        }
        startActivity(intent)
        finish()
    }

    // Function to hash the password
    private fun hashPassword(password: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    // Function to validate phone number
    private fun isPhoneValid(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]+$"))
    }
}
