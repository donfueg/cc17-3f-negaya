package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.ProgressBar
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Fade-in animation
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000 // 1 second fade-in animation

        // Scale-up animation
        val scaleUp = ScaleAnimation(
            0.5f, 1.0f,  // Scale from 50% to 100% in X direction
            0.5f, 1.0f,  // Scale from 50% to 100% in Y direction
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleUp.duration = 1000 // 1 second scale-up animation

        // Start both animations on the logo
        logoImageView.startAnimation(fadeIn)
        logoImageView.startAnimation(scaleUp)

        // Show the progress bar during animation
        progressBar.visibility = ProgressBar.VISIBLE

        // Set a 3-second delay for splash screen
        Handler().postDelayed({
            // After the delay, hide the progress bar
            progressBar.visibility = ProgressBar.GONE

            // Start MainActivity after 3 seconds
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)

            // Close the SplashActivity so the user cannot go back to it
            finish()
        }, 3000) // 3000 milliseconds = 3 seconds
    }
}
