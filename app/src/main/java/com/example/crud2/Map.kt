package com.example.crud2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var showPoliceButton: Button

    private lateinit var locationDatabase: DatabaseReference
    private lateinit var policeDatabase: DatabaseReference

    private var firebaseLocation: LatLng? = null
    private var locationFetched: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase references
        locationDatabase = FirebaseDatabase.getInstance().getReference("location")
        policeDatabase = FirebaseDatabase.getInstance("https://ahhh-41e71-default-rtdb.firebaseio.com")
            .getReference("location/police_stations")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        showPoliceButton = findViewById(R.id.showPoliceButton)
        showPoliceButton.setOnClickListener {
            firebaseLocation?.let { location ->
                findNearestPoliceStation(location.latitude, location.longitude)
            } ?: run {
                Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
        }

        fetchLocationFromFirebase()
    }

    private fun fetchLocationFromFirebase() {
        locationDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java)
                val longitude = snapshot.child("longitude").getValue(Double::class.java)

                Log.d("FirebaseLocation", "Fetched location: lat=$latitude, lon=$longitude")

                if (latitude != null && longitude != null) {
                    firebaseLocation = LatLng(latitude, longitude)
                    locationFetched = true

                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(firebaseLocation!!)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firebaseLocation!!, 15f))
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

    private fun findNearestPoliceStation(latitude: Double, longitude: Double) {
        if (!locationFetched) {
            Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show()
            return
        }

        policeDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(policeStationsSnapshot: DataSnapshot) {
                if (!policeStationsSnapshot.exists() || !policeStationsSnapshot.hasChildren()) {
                    Log.e("FirebaseData", "No police station data found")
                    Toast.makeText(this@Map, "No police stations found", Toast.LENGTH_SHORT).show()
                    return
                }

                googleMap.clear() // Clear markers only after confirming data exists

                firebaseLocation?.let {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }

                var nearestStation: LatLng? = null
                var minDistance = Double.MAX_VALUE

                for (stationSnapshot in policeStationsSnapshot.children) {
                    val policeLat = stationSnapshot.child("latitude").getValue(Double::class.java)
                    val policeLng = stationSnapshot.child("longitude").getValue(Double::class.java)

                    if (policeLat == null || policeLng == null) {
                        Log.w("FirebaseData", "Skipping station ${stationSnapshot.key}: Missing coordinates")
                        continue
                    }

                    val policeLocation = LatLng(policeLat, policeLng)
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(policeLocation)
                            .title("Police Station")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )

                    val distance = calculateDistance(latitude, longitude, policeLat, policeLng)
                    Log.d("DistanceCalc", "Distance to ${stationSnapshot.key}: $distance km")

                    if (distance < minDistance) {
                        minDistance = distance
                        nearestStation = policeLocation
                    }
                }

                nearestStation?.let {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("Nearest Police Station")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    Toast.makeText(this@Map, "Nearest police station marked", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this@Map, "No valid police stations found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapActivity", "Error fetching police stations: ${error.message}")
                Toast.makeText(this@Map, "Error reading police station data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ✅ FIXED: Now returns Double instead of Float
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble() / 1000 // Convert meters to km and ensure Double type
    }
}
