package com.example.crud2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.firebase.database.*

class Dashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var usernameTextView: TextView
    private lateinit var heartRateTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private var currentMarker: Marker? = null
    private lateinit var dbHelper: FirebaseHelper

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var locationRef: DatabaseReference
    private lateinit var heartRateRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase database references
        locationRef = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://ahhh-41e71-default-rtdb.firebaseio.com/location")
        heartRateRef = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://ahhh-41e71-default-rtdb.firebaseio.com/heartrate")

        // Initialize FirebaseHelper
        dbHelper = FirebaseHelper(this)

        // UI elements
        usernameTextView = findViewById(R.id.textView5)
        heartRateTextView = findViewById(R.id.heartRateTextView)

        // Get username from intent or shared prefs
        val username = intent.getStringExtra("EXTRA_USERNAME")
            ?: getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)

        // If username is null or empty, redirect to login
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Session error. Please login again", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        // Check if user is logged in via shared preferences
        val isLoggedIn = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            Toast.makeText(this, "Session expired. Please login again", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        // Verify user exists before proceeding
        dbHelper.checkUserExists(username) { userExists ->
            runOnUiThread {
                if (!userExists) {
                    // Clear invalid user data and redirect to login
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(this, "User session expired. Please login again", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                    return@runOnUiThread
                }

                // Setup UI with valid username
                usernameTextView.text = "Hello $username"

                // Save username in shared preferences for later use
                getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                    .putString("username", username)
                    .putBoolean("is_logged_in", true)
                    .apply()

                setupDashboard()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupDashboard() {
        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up Google Map fragment and async callback
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Button listeners
        findViewById<Button>(R.id.contact).setOnClickListener {
            val intent = Intent(this, ContactActivity::class.java)
            intent.putExtra("EXTRA_USERNAME", getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null))
            startActivity(intent)
        }

        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        findViewById<Button>(R.id.button7).setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }

        findViewById<Button>(R.id.settings).setOnClickListener {
            // Navigate to settings without clearing preferences
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Listen for heart rate updates
        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val heartRate = snapshot.getValue(Int::class.java)
                heartRateTextView.text = if (heartRate != null) "Heart Rate: $heartRate bpm" else "Heart Rate: --"
            }

            override fun onCancelled(error: DatabaseError) {
                heartRateTextView.text = "Heart Rate: --"
            }
        })

        // Listen for location changes from Firebase DB
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                if (lat != null && lng != null && ::mMap.isInitialized) {
                    val location = LatLng(lat, lng)
                    currentMarker?.remove()
                    currentMarker = mMap.addMarker(MarkerOptions().position(location).title("Tracked Location"))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                currentMarker?.remove()
                currentMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("Your Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }
}