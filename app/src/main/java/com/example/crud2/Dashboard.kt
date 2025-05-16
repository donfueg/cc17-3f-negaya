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
    private lateinit var heartRateTextView: TextView
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var locationRef: DatabaseReference
    private lateinit var heartRateRef: DatabaseReference
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Firebase references
        locationRef = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://ahhh-41e71-default-rtdb.firebaseio.com/location")
        heartRateRef = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://ahhh-41e71-default-rtdb.firebaseio.com/heartrate")

        // UI references
        usernameTextView = findViewById(R.id.textView5)
        heartRateTextView = findViewById(R.id.heartRateTextView)
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "Guest"
        getSharedPreferences("user_prefs", MODE_PRIVATE)
            .edit().putString("username", username).apply()
        usernameTextView.text = "Hello $username"

        // Window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Buttons
        findViewById<Button>(R.id.contact).setOnClickListener {
            val user = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "Guest")
            val intent = Intent(this, ContactActivity::class.java)
            intent.putExtra("EXTRA_USERNAME", user)
            startActivity(intent)
        }

        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        findViewById<Button>(R.id.button7).setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }

        findViewById<Button>(R.id.settings).setOnClickListener {
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        // Realtime database listeners
        fetchLocationsFromDatabase()
        fetchHeartRate()
    }

    private fun fetchHeartRate() {
        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val heartRate = snapshot.getValue(Int::class.java)
                heartRate?.let {
                    heartRateTextView.text = "Heart Rate: $it bpm"
                } ?: run {
                    heartRateTextView.text = "Heart Rate: --"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                heartRateTextView.text = "Heart Rate: --"
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
                currentMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun fetchLocationsFromDatabase() {
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java)
                val longitude = snapshot.child("longitude").getValue(Double::class.java)

                if (latitude != null && longitude != null) {
                    val location = LatLng(latitude, longitude)
                    currentMarker?.remove()
                    currentMarker = mMap.addMarker(MarkerOptions().position(location).title("Location from Firebase"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error reading Firebase data: ${error.message}")
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }
}
