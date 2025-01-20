package com.example.crud2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var showPoliceButton: Button

    // Firebase Realtime Database reference
    private lateinit var database: DatabaseReference

    // Firebase location data
    private var firebaseLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map2)

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().getReference("location") // Reference to the location node in Firebase

        // Initialize the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Button to show nearest police station
        showPoliceButton = findViewById(R.id.showPoliceButton)
        showPoliceButton.setOnClickListener {
            firebaseLocation?.let { location ->
                // Call function to find the nearest police station
                findNearestPoliceStation(location.latitude, location.longitude)
            } ?: run {
                Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // This will be called when the map is ready
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable My Location layer
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        // Fetch the real-time location from Firebase
        fetchLocationFromFirebase()
    }

    // Function to fetch location from Firebase
    private fun fetchLocationFromFirebase() {
        // Listen for real-time updates in Firebase location node
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java)
                val longitude = snapshot.child("longitude").getValue(Double::class.java)

                if (latitude != null && longitude != null) {
                    firebaseLocation = LatLng(latitude, longitude)

                    // Clear previous markers
                    googleMap.clear()

                    // Add a marker for the Firebase location
                    firebaseLocation?.let {
                        googleMap.addMarker(MarkerOptions().position(it).title("Firebase Location"))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    }
                } else {
                    Toast.makeText(this@Map, "Location data not available", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapActivity", "Failed to read location data: ${error.message}")
                Toast.makeText(this@Map, "Error reading Firebase data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to find the nearest police station (optional, as per your previous implementation)
    private fun findNearestPoliceStation(latitude: Double, longitude: Double) {
        // Your previous logic for finding the nearest police station can be used here
        // For simplicity, this function can be modified based on your requirements
        Toast.makeText(this, "Finding nearest police station...", Toast.LENGTH_SHORT).show()
    }
}