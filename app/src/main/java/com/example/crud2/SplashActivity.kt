package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Fade-in animation for the logo
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000 // Fade in over 1 second
            startOffset = 0 // Start immediately
        }

        // Scale-up animation for the logo
        val scaleUp = ScaleAnimation(
            0.85f, 1.0f,  // Scale from 85% to 100% in X direction
            0.85f, 1.0f,  // Scale from 85% to 100% in Y direction
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000 // Scale up over 1 second
            startOffset = 500 // Start slightly after fade-in to create a smooth transition
        }

        // Start fade-in and scale-up animations on the logo
        logoImageView.startAnimation(fadeIn)
        logoImageView.startAnimation(scaleUp)

        // Fade-in the progress bar
        progressBar.visibility = ProgressBar.VISIBLE
        val progressBarFadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000 // Fade in over 1 second
            startOffset = 1000 // Start after the logo animations are finished
        }
        progressBar.startAnimation(progressBarFadeIn)

        // Set a 3-second delay before transitioning to the main activity
        Handler().postDelayed({
            // Hide the progress bar after the delay
            progressBar.visibility = ProgressBar.GONE

            // Start MainActivity after 3 seconds
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)

            // Close the SplashActivity so the user cannot go back to it
            finish()
        }, 3000) // 3000 milliseconds = 3 seconds
    }
}
