package com.example.crud2

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the logo ImageView
        val logoImageView = findViewById<ImageView>(R.id.imageView4)

        // Create a rotation animation for the logo (continuous rotation)
        val rotate = ObjectAnimator.ofFloat(logoImageView, "rotation", 0f, 360f)
        rotate.duration = 2000 // Rotation completes every 2 seconds
        rotate.repeatCount = ObjectAnimator.INFINITE // Repeat infinitely
        rotate.start()

        // Set click listeners for the buttons
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
