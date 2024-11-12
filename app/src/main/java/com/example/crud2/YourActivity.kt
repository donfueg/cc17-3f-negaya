package com.example.crud2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View

class YourActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_activity)

        // Ensure the view with ID R.id.main exists in your layout
        val mainView = findViewById<View>(R.id.main)

        // Set the OnApplyWindowInsetsListener
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            // Handle the insets here
            // For example, you can consume the system window insets
            val consumedInsets = insets.consumeSystemWindowInsets()

            // Update the view's padding based on the consumed insets
            v.setPadding(
                v.paddingLeft + consumedInsets.getSystemWindowInsetLeft(),
                v.paddingTop + consumedInsets.getSystemWindowInsetTop(),
                v.paddingRight + consumedInsets.getSystemWindowInsetRight(),
                v.paddingBottom + consumedInsets.getSystemWindowInsetBottom()
            )

            // Return the consumed insets
            consumedInsets
        }
    }
}