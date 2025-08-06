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
    private var mainUserTextView: TextView? = null

    private lateinit var usernameTextView: TextView
    private lateinit var dbHelper: FirebaseHelper
    private lateinit var locationRef: DatabaseReference
    private lateinit var heartRateRef: DatabaseReference
    private lateinit var userContactsRef: DatabaseReference
    private var currentUsername: String = ""

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        usernameTextView = findViewById(R.id.textView5)

        val username = intent.getStringExtra("EXTRA_USERNAME")
            ?: getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)

        if (username.isNullOrEmpty()) {
            showToast("Session error. Please login again")
            navigateToLogin()
            return
        }

        currentUsername = username

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

        val userId = getCurrentUserId()
        userContactsRef = FirebaseDatabase.getInstance().getReference("user_contacts").child(userId)

        dbHelper.checkUserExists(username) { exists ->
            runOnUiThread {
                if (!exists) {
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                    showToast("User session expired. Please login again")
                    navigateToLogin()
                    return@runOnUiThread
                }

                usernameTextView.text = "Hello $username"
                setupDashboard()
            }
        }
    }

    private fun getCurrentUserId(): String {
        return currentUsername
    }

    private fun setupDashboard() {
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
                putExtra("EXTRA_USERNAME", currentUsername)
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

        val userListContainer = findViewById<LinearLayout>(R.id.userListContainer)

        // Setup real-time main user updates
        setupMainUserRealTimeUpdates(userListContainer)

        // Setup contacts listener
        userContactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!::mMap.isInitialized) return

                // Clear only contact items and markers, preserve main user
                clearContactsOnly(userListContainer)

                // Add all contacts
                for (contactSnap in snapshot.children) {
                    val name = contactSnap.child("name").getValue(String::class.java)
                    val contactLat = contactSnap.child("latitude").getValue(Double::class.java)
                    val contactLng = contactSnap.child("longitude").getValue(Double::class.java)
                    val heartRate = contactSnap.child("heartrate").getValue(Int::class.java) ?: Random.nextInt(60, 100)

                    if (name != null && contactLat != null && contactLng != null) {
                        val userLatLng = LatLng(contactLat, contactLng)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(userLatLng)
                                .title(name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )

                        val userItem = TextView(this@Dashboard).apply {
                            text = "$name - HR: $heartRate bpm"
                            textSize = 16f
                            setPadding(16, 8, 16, 8)
                            setTextColor(resources.getColor(android.R.color.black))
                            setBackgroundColor(resources.getColor(android.R.color.white))
                            tag = "contact"
                            setOnClickListener {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                            }
                        }
                        userListContainer.addView(userItem)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load contacts: ${error.message}")
            }
        })
    }

    private fun setupMainUserRealTimeUpdates(userListContainer: LinearLayout) {
        var currentHeartRate: Int = 83 // Current value from your database
        var currentLatLng: LatLng? = null

        // Listen for heart rate changes in real-time
        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                val heartRate = data.getValue(Int::class.java)
                if (heartRate != null) {
                    currentHeartRate = heartRate
                    runOnUiThread {
                        updateMainUserDisplay(userListContainer, currentHeartRate, currentLatLng)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Heart rate error: ${error.message}")
            }
        })

        // Listen for location changes in real-time
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                if (!::mMap.isInitialized) return

                val lat = data.child("latitude").getValue(Double::class.java)
                val lng = data.child("longitude").getValue(Double::class.java)

                if (lat != null && lng != null) {
                    currentLatLng = LatLng(lat, lng)

                    runOnUiThread {
                        // Update map marker
                        currentMarker?.remove()
                        currentMarker = mMap.addMarker(
                            MarkerOptions().position(currentLatLng!!).title("$currentUsername")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )

                        // Update list display
                        updateMainUserDisplay(userListContainer, currentHeartRate, currentLatLng)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Location error: ${error.message}")
            }
        })
    }

    private fun updateMainUserDisplay(userListContainer: LinearLayout, heartRate: Int, latLng: LatLng?) {
        if (latLng == null) return

        // Create main user item if it doesn't exist
        if (mainUserTextView == null) {
            mainUserTextView = TextView(this@Dashboard).apply {
                textSize = 16f
                setPadding(16, 8, 16, 8)
                setTextColor(resources.getColor(android.R.color.black))
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                tag = "mainuser"
            }
            userListContainer.addView(mainUserTextView, 0)
        }

        // Update the text and click listener
        mainUserTextView?.apply {
            text = "$currentUsername (You) - HR: $heartRate bpm"
            setOnClickListener {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    private fun clearContactsOnly(userListContainer: LinearLayout) {
        // Clear map completely and re-add main user marker if it exists
        mMap.clear()
        currentMarker?.let {
            val position = it.position
            val title = it.title
            currentMarker = mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // Remove only contact items from list
        val contactItems = mutableListOf<TextView>()
        for (i in 0 until userListContainer.childCount) {
            val child = userListContainer.getChildAt(i)
            if (child.tag == "contact") {
                contactItems.add(child as TextView)
            }
        }
        contactItems.forEach { userListContainer.removeView(it) }
    }

    private fun showAddUserDialog() {
        val nameInput = EditText(this).apply { hint = "Enter Contact Name" }
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
            .setTitle("Add New Contact")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val mobile = mobileInput.text.toString().trim()

                // Validate input fields
                if (name.isEmpty()) {
                    showToast("Please enter contact name.")
                    return@setPositiveButton
                }

                if (mobile.isEmpty()) {
                    showToast("Please enter mobile number.")
                    return@setPositiveButton
                }

                // Validate mobile number format
                if (!isValidPhilippineMobileNumber(mobile)) {
                    showToast("Invalid mobile number. Must be 11 digits starting with '09'.")
                    return@setPositiveButton
                }

                val randomLocation = getRandomBaguioLocation()
                val heartRate = Random.nextInt(60, 100)

                val newContactRef = userContactsRef.push()
                val contactData = mapOf(
                    "name" to name,
                    "mobile" to mobile,
                    "latitude" to randomLocation.latitude,
                    "longitude" to randomLocation.longitude,
                    "heartrate" to heartRate,
                    "addedBy" to currentUsername,
                    "addedAt" to System.currentTimeMillis()
                )

                newContactRef.setValue(contactData)
                    .addOnSuccessListener { showToast("Contact added successfully.") }
                    .addOnFailureListener { showToast("Failed to add contact: ${it.message}") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getRandomBaguioLocation(): LatLng {
        val randomLat = Random.nextDouble(16.385, 16.420)
        val randomLng = Random.nextDouble(120.580, 120.620)
        return LatLng(randomLat, randomLng)
    }

    private fun isValidPhilippineMobileNumber(mobile: String): Boolean {
        // Remove any spaces or special characters
        val cleanMobile = mobile.replace(Regex("[^0-9]"), "")

        // Check if it's exactly 11 digits and starts with "09"
        return cleanMobile.length == 11 && cleanMobile.startsWith("09")
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }
}