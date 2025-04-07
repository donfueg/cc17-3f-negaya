package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve theme preference and apply theme
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            setTheme(R.style.Theme_App_Dark) // Replace with your dark theme style
        } else {
            setTheme(R.style.Theme_App_Light) // Replace with your light theme style
        }

        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        val profileNameTextView = findViewById<TextView>(R.id.profileName)
        val notificationsSwitch = findViewById<Switch>(R.id.notificationsSwitch)
        val lightThemeRadioButton = findViewById<RadioButton>(R.id.lightThemeRadioButton)
        val darkThemeRadioButton = findViewById<RadioButton>(R.id.darkThemeRadioButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // Retrieve the username from SharedPreferences
        val username = sharedPreferences.getString("username", "Guest")
        profileNameTextView.text = "Username: $username"

        // Set default theme based on saved preferences
        if (isDarkTheme) {
            darkThemeRadioButton.isChecked = true
        } else {
            lightThemeRadioButton.isChecked = true
        }

        // Theme selection handling
        lightThemeRadioButton.setOnClickListener {
            saveThemePreference(false)
            restartActivity() // Restart activity to apply new theme
        }

        darkThemeRadioButton.setOnClickListener {
            saveThemePreference(true)
            restartActivity() // Restart activity to apply new theme
        }

        // Notifications toggle handling
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleNotifications(isChecked)
        }

        // Log out button handling
        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    // Save theme preference in SharedPreferences
    private fun saveThemePreference(isDarkTheme: Boolean) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("dark_theme", isDarkTheme).apply()
    }

    // Handle notification toggling
    private fun handleNotifications(isChecked: Boolean) {
        // Example logic: Use Firebase or local notification logic here
        if (isChecked) {
            // Enable notifications (e.g., subscribe to Firebase topic)
        } else {
            // Disable notifications (e.g., unsubscribe from Firebase topic)
        }
    }

    // Logout user and navigate back to login screen
    private fun logoutUser() {
        // Clear user-related data
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("username").apply()

        // Navigate to login activity
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish() // Finish SettingsActivity to prevent returning to it
    }

    // Restart activity to apply the new theme
    private fun restartActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        finish() // Finish current activity
        startActivity(intent) // Start new activity
    }
}
