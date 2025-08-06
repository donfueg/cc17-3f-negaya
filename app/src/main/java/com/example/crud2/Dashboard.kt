package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    // Track markers for added users keyed by their Firebase DB key
    private val addedUserMarkers = mutableMapOf<String, Marker>()

    private var mainUserHeartRate: Int? = null  // To store latest HR for main user

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
                setupRealtimeListeners()  // Start realtime listeners
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

        val openBottomSheet = findViewById<Button>(R.id.addUserButton)
        openBottomSheet.setOnClickListener {
            showUserBottomSheet()
        }
    }

    private fun setupRealtimeListeners() {
        // Main user location listener (red marker)
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

        // Heart rate listener (update usernameTextView and store latest HR)
        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.getValue(Int::class.java)
                if (hr != null) {
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

        // Listen for added user contacts and show them as green markers
        userContactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                runOnUiThread {
                    // Remove markers for users no longer in DB
                    val currentKeys = snapshot.children.mapNotNull { it.key }.toSet()
                    val keysToRemove = addedUserMarkers.keys - currentKeys
                    for (key in keysToRemove) {
                        addedUserMarkers[key]?.remove()
                        addedUserMarkers.remove(key)
                    }

                    // Add or update markers for each contact
                    for (contactSnap in snapshot.children) {
                        val key = contactSnap.key ?: continue
                        val name = contactSnap.child("name").getValue(String::class.java) ?: continue
                        val lat = contactSnap.child("latitude").getValue(Double::class.java) ?: continue
                        val lng = contactSnap.child("longitude").getValue(Double::class.java) ?: continue

                        val pos = LatLng(lat, lng)

                        val existingMarker = addedUserMarkers[key]
                        if (existingMarker == null) {
                            // Add new green marker
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
                            // Update existing marker position if changed
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

        // Clear container before adding views
        bottomUserListContainer.removeAllViews()

        // Create main user TextView and add it at the top
        val mainUserInfoTextView = TextView(this).apply {
            text = "You: $currentUsername - HR: ${mainUserHeartRate ?: "Loading..."} bpm"
            textSize = 16f
            setPadding(16, 8, 16, 8)
            setTextColor(resources.getColor(android.R.color.holo_red_dark))
            setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val pos = mainUserMarker?.position
                if (pos != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                } else {
                    showToast("Main user location not available yet.")
                }
            }
        }
        bottomUserListContainer.addView(mainUserInfoTextView)

        updateUserListInBottomSheet(bottomUserListContainer)

        // Listen to heart rate updates to update mainUserInfoTextView dynamically
        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.getValue(Int::class.java)
                runOnUiThread {
                    mainUserInfoTextView.text = "You: $currentUsername - HR: ${hr ?: "Loading..."} bpm"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

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
                    updateUserListInBottomSheet(bottomUserListContainer)
                }
                .addOnFailureListener {
                    showToast("Failed to add contact.")
                }
        }

        dialog.show()
    }


    private fun updateUserListInBottomSheet(container: LinearLayout) {
        // Remove all but the first child (main user info TextView) to avoid duplicates
        if (container.childCount > 1) {
            container.removeViews(1, container.childCount - 1)
        }

        userContactsRef.get().addOnSuccessListener { snapshot ->
            for (contactSnap in snapshot.children) {
                val name = contactSnap.child("name").getValue(String::class.java)
                val heartRate = contactSnap.child("heartrate").getValue(Int::class.java)
                val lat = contactSnap.child("latitude").getValue(Double::class.java)
                val lng = contactSnap.child("longitude").getValue(Double::class.java)

                if (name != null && heartRate != null && lat != null && lng != null) {
                    val item = TextView(this).apply {
                        text = "$name - HR: $heartRate bpm"
                        textSize = 16f
                        setPadding(16, 8, 16, 8)
                        setTextColor(resources.getColor(android.R.color.black))
                        setBackgroundColor(resources.getColor(android.R.color.white))
                        setOnClickListener {
                            val target = LatLng(lat, lng)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))
                        }
                    }
                    container.addView(item)
                }
            }
        }
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
