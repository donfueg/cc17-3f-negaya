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
import android.text.Editable
import android.text.TextWatcher


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
    private val heartRateUpdateTimer = android.os.Handler(android.os.Looper.getMainLooper())
    private val heartRateUpdateRunnable = object : Runnable {
        override fun run() {
            updateContactsHeartRates()
            heartRateUpdateTimer.postDelayed(this, 10000) // Update every 10 seconds
        }
    }

    private var mainUserHeartRate: Int? = null
    private var mainUserLastUpdatedHeartRate: Int? = null

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
                startHeartRateUpdates()
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
        // Main user location
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)
                if (lat != null && lng != null && ::mMap.isInitialized) {
                    val newLocation = LatLng(lat, lng)
                    runOnUiThread {
                        if (mainUserMarker == null) {
                            mainUserMarker = mMap.addMarker(
                                MarkerOptions()
                                    .position(newLocation)
                                    .title("You (Main User)")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            )
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 15f))
                        } else {
                            mainUserMarker?.position = newLocation
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to get location updates: ${error.message}")
            }
        })

        // Main user heart rate
        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.getValue(Int::class.java)
                if (hr != null) {
                    mainUserLastUpdatedHeartRate = mainUserHeartRate
                    mainUserHeartRate = hr
                    runOnUiThread {
                        usernameTextView.text = "Hello $currentUsername"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to get heart rate updates: ${error.message}")
            }
        })

        // Contacts real-time update
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
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_user, null)

        val addUserName = view.findViewById<EditText>(R.id.inputName)
        val addUserMobile = view.findViewById<EditText>(R.id.inputMobile)
        val addUserButton = view.findViewById<Button>(R.id.bottomSheetAddUserButton)
        val userListLayout = view.findViewById<LinearLayout>(R.id.bottomUserListContainer)
        val mainUserInfoTextView = view.findViewById<TextView>(R.id.mainUserInfoTextView)

        // Display main user info and make it clickable
        val mainUserHR = mainUserHeartRate ?: 0
        val lastUserHR = mainUserLastUpdatedHeartRate ?: 0
        mainUserInfoTextView.text = "Main User: $currentUsername | Current HR: $mainUserHR bpm | Last HR: $lastUserHR bpm"

        // Make main user info clickable
        mainUserInfoTextView.setOnClickListener {
            showMainUserOnMap()
            dialog.dismiss()
        }

        // Set +63 prefix initially
        addUserMobile.setText("+63")
        addUserMobile.setSelection(addUserMobile.text.length)

        // Prevent removing +63 prefix
        addUserMobile.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                if (!s.toString().startsWith("+63")) {
                    addUserMobile.setText("+63")
                    addUserMobile.setSelection(addUserMobile.text.length)
                }
                isEditing = false
            }
        })

        // Add User Button Click
        addUserButton.setOnClickListener {
            val name = addUserName.text.toString().trim()
            val mobile = addUserMobile.text.toString().trim()

            if (name.isEmpty() || mobile.isEmpty()) {
                showToast("Please enter both name and mobile number")
                return@setOnClickListener
            }

            val numberPart = mobile.removePrefix("+63")
            if (!numberPart.matches(Regex("\\d{10}"))) {
                showToast("Mobile number must be in format +639XXXXXXXXX")
                return@setOnClickListener
            }

            val randomLocation = getRandomBaguioLocation()
            val newUserRef = userContactsRef.push()
            val userData = mapOf(
                "name" to name,
                "mobile" to mobile,
                "latitude" to randomLocation.latitude,
                "longitude" to randomLocation.longitude,
                "heartrate" to Random.nextInt(60, 100),
                "lastHeartrate" to Random.nextInt(60, 100)
            )

            newUserRef.setValue(userData)
                .addOnSuccessListener {
                    showToast("User added successfully")
                    addUserName.text.clear()
                    addUserMobile.setText("+63")
                    addUserMobile.setSelection(addUserMobile.text.length)
                }
                .addOnFailureListener { e ->
                    showToast("Failed to add user: ${e.message}")
                }
        }

        // Populate user list with 4-column layout (including Locate button)
        userContactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userListLayout.removeAllViews()

                for (userSnapshot in snapshot.children) {
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: ""
                    val heartrate = userSnapshot.child("heartrate").getValue(Int::class.java) ?: 0
                    val lastHeartrate = userSnapshot.child("lastHeartrate").getValue(Int::class.java) ?: 0
                    val lat = userSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = userSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0

                    // Create horizontal layout for each user row
                    val userRow = LinearLayout(this@Dashboard)
                    userRow.orientation = LinearLayout.HORIZONTAL
                    userRow.setPadding(8, 8, 8, 8)
                    userRow.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(0, 4, 0, 4)
                    userRow.layoutParams = layoutParams
                    userRow.gravity = Gravity.CENTER_VERTICAL

                    // User Name (weight 2.5)
                    val nameTextView = TextView(this@Dashboard)
                    nameTextView.text = name
                    nameTextView.textSize = 14f
                    nameTextView.setTextColor(android.graphics.Color.BLACK)
                    val nameParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.5f)
                    nameTextView.layoutParams = nameParams

                    // Current Heart Rate (weight 2)
                    val currentHRTextView = TextView(this@Dashboard)
                    currentHRTextView.text = "Now: $heartrate"
                    currentHRTextView.textSize = 12f
                    currentHRTextView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    currentHRTextView.gravity = Gravity.CENTER
                    val currentHRParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                    currentHRTextView.layoutParams = currentHRParams

                    // Last Heart Rate (weight 2)
                    val lastHRTextView = TextView(this@Dashboard)
                    lastHRTextView.text = "Last: $lastHeartrate"
                    lastHRTextView.textSize = 12f
                    lastHRTextView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                    lastHRTextView.gravity = Gravity.CENTER
                    val lastHRParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                    lastHRTextView.layoutParams = lastHRParams

                    // Locate Button (weight 1.5)
                    val locateButton = Button(this@Dashboard)
                    locateButton.text = "Locate"
                    locateButton.textSize = 10f
                    locateButton.setTextColor(android.graphics.Color.WHITE)
                    locateButton.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                    val buttonParams = LinearLayout.LayoutParams(0, 32.dpToPx(), 1.5f)
                    buttonParams.setMargins(4.dpToPx(), 0, 0, 0)
                    locateButton.layoutParams = buttonParams
                    locateButton.setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())

                    // Set click listener for the locate button
                    locateButton.setOnClickListener {
                        showUserOnMap(name, lat, lng, heartrate)
                        dialog.dismiss()
                    }

                    // Add all views to the row
                    userRow.addView(nameTextView)
                    userRow.addView(currentHRTextView)
                    userRow.addView(lastHRTextView)
                    userRow.addView(locateButton)

                    userListLayout.addView(userRow)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load users: ${error.message}")
            }
        })

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showMainUserOnMap() {
        if (::mMap.isInitialized && mainUserMarker != null) {
            val userLocation = mainUserMarker!!.position

            // Move camera to main user's location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

            // Show main user marker info window
            mainUserMarker!!.showInfoWindow()

            // Show toast with main user details
            val currentHR = mainUserHeartRate ?: 0
            showToast("$currentUsername (You) - Heart Rate: $currentHR bpm")
        }
    }

    private fun showUserOnMap(name: String, lat: Double, lng: Double, heartrate: Int) {
        if (::mMap.isInitialized) {
            val userLocation = LatLng(lat, lng)

            // Move camera to user's location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

            // Find the marker for this user and show info window
            val marker = addedUserMarkers.values.find { it.title == name }
            if (marker != null) {
                marker.showInfoWindow()
            }

            // Optional: Show a toast with user details
            showToast("$name - Heart Rate: $heartrate bpm")
        }
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
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun navigateToLogin() {
        startActivity(
            Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        finish()
    }

    private fun startHeartRateUpdates() {
        heartRateUpdateTimer.post(heartRateUpdateRunnable)
    }

    private fun updateContactsHeartRates() {
        userContactsRef.get().addOnSuccessListener { snapshot ->
            for (contactSnap in snapshot.children) {
                val currentHR = contactSnap.child("heartrate").getValue(Int::class.java) ?: 60
                val newHR = Random.nextInt(60, 120)

                // Update heart rate values
                contactSnap.ref.child("lastHeartrate").setValue(currentHR)
                contactSnap.ref.child("heartrate").setValue(newHR)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateUpdateTimer.removeCallbacks(heartRateUpdateRunnable)
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}