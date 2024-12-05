package com.example.crud2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private lateinit var showPoliceButton: Button
    private var currentLocation: Location? = null

    // Permission result callback
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map2)

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the location provider client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "YOUR_API_KEY") // Replace with your API key
        }

        // Initialize the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Check for location permissions and request if necessary
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getCurrentLocation()
        }

        // Button to show nearest police station
        showPoliceButton = findViewById(R.id.showPoliceButton)
        showPoliceButton.setOnClickListener {
            currentLocation?.let { location ->
                // Call function to find the nearest police station
                findNearestPoliceStation(location.latitude, location.longitude)
            } ?: run {
                Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // This will be called when the map is ready
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable My Location layer
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
    }

    // Function to get the current location
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                currentLocation = it
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
            } ?: run {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to find the nearest police station
    private fun findNearestPoliceStation(latitude: Double, longitude: Double) {
        val placesClient: PlacesClient = Places.createClient(this)
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        // Request for places around the current location
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        placesClient.findCurrentPlace(request)
            .addOnSuccessListener { response ->
                val results = response.placeLikelihoods
                if (results.isEmpty()) {
                    Toast.makeText(this, "No nearby places found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                var nearestPoliceStation: Place? = null
                var minDistance = Double.MAX_VALUE

                // Iterate through the results to find police stations
                for (placeLikelihood in results) {
                    val place = placeLikelihood.place
                    if (place.name.contains("police", ignoreCase = true)) {
                        val placeLocation = place.latLng
                        placeLocation?.let {
                            // Calculate the distance to the police station
                            val distance = calculateDistance(latitude, longitude, it.latitude, it.longitude)
                            if (distance < minDistance) {
                                nearestPoliceStation = place
                                minDistance = distance
                            }
                        }
                    }
                }

                // If a police station was found, highlight it
                nearestPoliceStation?.let { policeStation ->
                    val policeLocation = policeStation.latLng
                    googleMap?.clear() // Clear previous markers
                    googleMap?.addMarker(MarkerOptions().position(policeLocation).title("Nearest Police Station"))
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(policeLocation, 15f))
                } ?: run {
                    Toast.makeText(this, "No police stations found nearby", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapActivity", "Error finding police stations: ${e.message}")
                Toast.makeText(this, "Error finding police stations: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to calculate the distance between two coordinates
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}
