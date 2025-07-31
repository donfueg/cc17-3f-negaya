package com.example.crud2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var showPoliceButton: Button
    private lateinit var policeInfoTextView: TextView
    private lateinit var backButton: ImageButton

    private lateinit var locationDatabase: DatabaseReference
    private lateinit var policeDatabase: DatabaseReference

    private var firebaseLocation: LatLng? = null
    private var hasZoomedInitially = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map2)

        locationDatabase = FirebaseDatabase.getInstance().getReference("location")
        policeDatabase = FirebaseDatabase.getInstance("https://ahhh-41e71-default-rtdb.firebaseio.com")
            .getReference("location/police_stations")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        showPoliceButton = findViewById(R.id.showPoliceButton)
        policeInfoTextView = findViewById(R.id.policeInfoTextView)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { onBackPressed() }

        showPoliceButton.setOnClickListener {
            firebaseLocation?.let { location ->
                findNearestPoliceStation(location.latitude, location.longitude)
            } ?: Toast.makeText(this, "Fetching current location...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = false
            googleMap.uiSettings.isMyLocationButtonEnabled = false
        }

        listenToRealtimeLocation()
    }

    private fun listenToRealtimeLocation() {
        locationDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java)
                val longitude = snapshot.child("longitude").getValue(Double::class.java)

                if (latitude != null && longitude != null) {
                    firebaseLocation = LatLng(latitude, longitude)

                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(firebaseLocation!!)
                            .title("Your Location (Realtime)")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )

                    if (!hasZoomedInitially) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firebaseLocation!!, 15f))
                        hasZoomedInitially = true
                    } else {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(firebaseLocation!!))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Map, "Failed to load real-time location", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun findNearestPoliceStation(lat: Double, lng: Double) {
        policeDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@Map, "No police stations found", Toast.LENGTH_SHORT).show()
                    return
                }

                googleMap.clear()

                firebaseLocation?.let {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("Your Location (Realtime)")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }

                var nearestStation: LatLng? = null
                var nearestSnapshot: DataSnapshot? = null
                var minDistance = Double.MAX_VALUE

                for (stationSnapshot in snapshot.children) {
                    val policeLat = stationSnapshot.child("latitude").getValue(Double::class.java)
                    val policeLng = stationSnapshot.child("longitude").getValue(Double::class.java)
                    if (policeLat == null || policeLng == null) continue

                    val distance = calculateDistance(lat, lng, policeLat, policeLng)

                    if (distance < minDistance) {
                        minDistance = distance
                        nearestStation = LatLng(policeLat, policeLng)
                        nearestSnapshot = stationSnapshot
                    }
                }

                nearestStation?.let {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("Nearest Police Station")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    firebaseLocation?.let { userLoc -> drawRouteAndShowETA(userLoc, it) }
                }

                nearestSnapshot?.let {
                    val name = it.key ?: "Unnamed Station"
                    val contact = it.child("Contact").getValue(String::class.java) ?: "No contact"
                    policeInfoTextView.text = "\uD83D\uDEA8 $name\n\uD83D\uDCDE $contact"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Map, "Error reading police stations", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    private fun drawRouteAndShowETA(origin: LatLng, destination: LatLng) {
        val apiKey = "YOUR_GOOGLE_MAPS_API_KEY"
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&key=$apiKey"

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonData = response.body?.string()

                val jsonObject = JSONObject(jsonData!!)
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                    val legs = route.getJSONArray("legs").getJSONObject(0)
                    val duration = legs.getJSONObject("duration").getString("text")

                    val decodedPath = decodePolyline(overviewPolyline)

                    runOnUiThread {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .color(Color.BLUE)
                                .width(10f)
                        )
                        policeInfoTextView.append("\n\uD83D\uDE97 ETA: $duration")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@Map, "Failed to get directions", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }
}
