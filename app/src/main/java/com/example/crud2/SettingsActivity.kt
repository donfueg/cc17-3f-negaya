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
            setTheme(R.style.Theme_App_Dark)
        } else {
            setTheme(R.style.Theme_App_Light)
        }

        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // Log out button handling
        logoutButton.setOnClickListener {
            logoutUser()
        }

        // Setup the action bar back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // Handle back button in action bar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Handle device back button press
    override fun onBackPressed() {
        // Simply finish this activity and return to the previous activity (Dashboard)
        // The login state is maintained in SharedPreferences
        finish()
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
        // Clear user session data but keep theme preferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save the theme preference
        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)

        // Clear user authentication data
        editor.remove("username")
        editor.putBoolean("is_logged_in", false)
        editor.apply()

        // Restore theme preference after clearing
        editor.putBoolean("dark_theme", isDarkTheme)
        editor.apply()

        // Navigate to login activity
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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