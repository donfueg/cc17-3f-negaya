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

        // Retrieve the username passed from DashboardActivity
        val username = intent.getStringExtra("EXTRA_USERNAME")

        if (username != null && username.isNotEmpty()) {
            profileNameTextView.text = "Username: $username"  // Display the username dynamically
        } else {
            profileNameTextView.text = "Username: Guest"  // Fallback
        }

        // Set default theme based on saved preferences (if any)
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            darkThemeRadioButton.isChecked = true
        } else {
            lightThemeRadioButton.isChecked = true
        }

        // Theme selection handling
        lightThemeRadioButton.setOnClickListener {
            setTheme(R.style.LightTheme)  // Assuming LightTheme is defined in styles.xml
            saveThemePreference(false)
        }

        darkThemeRadioButton.setOnClickListener {
            setTheme(R.style.DarkTheme)  // Assuming DarkTheme is defined in styles.xml
            saveThemePreference(true)
        }

        // Notifications toggle handling
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Enable notifications logic
            } else {
                // Disable notifications logic
            }
        }

        // Log out button handling
        logoutButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()  // Finish SettingsActivity to go back to LoginActivity
        }
    }

    // Save theme preference in SharedPreferences
    private fun saveThemePreference(isDarkTheme: Boolean) {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("dark_theme", isDarkTheme)
        editor.apply()
    }
}
