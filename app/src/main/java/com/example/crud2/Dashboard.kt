package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

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
    }
}

