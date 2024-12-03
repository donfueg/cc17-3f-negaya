package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Register : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize the views
        emailEditText = findViewById(R.id.email)
        usernameEditText = findViewById(R.id.contact)
        phoneEditText = findViewById(R.id.phone)
        passwordEditText = findViewById(R.id.password)
        registerButton = findViewById(R.id.button)
        loginTextView = findViewById(R.id.login)

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Handle register button click
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val username = usernameEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Validate input fields
            if (email.isNotEmpty() && username.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty()) {
                if (isPhoneValid(phone)) {
                    // Hash the password before storing it
                    val hashedPassword = hashPassword(password)

                    // Insert the user into the database
                    val result = dbHelper.insertUser(username, hashedPassword, email, phone)

                    if (result != -1L) { // Success
                        // Pass username to the Verification activity
                        val intent = Intent(this, Verification::class.java).apply {
                            putExtra("EXTRA_USERNAME", username) // Pass username
                        }
                        startActivity(intent)
                        finish() // Close the register screen
                    } else {
                        // Show failure message
                        Toast.makeText(this, "Registration failed, try again!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Show error message for invalid phone number
                    Toast.makeText(this, "Invalid phone number. Please enter numbers only.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show error message for empty fields
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Redirect to login if user already has an account
        loginTextView.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
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

    // Function to validate the phone number
    private fun isPhoneValid(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]+$")) // Regex checks if the phone contains only digits
    }
}
