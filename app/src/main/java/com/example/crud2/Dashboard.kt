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
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "Guest"

        // Save the username in SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("username", username).apply()

        // Set the username in the TextView
        usernameTextView.text = "Hello $username"

        // Handling Edge-to-Edge for padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the button to navigate to ContactActivity
        findViewById<Button>(R.id.contact).setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        // Set up the button to navigate to BraceletActivity
        findViewById<Button>(R.id.button6).setOnClickListener {
            startActivity(Intent(this, BraceletActivity::class.java))
        }

        // Set up the button to navigate to EmergencyActivity
        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        // Set up Bottom Navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.nav_emergency -> {
                    startActivity(Intent(this, EmergencyActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
