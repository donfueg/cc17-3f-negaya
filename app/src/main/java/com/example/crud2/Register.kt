package com.example.crud2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Register : AppCompatActivity() {
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var contactEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        registerButton = findViewById(R.id.button)
        emailEditText = findViewById(R.id.email)
        contactEditText = findViewById(R.id.contact)
        passwordEditText = findViewById(R.id.password)
        loginTextView = findViewById(R.id.login)

        registerButton.setOnClickListener {
            // Handle registration logic here
        }

        loginTextView.setOnClickListener {
            // Navigate to login activity
        }
    }
}
