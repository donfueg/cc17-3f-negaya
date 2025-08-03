package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import kotlin.random.Random

class Dashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private var currentMarker: Marker? = null

    private lateinit var usernameTextView: TextView
    private lateinit var heartRateTextView: TextView

    private lateinit var dbHelper: FirebaseHelper
    private lateinit var locationRef: DatabaseReference
    private lateinit var heartRateRef: DatabaseReference

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        usernameTextView = findViewById(R.id.textView5)
        heartRateTextView = findViewById(R.id.heartRateTextView)

        val username = intent.getStringExtra("EXTRA_USERNAME")
            ?: getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)

        if (username.isNullOrEmpty()) {
            showToast("Session error. Please login again")
            navigateToLogin()
            return
        }

        val isLoggedIn = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            showToast("Session expired. Please login again")
            navigateToLogin()
            return
        }

        dbHelper = FirebaseHelper(this)
        locationRef = FirebaseDatabase.getInstance().getReference("location")
        heartRateRef = FirebaseDatabase.getInstance().getReference("heartrate")

        dbHelper.checkUserExists(username) { exists ->
            runOnUiThread {
                if (!exists) {
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                    showToast("User session expired. Please login again")
                    navigateToLogin()
                    return@runOnUiThread
                }

                usernameTextView.text = "Hello $username"
                setupDashboard(username)
            }
        }
    }

    private fun setupDashboard(username: String) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.contact).setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java).apply {
                putExtra("EXTRA_USERNAME", username)
            })
        }

        findViewById<Button>(R.id.addUserButton).setOnClickListener {
            showAddUserDialog()
        }

        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        findViewById<Button>(R.id.button7).setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }

        findViewById<Button>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val heartRate = snapshot.getValue(Int::class.java)
                heartRateTextView.text = "Heart Rate: ${heartRate ?: "--"} bpm"
            }

            override fun onCancelled(error: DatabaseError) {
                heartRateTextView.text = "Heart Rate: --"
            }
        })

        // Real-time marker for general location
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)

                if (lat != null && lng != null && ::mMap.isInitialized) {
                    val location = LatLng(lat, lng)
                    currentMarker?.remove()
                    currentMarker = mMap.addMarker(
                        MarkerOptions().position(location).title("Real-Time Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to fetch location: ${error.message}")
            }
        })

        // Listen for added users under location/users
        locationRef.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (::mMap.isInitialized) {
                    mMap.clear()

                    // Add the real-time red marker again
                    locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(data: DataSnapshot) {
                            val lat = data.child("latitude").getValue(Double::class.java)
                            val lng = data.child("longitude").getValue(Double::class.java)
                            if (lat != null && lng != null) {
                                val loc = LatLng(lat, lng)
                                mMap.addMarker(
                                    MarkerOptions().position(loc).title("Real-Time Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                )
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

                    for (userSnap in snapshot.children) {
                        val name = userSnap.child("name").getValue(String::class.java)
                        val lat = userSnap.child("latitude").getValue(Double::class.java)
                        val lng = userSnap.child("longitude").getValue(Double::class.java)

                        if (name != null && lat != null && lng != null) {
                            val userLatLng = LatLng(lat, lng)
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(userLatLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            )
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load user markers: ${error.message}")
            }
        })
    }

    private fun showAddUserDialog() {
        val nameInput = EditText(this).apply {
            hint = "Enter Name"
        }

        val mobileInput = EditText(this).apply {
            hint = "Enter Mobile Number"
            inputType = InputType.TYPE_CLASS_PHONE
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(nameInput)
            addView(mobileInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Add New User")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val mobile = mobileInput.text.toString().trim()

                if (name.isEmpty() || mobile.isEmpty()) {
                    showToast("Please enter both name and mobile number.")
                    return@setPositiveButton
                }

                val randomLocation = getRandomBaguioLocation()
                val newUserRef = locationRef.child("users").push()
                val userData = mapOf(
                    "name" to name,
                    "mobile" to mobile,
                    "latitude" to randomLocation.latitude,
                    "longitude" to randomLocation.longitude
                )

                newUserRef.setValue(userData)
                    .addOnSuccessListener {
                        showToast("User added successfully.")
                    }
                    .addOnFailureListener {
                        showToast("Failed to add user: ${it.message}")
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getRandomBaguioLocation(): LatLng {
        val randomLat = Random.nextDouble(16.385, 16.420)
        val randomLng = Random.nextDouble(120.580, 120.620)
        return LatLng(randomLat, randomLng)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(
                    MarkerOptions().position(currentLatLng).title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }
}
