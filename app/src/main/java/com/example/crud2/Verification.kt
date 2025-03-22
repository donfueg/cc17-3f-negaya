package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthOptions
import java.util.concurrent.TimeUnit

class Verification : AppCompatActivity() {
    private lateinit var verifyButton: Button
    private lateinit var resendOtpText: TextView
    private lateinit var otpDigit1: EditText
    private lateinit var otpDigit2: EditText
    private lateinit var otpDigit3: EditText
    private lateinit var otpDigit4: EditText
    private lateinit var otpDigit5: EditText
    private lateinit var otpDigit6: EditText

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var verificationId: String? = null
    private var phoneNumber: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get data from intent
        verificationId = intent.getStringExtra("EXTRA_VERIFICATION_ID")
        phoneNumber = intent.getStringExtra("EXTRA_PHONE")
        userId = intent.getStringExtra("EXTRA_USER_ID")

        // Initialize views
        verifyButton = findViewById(R.id.verifyButton)
        resendOtpText = findViewById(R.id.resendOtpText)
        otpDigit1 = findViewById(R.id.otpDigit1)
        otpDigit2 = findViewById(R.id.otpDigit2)
        otpDigit3 = findViewById(R.id.otpDigit3)
        otpDigit4 = findViewById(R.id.otpDigit4)
        otpDigit5 = findViewById(R.id.otpDigit5)
        otpDigit6 = findViewById(R.id.otpDigit6)

        // Setup auto-focus for OTP input
        setupOtpInputs()

        // Verify button click listener
        verifyButton.setOnClickListener {
            val enteredOtp = otpDigit1.text.toString() + otpDigit2.text.toString() +
                    otpDigit3.text.toString() + otpDigit4.text.toString() +
                    otpDigit5.text.toString() + otpDigit6.text.toString()

            if (enteredOtp.length == 6) {
                verifyOtp(enteredOtp)
            } else {
                Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        // Resend OTP click listener
        resendOtpText.setOnClickListener {
            if (phoneNumber != null) {
                resendOtp(phoneNumber!!)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to verify the OTP
    private fun verifyOtp(enteredOtp: String) {
        if (verificationId != null) {
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, enteredOtp)
                signInWithPhoneAuthCredential(credential)
            } catch (e: Exception) {
                Toast.makeText(this, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Verification ID not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to sign in with phone auth credential
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Update user verification status in Firestore
                    updateUserVerificationStatus(true)

                    Toast.makeText(this, "Phone number verified successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to login screen or main activity
                    val intent = Intent(this, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Verification failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to update user verification status in Firestore
    private fun updateUserVerificationStatus(isVerified: Boolean) {
        if (userId != null) {
            firestore.collection("users").document(userId!!)
                .update("isVerified", isVerified)
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update verification status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Function to resend OTP
    private fun resendOtp(phone: String) {
        val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"

        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        signInWithPhoneAuthCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Toast.makeText(this@Verification, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onCodeSent(newVerificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId = newVerificationId
                        Toast.makeText(this@Verification, "OTP resent successfully!", Toast.LENGTH_SHORT).show()
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error resending OTP: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Setup automatic focus change for OTP input fields
    private fun setupOtpInputs() {
        val editTexts = arrayOf(otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6)

        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < editTexts.size - 1) {
                        editTexts[i + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }
}