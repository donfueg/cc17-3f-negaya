package com.example.crud2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
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
import java.text.SimpleDateFormat
import java.util.*

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
            heartRateUpdateTimer.postDelayed(this, 10000)
        }
    }

    private var mainUserHeartRate: Int? = null
    private var mainUserLastUpdatedHeartRate: Int? = null
    private var mainUserLastUpdatedTime: String? = null

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

        heartRateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.getValue(Int::class.java)
                if (hr != null) {
                    mainUserLastUpdatedHeartRate = mainUserHeartRate
                    mainUserHeartRate = hr
                    mainUserLastUpdatedTime = getCurrentTimeStamp()
                    runOnUiThread {
                        usernameTextView.text = "Hello $currentUsername"
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
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_user, null)

        val addUserName = view.findViewById<EditText>(R.id.inputName)
        val addUserMobile = view.findViewById<EditText>(R.id.inputMobile)
        val addUserButton = view.findViewById<Button>(R.id.bottomSheetAddUserButton)
        val userListLayout = view.findViewById<LinearLayout>(R.id.bottomUserListContainer)
        val mainUserInfoTextView = view.findViewById<TextView>(R.id.mainUserInfoTextView)

        fun updateMainUserHRText() {
            val mainHR = mainUserHeartRate ?: 0
            val lastHR = mainUserLastUpdatedHeartRate ?: 0
            val lastTime = mainUserLastUpdatedTime ?: "N/A"
            mainUserInfoTextView.text =
                "Main User: $currentUsername | Current HR: $mainHR bpm | Last HR: $lastHR bpm\nLast Updated: $lastTime"
        }

        updateMainUserHRText()
        mainUserInfoTextView.setOnClickListener {
            showMainUserOnMap()
            dialog.dismiss()
        }

        // Mobile number validation
        addUserMobile.setText("+63")
        addUserMobile.setSelection(addUserMobile.text.length)
        addUserMobile.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                var text = s.toString()
                if (!text.startsWith("+63")) text = "+63"
                if (text.length > 13) text = text.substring(0, 13)
                addUserMobile.setText(text)
                addUserMobile.setSelection(text.length)
                isEditing = false
            }
        })

        addUserButton.setOnClickListener {
            val name = addUserName.text.toString().trim()
            val mobile = addUserMobile.text.toString().trim()
            if (name.isEmpty() || mobile.isEmpty()) {
                showToast("Please enter both name and mobile number")
                return@setOnClickListener
            }

            val numberPart = mobile.removePrefix("+63")
            if (!numberPart.matches(Regex("9\\d{9}"))) {
                showToast("Mobile number must start with 9 and be 10 digits after +63")
                return@setOnClickListener
            }

            val randomLocation = getRandomBaguioLocation()
            val nowTime = getCurrentTimeStamp()
            val newUserRef = userContactsRef.push()
            val userData = mapOf(
                "name" to name,
                "mobile" to mobile,
                "latitude" to randomLocation.latitude,
                "longitude" to randomLocation.longitude,
                "heartrate" to Random.nextInt(60, 100),
                "lastHeartrate" to Random.nextInt(60, 100),
                "lastUpdatedTime" to nowTime
            )

            newUserRef.setValue(userData).addOnSuccessListener {
                showToast("User added successfully")
                addUserName.text.clear()
                addUserMobile.setText("+63")
                addUserMobile.setSelection(addUserMobile.text.length)
            }.addOnFailureListener { e ->
                showToast("Failed to add user: ${e.message}")
            }
        }

        // Single listener for showing users
        userContactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userListLayout.removeAllViews()
                for (userSnap in snapshot.children) {
                    val name = userSnap.child("name").getValue(String::class.java) ?: ""
                    val currentHR = userSnap.child("heartrate").getValue(Int::class.java) ?: 0
                    val lastHR = userSnap.child("lastHeartrate").getValue(Int::class.java) ?: 0
                    val lastUpdatedTime = userSnap.child("lastUpdatedTime").getValue(String::class.java) ?: "N/A"
                    val lat = userSnap.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = userSnap.child("longitude").getValue(Double::class.java) ?: 0.0

                    val userRow = LinearLayout(this@Dashboard)
                    userRow.orientation = LinearLayout.VERTICAL
                    userRow.setPadding(8, 8, 8, 8)
                    userRow.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                    userRow.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 4, 0, 4) }

                    val topRow = LinearLayout(this@Dashboard).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val nameTextView = TextView(this@Dashboard).apply {
                        text = name
                        textSize = 14f
                        setTextColor(android.graphics.Color.BLACK)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.5f)
                    }

                    val currentHRTextView = TextView(this@Dashboard).apply {
                        text = "Now: $currentHR"
                        textSize = 12f
                        setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                    }

                    val lastHRTextView = TextView(this@Dashboard).apply {
                        text = "Last: $lastHR"
                        textSize = 12f
                        setTextColor(android.graphics.Color.parseColor("#F44336"))
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                    }

                    val locateButton = Button(this@Dashboard).apply {
                        text = "Locate"
                        textSize = 10f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                        layoutParams = LinearLayout.LayoutParams(0, 32.dpToPx(), 1.5f).apply { setMargins(4.dpToPx(), 0, 0, 0) }
                        setOnClickListener {
                            showUserOnMap(name, lat, lng, currentHR)
                            dialog.dismiss()
                        }
                    }

                    topRow.addView(nameTextView)
                    topRow.addView(currentHRTextView)
                    topRow.addView(lastHRTextView)
                    topRow.addView(locateButton)

                    val timeTextView = TextView(this@Dashboard).apply {
                        text = "Updated: $lastUpdatedTime"
                        textSize = 10f
                        setTextColor(android.graphics.Color.DKGRAY)
                    }

                    userRow.addView(topRow)
                    userRow.addView(timeTextView)
                    userListLayout.addView(userRow)
                }
                updateMainUserHRText()
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load users: ${error.message}")
            }
        })

        dialog.setContentView(view)
        dialog.show()
    }


    private fun getCurrentTimeStamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun showMainUserOnMap() {
        if (::mMap.isInitialized && mainUserMarker != null) {
            val userLocation = mainUserMarker!!.position
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            mainUserMarker!!.showInfoWindow()
            val currentHR = mainUserHeartRate ?: 0
            showToast("$currentUsername (You) - Heart Rate: $currentHR bpm")
        }
    }

    private fun showUserOnMap(name: String, lat: Double, lng: Double, heartrate: Int) {
        if (::mMap.isInitialized) {
            val userLocation = LatLng(lat, lng)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            val marker = addedUserMarkers.values.find { it.title == name }
            marker?.showInfoWindow()
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
        val nowTime = getCurrentTimeStamp()
        userContactsRef.get().addOnSuccessListener { snapshot ->
            for (contactSnap in snapshot.children) {
                val currentHR = contactSnap.child("heartrate").getValue(Int::class.java) ?: 60
                val newHR = Random.nextInt(60, 120)
                contactSnap.ref.child("lastHeartrate").setValue(currentHR)
                contactSnap.ref.child("heartrate").setValue(newHR)
                contactSnap.ref.child("lastUpdatedTime").setValue(nowTime)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateUpdateTimer.removeCallbacks(heartRateUpdateRunnable)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
