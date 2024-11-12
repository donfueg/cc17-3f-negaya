package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NumberPage : AppCompatActivity() {
    private lateinit var nextButton: Button
    private lateinit var numberEditText: EditText
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_page)

        db = DatabaseHelper(this)

        nextButton = findViewById(R.id.next)
        numberEditText = findViewById(R.id.numberTextBox)

        // Retrieve the previously stored number (if any)
        val username = intent.getStringExtra("username")
        if (username != null) {
            val storedNumber = db.getUserNumber(username)
            if (storedNumber != null) {
                numberEditText.setText(storedNumber)
            }
        }

        nextButton.setOnClickListener {
            val enteredNumber = numberEditText.text.toString().trim()

            // Validate the entered number
            if (enteredNumber.isNotEmpty() && enteredNumber.matches(Regex("[0-9]+"))) {
                // Store the number in the database
                if (username != null) {
                    db.updateUserNumber(username, enteredNumber)
                    Toast.makeText(this, "Number updated successfully", Toast.LENGTH_SHORT).show()
                }

                // Navigate to the next activity
                val intent = Intent(this, Verification::class.java)
                intent.putExtra("number", enteredNumber) // Pass the entered number
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
    }
}