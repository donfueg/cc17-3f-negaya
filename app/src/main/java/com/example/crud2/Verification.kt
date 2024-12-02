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
            // Concatenate the OTP digits entered by the user
            val enteredOtp = otpDigit1.text.toString() + otpDigit2.text.toString() +
                    otpDigit3.text.toString() + otpDigit4.text.toString() + otpDigit5.text.toString()

            // Perform verification logic here (check if OTP matches the default "54321")
            if (enteredOtp.isNotEmpty()) {
                if (enteredOtp == "54321") {
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
