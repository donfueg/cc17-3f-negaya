package com.example.registrationpage

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val registerButton = findViewById<Button>(R.id.button3)

        registerButton.setOnClickListener {

            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
        val loginButton = findViewById<Button>(R.id.button4)
        loginButton.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }

        //we will add the textview button here

    }
}
