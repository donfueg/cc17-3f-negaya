package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import kotlin.random.Random

class Dashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private var mainUserMarker: Marker? = null

    private lateinit var usernameTextView: TextView
    private lateinit var dbHelper: FirebaseHelper
    private lateinit var locationRef: DatabaseReference
    private lateinit var heartRateRef: DatabaseReference
    private lateinit var userContactsRef: DatabaseReference
    private var currentUsername: String = ""

    private val addedUserMarkers = mutableMapOf<String, Marker>()

    private var mainUserHeartRate: Int? = null          // live heart rate (latest)
    private var mainUserLastUpdatedHeartRate: Int? = null  // last updated heart rate (previous)

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
                setupRealtimeListeners()
            }
        }
    }

    private fun getCurrentUserId(): String = currentUsername

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

        findViewById<Button>(R.id.emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        findViewById<Button>(R.id.button7).setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }

        findViewById<Button>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.addUserButton).setOnClickListener {
            showUserBottomSheet()
        }
    }

    private fun setupRealtimeListeners() {
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)
                if (lat != null && lng != null && ::mMap.isInitialized) {
                    val newLocation = LatLng(lat, lng)
                    runOnUiThread {
                        mainUserMarker?.remove()
                        mainUserMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(newLocation)
                                .title("You (Main User)")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 15f))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to get location updates: ${error.message}")
            }
        })

        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.getValue(Int::class.java)
                if (hr != null) {
                    // Before updating live HR, store current live HR as last updated HR
                    mainUserLastUpdatedHeartRate = mainUserHeartRate
                    mainUserHeartRate = hr

                    runOnUiThread {
                        usernameTextView.text = "Hello $currentUsername - HR: $hr bpm"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to get heart rate updates: ${error.message}")
            }
        })

        userContactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                runOnUiThread {
                    val currentKeys = snapshot.children.mapNotNull { it.key }.toSet()
                    val keysToRemove = addedUserMarkers.keys - currentKeys
                    for (key in keysToRemove) {
                        addedUserMarkers[key]?.remove()
                        addedUserMarkers.remove(key)
                    }

                    for (contactSnap in snapshot.children) {
                        val key = contactSnap.key ?: continue
                        val name = contactSnap.child("name").getValue(String::class.java) ?: continue
                        val lat = contactSnap.child("latitude").getValue(Double::class.java) ?: continue
                        val lng = contactSnap.child("longitude").getValue(Double::class.java) ?: continue

                        val pos = LatLng(lat, lng)

                        val existingMarker = addedUserMarkers[key]
                        if (existingMarker == null) {
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(pos)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            )
                            if (marker != null) {
                                addedUserMarkers[key] = marker
                            }
                        } else {
                            if (existingMarker.position != pos) {
                                existingMarker.position = pos
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to get contacts updates: ${error.message}")
            }
        })
    }

    private fun showUserBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_user, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        val nameInput = view.findViewById<EditText>(R.id.inputName)
        val mobileInput = view.findViewById<EditText>(R.id.inputMobile)
        val addBtn = view.findViewById<Button>(R.id.bottomSheetAddUserButton)
        val bottomUserListContainer = view.findViewById<LinearLayout>(R.id.bottomUserListContainer)
        val mainUserInfoTextView = view.findViewById<TextView>(R.id.mainUserInfoTextView)

        fun updateMainUserInfoText() {
            val liveHRText = mainUserHeartRate?.let { "$it bpm" } ?: "Loading..."
            val lastHRText = mainUserLastUpdatedHeartRate?.let { "$it bpm" } ?: "N/A"
            mainUserInfoTextView.text =
                "You: $currentUsername - Live HR: $liveHRText - Last Updated HR: $lastHRText"
        }

        updateMainUserInfoText()

        mainUserInfoTextView.setOnClickListener {
            val pos = mainUserMarker?.position
            if (pos != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            } else {
                showToast("Main user location not available yet.")
            }
        }

        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.getValue(Int::class.java)
                if (hr != null) {
                    // Update live and last updated HR here as well to keep in sync
                    mainUserLastUpdatedHeartRate = mainUserHeartRate
                    mainUserHeartRate = hr

                    runOnUiThread {
                        updateMainUserInfoText()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        fun populateUserList() {
            bottomUserListContainer.removeAllViews()
            userContactsRef.get().addOnSuccessListener { snapshot ->
                for (contactSnap in snapshot.children) {
                    val name = contactSnap.child("name").getValue(String::class.java)
                    val lastHeartRate = contactSnap.child("heartrate").getValue(Int::class.java) ?: 0
                    val lat = contactSnap.child("latitude").getValue(Double::class.java)
                    val lng = contactSnap.child("longitude").getValue(Double::class.java)

                    if (name != null && lat != null && lng != null) {
                        val rowLayout = LinearLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(8, 16, 8, 16)
                            gravity = Gravity.CENTER_VERTICAL
                        }

                        val nameView = TextView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3f)
                            text = name
                            textSize = 16f
                            setTextColor(resources.getColor(android.R.color.black))
                        }

                        val liveHRView = TextView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
                            text = "Loading..." // Update if you have live per-user HR data
                            textSize = 16f
                            setTextColor(resources.getColor(android.R.color.holo_green_dark))
                            gravity = Gravity.CENTER
                        }

                        val lastHRView = TextView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
                            text = "$lastHeartRate bpm"
                            textSize = 16f
                            setTextColor(resources.getColor(android.R.color.holo_red_dark))
                            gravity = Gravity.END
                        }

                        rowLayout.addView(nameView)
                        rowLayout.addView(liveHRView)
                        rowLayout.addView(lastHRView)

                        rowLayout.setOnClickListener {
                            val target = LatLng(lat, lng)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))

                            for (i in 0 until bottomUserListContainer.childCount) {
                                bottomUserListContainer.getChildAt(i).setBackgroundColor(resources.getColor(android.R.color.white))
                            }
                            rowLayout.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                        }

                        bottomUserListContainer.addView(rowLayout)

                        // TODO: Add per-user live HR listener here if available
                    }
                }
            }
        }

        populateUserList()

        addBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val mobile = mobileInput.text.toString().trim()

            if (name.isEmpty()) {
                showToast("Please enter contact name.")
                return@setOnClickListener
            }

            if (!isValidPhilippineMobileNumber(mobile)) {
                showToast("Invalid mobile number.")
                return@setOnClickListener
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
                .addOnSuccessListener {
                    showToast("Contact added successfully.")
                    nameInput.text.clear()
                    mobileInput.text.clear()
                    populateUserList()
                }
                .addOnFailureListener {
                    showToast("Failed to add contact.")
                }
        }

        dialog.show()
    }

    private fun isValidPhilippineMobileNumber(mobile: String): Boolean {
        val cleanMobile = mobile.replace(Regex("[^0-9]"), "")
        return cleanMobile.length == 11 && cleanMobile.startsWith("09")
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
        startActivity(
            Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }
}
