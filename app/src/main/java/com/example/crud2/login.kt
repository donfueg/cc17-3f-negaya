package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var dbHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is already logged in
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        // If user is logged in, go directly to the Dashboard
        if (isLoggedIn) {
            val username = sharedPreferences.getString("username", "") ?: ""
            navigateToDashboard(username)
            return  // Important: exit onCreate to prevent setting content view
        }

        // If not logged in, display the login screen
        setContentView(R.layout.activity_login)

        // Initialize FirebaseHelper
        dbHelper = FirebaseHelper(this)

        // Find views
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.register)

        // Set up the Login button click listener
        loginButton.setOnClickListener {
            handleLogin()
        }

        // Set up the Register link click listener
        registerTextView.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }

    private fun handleLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate inputs
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Hash the password
        val hashedPassword = hashPassword(password)

        // Validate the user
        dbHelper.validateUserByUsernameAndPassword(username, hashedPassword) { isValid ->
            runOnUiThread {
                if (isValid) {
                    // Save login state in SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("username", username) // Save the username
                    editor.putBoolean("is_logged_in", true) // Set the login state to true
                    editor.apply()  // Commit the changes to SharedPreferences

                    // Set the current user in FirebaseHelper
                    dbHelper.setCurrentUser(username) { success ->
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                                navigateToDashboard(username)
                            } else {
                                Toast.makeText(this, "Error setting up user session", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToDashboard(username: String) {
        val intent = Intent(this, Dashboard::class.java).apply {
            putExtra("EXTRA_USERNAME", username)
            // Add flags to clear the activity stack
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun hashPassword(password: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(password.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            password // Fallback to plain password if hashing fails
        }
    }
}