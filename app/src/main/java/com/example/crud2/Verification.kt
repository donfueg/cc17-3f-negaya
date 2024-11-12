package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Verification : AppCompatActivity() {
    private lateinit var verifyButton: Button
    private lateinit var otpDigit1: EditText
    private lateinit var otpDigit2: EditText
    private lateinit var otpDigit3: EditText
    private lateinit var otpDigit4: EditText
    private lateinit var otpDigit5: EditText
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        dbHelper = DatabaseHelper(this)

        verifyButton = findViewById(R.id.verifyButton)
        otpDigit1 = findViewById(R.id.otpDigit1)
        otpDigit2 = findViewById(R.id.otpDigit2)
        otpDigit3 = findViewById(R.id.otpDigit3)
        otpDigit4 = findViewById(R.id.otpDigit4)
        otpDigit5 = findViewById(R.id.otpDigit5)

        verifyButton.setOnClickListener {
            val enteredOtp = "$otpDigit1$otpDigit2$otpDigit3$otpDigit4$otpDigit5".trim()

            // Perform verification logic here (e.g., check if the OTP matches the expected value)
            if (enteredOtp.isNotEmpty()) {
                // For demonstration, we'll just check if the OTP is correct
                if (enteredOtp == "12345") {
                    Toast.makeText(this, "Verification successful", Toast.LENGTH_SHORT).show()
                    // Proceed to the Login activity
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}