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
        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        val profileNameTextView = findViewById<TextView>(R.id.profileName)
        val notificationsSwitch = findViewById<Switch>(R.id.notificationsSwitch)
        val lightThemeRadioButton = findViewById<RadioButton>(R.id.lightThemeRadioButton)
        val darkThemeRadioButton = findViewById<RadioButton>(R.id.darkThemeRadioButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // Retrieve the username from SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "Guest")
        profileNameTextView.text = "Username: $username"

        // Set default theme based on saved preferences (if any)
        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            darkThemeRadioButton.isChecked = true
        } else {
            lightThemeRadioButton.isChecked = true
        }

        // Theme selection handling
        lightThemeRadioButton.setOnClickListener {
            saveThemePreference(false)
        }

        darkThemeRadioButton.setOnClickListener {
            saveThemePreference(true)
        }

        // Notifications toggle handling
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Handle enabling/disabling notifications logic here
        }

        // Log out button handling
        logoutButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Finish SettingsActivity to prevent returning to it
        }
    }

    // Save theme preference in SharedPreferences
    private fun saveThemePreference(isDarkTheme: Boolean) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("dark_theme", isDarkTheme).apply()
    }
}
