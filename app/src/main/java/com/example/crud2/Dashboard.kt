package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Dashboard : AppCompatActivity() {

    private lateinit var usernameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // Initialize the TextView for displaying username
        usernameTextView = findViewById(R.id.textView5)

        // Retrieve the username passed from Login activity
        val username = intent.getStringExtra("EXTRA_USERNAME")

        // Set the username in the TextView (e.g., "Hello username")
        if (username != null) {
            usernameTextView.text = "Hello $username"
        } else {
            usernameTextView.text = "Hello Guest"
        }

        // Handling Edge-to-Edge for padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the button to navigate to ContactActivity
        val contactButton = findViewById<Button>(R.id.contact)
        contactButton.setOnClickListener {
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }

        // Set up the button to navigate to BraceletActivity
        val braceletButton = findViewById<Button>(R.id.button6)
        braceletButton.setOnClickListener {
            val intent = Intent(this, BraceletActivity::class.java)
            startActivity(intent)
        }

        // Set up the button to navigate to EmergencyActivity
        val emergencyButton = findViewById<Button>(R.id.emergency)
        emergencyButton.setOnClickListener {
            val intent = Intent(this, EmergencyActivity::class.java)
            startActivity(intent)
        }

        // Set up Bottom Navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate to Dashboard
                    val intent = Intent(this, Dashboard::class.java)
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.nav_emergency -> {
                    // Navigate to Emergency Activity
                    val intent = Intent(this, EmergencyActivity::class.java)
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.nav_settings -> {
                    // Navigate to Settings Activity and pass the username
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.putExtra("EXTRA_USERNAME", username) // Pass the username to SettingsActivity
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }
        }
    }
}
