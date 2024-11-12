package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val registerButton = findViewById<Button>(R.id.button3)
        registerButton.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        val loginButton = findViewById<Button>(R.id.button4)
        loginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }
}
