package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class Dashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var usernameTextView: TextView
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var database: DatabaseReference
    private var currentMarker: Marker? = null // Single marker reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://ahhh-41e71-default-rtdb.firebaseio.com/location")

        // Set up UI and map fragment
        usernameTextView = findViewById(R.id.textView5)
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "Guest"
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("username", username).apply()
        usernameTextView.text = "Hello $username"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Navigation button: Contact
        findViewById<Button>(R.id.contact).setOnClickListener {
            val username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "Guest")
            val intent = Intent(this, ContactActivity::class.java)
            intent.putExtra("EXTRA_USERNAME", username)
            startActivity(intent)
        }

        // Navigation button: Emergency
        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        // Navigation button: Map (if this points to a different Map screen)
        findViewById<Button>(R.id.button7).setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }
    }

    private fun navigateToContacts(username: String) {
        val intent = Intent(this, Dashboard::class.java).apply {
            putExtra("EXTRA_USERNAME", username)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
            fetchLocationsFromDatabase()
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

                // Remove previous marker if it exists
                currentMarker?.remove()

                // Add a new marker
                currentMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))

                // Move camera to the current location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun fetchLocationsFromDatabase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java)
                val longitude = snapshot.child("longitude").getValue(Double::class.java)

                if (latitude != null && longitude != null) {
                    val location = LatLng(latitude, longitude)

                    // Remove previous marker if it exists
                    currentMarker?.remove()

                    // Add a new marker
                    currentMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("Location from Firebase")
                    )

                    // Move camera to the new location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                println("Error reading Firebase data: ${error.message}")
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.isMyLocationEnabled = true
                    getCurrentLocation()
                    fetchLocationsFromDatabase()
                }
            }
        }
    }
}
