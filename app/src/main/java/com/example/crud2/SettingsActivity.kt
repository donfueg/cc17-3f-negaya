package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve theme preference and apply theme
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            setTheme(R.style.Theme_App_Dark)
        } else {
            setTheme(R.style.Theme_App_Light)
        }

        // Hide the default ActionBar (removes "CRUD2" text)
        supportActionBar?.hide()

        setContentView(R.layout.activity_settings)

        // Get username from SharedPreferences
        val username = sharedPreferences.getString("username", "User")

        // Set header subtitle to "Hello <username>"
        val headerSubtitle = findViewById<TextView>(R.id.headerSubtitle)
        headerSubtitle.text = "Hello $username"

        // Handle back button click in custom header
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Logout button handling
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun logoutUser() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)

        editor.remove("username")
        editor.putBoolean("is_logged_in", false)
        editor.apply()

        editor.putBoolean("dark_theme", isDarkTheme)
        editor.apply()

        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
