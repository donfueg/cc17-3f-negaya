package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView

class Dashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var usernameTextView: TextView
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize the TextView for displaying username
        usernameTextView = findViewById(R.id.textView5)

        // Retrieve the username passed from Login activity
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "Guest"

        // Save the username in SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("username", username).apply()

        // Set the username in the TextView
        usernameTextView.text = "Hello $username"

        // Handling Edge-to-Edge for padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up buttons for navigation
        findViewById<Button>(R.id.contact).setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        findViewById<Button>(R.id.button6).setOnClickListener {
            startActivity(Intent(this, BraceletActivity::class.java))
        }

        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        // Set up Bottom Navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.nav_emergency -> {
                    startActivity(Intent(this, EmergencyActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // This is called when the map is ready to be used
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Get the current location of the user
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                // Display the location on the map
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location tracking
                mMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }
}
