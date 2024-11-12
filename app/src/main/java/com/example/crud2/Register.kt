package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Register : AppCompatActivity() {
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var contactEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginTextView: TextView
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        registerButton = findViewById(R.id.button)
        emailEditText = findViewById(R.id.email)
        contactEditText = findViewById(R.id.contact)
        passwordEditText = findViewById(R.id.password)
        loginTextView = findViewById(R.id.login)

        // Initialize the database helper
        dbHelper = DatabaseHelper(this)

        // Set the listener for the register button
        registerButton.setOnClickListener {
            // Validate the inputs
            if (validateInputs()) {
                // Get the values from the inputs
                val email = emailEditText.text.toString().trim()
                val username = contactEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()

                // Insert the new user into the database
                val result = dbHelper.insertUser(username, password, email)

                // If user is successfully registered (not -1), proceed
                if (result != -1L) {
                    // Registration successful, show message and go to NumberPage activity
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, NumberPage::class.java)
                    intent.putExtra("username", username) // Pass the username
                    startActivity(intent)
                    finish()  // Close the register screen so the user can't go back to it
                } else {
                    // Username already exists, show error message
                    Toast.makeText(this, "Username already exists, please try another.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show a Toast message to inform the user that they need to fill all fields
                Toast.makeText(this, "Please fill all fields and ensure they are valid.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set listener for the login link (to navigate to login activity)
        loginTextView.setOnClickListener {
            // Navigate to the login activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    // Function to validate the inputs
    private fun validateInputs(): Boolean {
        val email = emailEditText.text.toString().trim()
        val username = contactEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Check if any of the fields are empty
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return false
        }

        if (username.isEmpty()) {
            contactEditText.error = "Username is required"
            contactEditText.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return false
        }

        // Check if the email is a valid email address
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email address"
            emailEditText.requestFocus()
            return false
        }

        // Check if the contact is a valid number (you can customize the number format as needed)
        if (!username.matches(Regex("[0-9]+"))) {
            contactEditText.error = "Please enter a valid contact number"
            contactEditText.requestFocus()
            return false
        }

        // Check if the email already exists in the database
        if (dbHelper.isEmailExists(email)) {
            emailEditText.error = "Email already exists"
            emailEditText.requestFocus()
            return false
        }

        // If all fields are filled and valid, return true
        return true
    }
}