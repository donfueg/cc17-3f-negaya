package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DatabaseHelper(this)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Check if the user is admin
            if (username == "admin" && password == "admin123") {
                Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show()
                // Redirect to Admin Dashboard
                val intent = Intent(this, Dashboard::class.java)
                startActivity(intent)
            } else {
                // Normal user login check
                val userNumber = db.getUserNumber(username)
                if (userNumber != null) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NumberPage::class.java)
                    intent.putExtra("username", username)
                    intent.putExtra("userNumber", userNumber)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
