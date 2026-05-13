package com.gramayatri.app.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gramayatri.app.model.BusPing
import com.gramayatri.app.model.BusRoute

/**
 * Single source of truth for all Firebase Realtime Database operations.
 *
 * Firebase structure:
 * /routes/{routeId}          — static route + stop definitions
 * /pings/{routeId}/{pingId}  — live community pings (last 24 h kept)
 * /alerts/{routeId}          — cancellation / special alerts
 */
class FirebaseRepository private constructor() {

    // ── Firebase refs ─────────────────────────────────────────────────────────
    private val db: DatabaseReference = FirebaseDatabase
        .getInstance("https://grama-yatri-5bfd2-default-rtdb.asia-southeast1.firebasedatabase.app")
        .reference
    private val routesRef: DatabaseReference = db.child("routes")
    private val pingsRef: DatabaseReference  = db.child("pings")
    private val alertsRef: DatabaseReference = db.child("alerts")

    // ── Callback interfaces ───────────────────────────────────────────────────

    interface RoutesCallback {
        fun onRoutesLoaded(routes: List<BusRoute>)
        fun onError(message: String)
    }

    interface PingCallback {
        fun onLatestPing(ping: BusPing)  // called every time a new ping arrives
        fun onError(message: String)
    }

    interface WriteCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    interface AlertCallback {
        fun onAlert(message: String?, reporterName: String?, timestamp: Long)
        fun onNoAlert()
    }

    // ── Routes ────────────────────────────────────────────────────────────────

    /**
     * Loads all routes once. Routes are mostly static so a single read is fine.
     */
    fun loadRoutes(callback: RoutesCallback) {
        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val routes = mutableListOf<BusRoute>()
                for (child in snapshot.children) {
                    val route = child.getValue(BusRoute::class.java)
                    if (route != null) {
                        route.routeId = child.key ?: ""
                        routes.add(route)
                    }
                }
                callback.onRoutesLoaded(routes)
            }

            override fun onCancelled(error: DatabaseError) {
                callback.onError(error.message)
            }
        })
    }

    // ── Pings ─────────────────────────────────────────────────────────────────

    /**
     * Submits a new community ping to Firebase.
     * The ping is stored under /pings/{routeId}/{pushId}.
     */
    fun submitPing(ping: BusPing, callback: WriteCallback) {
        val newPingRef = pingsRef.child(ping.routeId).push()
        ping.pingId = newPingRef.key
        newPingRef.setValue(ping)
            .addOnSuccessListener { callback.onSuccess() }
            .addOnFailureListener { e -> callback.onError(e.message ?: "Unknown error") }
    }

    /**
     * Listens in real-time for the most recent ping on a route.
     * Returns a ValueEventListener so the caller can remove it when done.
     */
    fun listenForLatestPing(routeId: String, callback: PingCallback): ValueEventListener {
        // Query the last 1 ping ordered by timestamp
        val query = pingsRef.child(routeId)
            .orderByChild("timestamp")
            .limitToLast(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val ping = child.getValue(BusPing::class.java)
                    if (ping != null) {
                        ping.pingId = child.key
                        callback.onLatestPing(ping)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback.onError(error.message)
            }
        }

        query.addValueEventListener(listener)
        return listener // caller must remove this listener to avoid leaks
    }

    /**
     * Removes a previously attached ping listener.
     */
    fun removePingListener(routeId: String, listener: ValueEventListener) {
        pingsRef.child(routeId)
            .orderByChild("timestamp")
            .limitToLast(1)
            .removeEventListener(listener)
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    /**
     * Posts a cancellation alert for a route.
     * Stored under /alerts/{routeId}/latest.
     */
    fun postCancellationAlert(
        routeId: String,
        reporterName: String,
        message: String,
        callback: WriteCallback
    ) {
        val alertRef = alertsRef.child(routeId).child("latest")
        alertRef.child("message").setValue(message)
        alertRef.child("reporterName").setValue(reporterName)
        alertRef.child("timestamp").setValue(System.currentTimeMillis())
            .addOnSuccessListener { callback.onSuccess() }
            .addOnFailureListener { e -> callback.onError(e.message ?: "Unknown error") }
    }

    /**
     * Listens for cancellation alerts on a route.
     * Returns the listener so the caller can detach it.
     */
    fun listenForAlerts(routeId: String, callback: AlertCallback): ValueEventListener {
        val alertRef = alertsRef.child(routeId).child("latest")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val message      = snapshot.child("message").getValue(String::class.java)
                    val reporter     = snapshot.child("reporterName").getValue(String::class.java)
                    val timestamp    = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    callback.onAlert(message, reporter, timestamp)
                } else {
                    callback.onNoAlert()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback.onNoAlert()
            }
        }

        alertRef.addValueEventListener(listener)
        return listener
    }

    fun removeAlertListener(routeId: String, listener: ValueEventListener) {
        alertsRef.child(routeId).child("latest").removeEventListener(listener)
    }

    // ── Seed data (dev helper) ────────────────────────────────────────────────

    /**
     * Seeds sample route data into Firebase.
     * Call this ONCE from a debug build to populate the database.
     */
    fun seedSampleData() {
        // Route: Kuppam → Bangalore (sample stops with avg travel times)
        val route1 = routesRef.child("route_kuppam_blr")
        route1.child("routeId").setValue("route_kuppam_blr")
        route1.child("routeName").setValue("Kuppam → Bangalore")
        route1.child("description").setValue("Village bus via NH-48")

        val stops = arrayOf(
            arrayOf("stop_kuppam",     "Kuppam",         "0", "0"),
            arrayOf("stop_nagaraja",   "Nagarajanpet",   "1", "8"),
            arrayOf("stop_palamaner",  "Palamaner",      "2", "12"),
            arrayOf("stop_bangarpet",  "Bangarpet",      "3", "20"),
            arrayOf("stop_kolar",      "Kolar",          "4", "15"),
            arrayOf("stop_malur",      "Malur",          "5", "18"),
            arrayOf("stop_whitefield", "Whitefield",     "6", "35"),
            arrayOf("stop_majestic",   "Majestic (Blr)", "7", "40")
        )

        for (s in stops) {
            val stopRef = route1.child("stops").child(s[0])
            stopRef.child("stopId").setValue(s[0])
            stopRef.child("villageName").setValue(s[1])
            stopRef.child("stopOrder").setValue(s[2].toInt())
            stopRef.child("avgMinutesFromPrev").setValue(s[3].toInt())
        }
    }

    // ── Singleton ────────────────────────────────────────────────────────────
    companion object {
        @Volatile
        private var instance: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository =
            instance ?: synchronized(this) {
                instance ?: FirebaseRepository().also { instance = it }
            }
    }
}
