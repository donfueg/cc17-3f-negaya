package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NumberPage : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var userNumberTextView: TextView
    private lateinit var nextButton: Button
    private lateinit var numberTextBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_page)

        // Initialize the views
        usernameTextView = findViewById(R.id.usernameTextView)
        userNumberTextView = findViewById(R.id.userNumberTextView)
        nextButton = findViewById(R.id.next)
        numberTextBox = findViewById(R.id.numberTextBox)

        // Retrieve the username and phone number from the intent
        val username = intent.getStringExtra("EXTRA_USERNAME")
        val phone = intent.getStringExtra("EXTRA_PHONE")

        // Set the values to the TextViews
        usernameTextView.text = username ?: "Username not available"
        userNumberTextView.text = phone ?: "Phone number not available"

        // Handle "Next" button click
        nextButton.setOnClickListener {
            val enteredPhone = numberTextBox.text.toString()

            // Validate the phone number entered
            if (enteredPhone.isNotEmpty()) {
                // Send SMS with the OTP
                sendOtp(enteredPhone)

                // Proceed to the verification page
                val intent = Intent(this, Verification::class.java).apply {
                    putExtra("EXTRA_PHONE", enteredPhone)
                }
                startActivity(intent)
            } else {
                // Show a toast if the number is empty
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to send OTP via SMS
    private fun sendOtp(phoneNumber: String) {
        val otp = "54321" // Example OTP, you can generate a random one
        val message = "Your OTP code is: $otp"

        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Send the SMS
                SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }
    }

    // Handle runtime permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending SMS
                val phoneNumber = numberTextBox.text.toString()
                sendOtp(phoneNumber)
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "SMS permission is required to send OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
