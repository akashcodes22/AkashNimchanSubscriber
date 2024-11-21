package com.example.akashnimchansubscriber

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.akashnimchansubscriber.adapters.PubAdapter
import com.example.akashnimchansubscriber.models.MarkerPoints
import com.example.akashnimchansubscriber.models.PubModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.abs

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    companion object {
        private const val POLYLINE_WIDTH = 5f
        private const val MAP_PADDING = 100
    }

    private lateinit var googleMap: GoogleMap
    private val markerPoints = mutableListOf<MarkerPoints>()
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var pubAdapter: PubAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyEdgeToEdgeInsets()
        initializeMapFragment()
        databaseHelper = DatabaseHelper(this, null)
        setupPublisherRecyclerView()
        MQTTService(this, this) // Initialize MQTT service
    }

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.let { it.getMapAsync(this) }
    }

    private fun setupPublisherRecyclerView() {
        val publisherRecyclerView: RecyclerView = findViewById(R.id.rvPublishers)
        pubAdapter = PubAdapter(fetchPublisherData())
        publisherRecyclerView.layoutManager = LinearLayoutManager(this)
        publisherRecyclerView.adapter = pubAdapter
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        renderStudentPaths()
    }

    override fun onNewLocationReceived(studentId: String) {
        val locations = databaseHelper.getAllLocations(studentId)
        locations.forEach {
            markerPoints.add(MarkerPoints(markerPoints.size + 1, LatLng(it.latitude, it.longitude)))
        }

        runOnUiThread {
            updatePolyline()
            refreshPublisherData(fetchPublisherData())
        }
    }

    private fun updatePolyline() {
        val latLngPoints = markerPoints.map { it.point }

        googleMap.addPolyline(configurePolylineOptions(latLngPoints, Color.BLUE))
        moveCameraToBounds(latLngPoints)
    }

    private fun renderStudentPaths() {
        val studentLocationData = databaseHelper.getAllLocationsGroupedByStudent()

        studentLocationData.forEach { (studentId, locations) ->
            googleMap.addPolyline(
                configurePolylineOptions(
                    locations.map { LatLng(it.latitude, it.longitude) },
                    determineStudentColor(studentId)
                )
            )
        }
    }

    private fun configurePolylineOptions(points: List<LatLng>, color: Int): PolylineOptions {
        return PolylineOptions()
            .addAll(points)
            .color(color)
            .width(POLYLINE_WIDTH)
            .geodesic(true)
    }

    private fun moveCameraToBounds(points: List<LatLng>) {
        val bounds = LatLngBounds.builder().apply {
            points.forEach { include(it) }
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), MAP_PADDING))
    }

    private fun determineStudentColor(studentId: String): Int {
        val availableColors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.BLACK)
        val index = abs(studentId.hashCode() % availableColors.size)
        return availableColors[index]
    }

    private fun fetchPublisherData(): MutableList<PubModel> {
        val studentData = databaseHelper.getAllLocationsGroupedByStudent()
        return studentData.map { (studentId, locations) ->
            val speeds = locations.map { it.speed }
            PubModel(studentId, speeds.minOrNull() ?: 0.0, speeds.maxOrNull() ?: 0.0)
        }.toMutableList()
    }

    private fun refreshPublisherData(updatedList: MutableList<PubModel>) {
        pubAdapter.updatePublisherList(updatedList)
    }
}