package com.gramayatri.app.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gramayatri.app.R
import com.gramayatri.app.data.FirebaseRepository
import com.gramayatri.app.model.BusPing
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.model.BusStop
import com.gramayatri.app.util.UserPrefs

/**
 * Lets a community member report the bus location.
 * Two ping types:
 *   ON_BUS     — "I am currently on the bus"
 *   BUS_PASSED — "The bus just passed my stop"
 */
class PingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROUTE_ID   = "route_id"
        const val EXTRA_ROUTE_NAME = "route_name"
    }

    private lateinit var routeId: String
    private lateinit var routeName: String
    private var currentRoute: BusRoute? = null
    private val stops = mutableListOf<BusStop>()
    private var selectedStopIndex = 0

    private lateinit var spinnerStops: Spinner
    private lateinit var rgPingType:   RadioGroup
    private lateinit var btnSubmit:    Button
    private lateinit var progressBar:  ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ping)

        routeId   = intent.getStringExtra(EXTRA_ROUTE_ID)   ?: ""
        routeName = intent.getStringExtra(EXTRA_ROUTE_NAME) ?: ""

        supportActionBar?.apply {
            title = "Ping Bus Location"
            setDisplayHomeAsUpEnabled(true)
        }

        spinnerStops = findViewById(R.id.spinner_stops)
        rgPingType   = findViewById(R.id.rg_ping_type)
        btnSubmit    = findViewById(R.id.btn_submit_ping)
        progressBar  = findViewById(R.id.progress_bar)

        loadStops()

        btnSubmit.setOnClickListener { submitPing() }
    }

    private fun loadStops() {
        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled    = false

        FirebaseRepository.getInstance().loadRoutes(object : FirebaseRepository.RoutesCallback {
            override fun onRoutesLoaded(routes: List<BusRoute>) {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled    = true

                currentRoute = routes.find { it.routeId == routeId }

                currentRoute?.getStopsList().let { routeStops ->
                    if (routeStops != null) {
                        stops.clear()
                        stops.addAll(routeStops)
                        populateSpinner()
                    }
                }
            }

            override fun onError(message: String) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@PingActivity, "Could not load stops", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateSpinner() {
        val stopNames = stops.map { it.villageName }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            stopNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerStops.adapter = adapter

        spinnerStops.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedStopIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun submitPing() {
        if (stops.isEmpty()) {
            Toast.makeText(this, "No stops loaded", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStop = stops[selectedStopIndex]
        val pingType = if (rgPingType.checkedRadioButtonId == R.id.rb_on_bus) "ON_BUS" else "BUS_PASSED"

        val prefs = UserPrefs(this)
        val reporterName = prefs.getDisplayName()

        val ping = BusPing(
            pingId       = null,                    // set by Firebase push()
            routeId      = routeId,
            stopId       = selectedStop.stopId,
            stopOrder    = selectedStop.stopOrder,
            timestamp    = System.currentTimeMillis(),
            reporterName = reporterName,
            pingType     = pingType,
            isCancellation = false                  // not a cancellation
        )

        btnSubmit.isEnabled    = false
        progressBar.visibility = View.VISIBLE

        FirebaseRepository.getInstance().submitPing(ping, object : FirebaseRepository.WriteCallback {
            override fun onSuccess() {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@PingActivity,
                    "Thanks $reporterName! Ping sent.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }

            override fun onError(message: String) {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled    = true
                Toast.makeText(
                    this@PingActivity,
                    "Failed to send ping: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
