package com.gramayatri.app.model

/**
 * A "ping" event submitted by a community member.
 * Records when a user saw the bus at a specific stop.
 *
 * Firebase path: /pings/{routeId}/{pingId}
 */
data class BusPing(
    var pingId: String? = null,
    var routeId: String = "",
    var stopId: String = "",
    var stopOrder: Int = 0,              // index of the stop where bus was seen
    var timestamp: Long = 0L,            // epoch millis when ping was submitted
    var reporterName: String = "",       // display name of the reporter
    var pingType: String = "",           // "ON_BUS" or "BUS_PASSED"
    var isCancellation: Boolean = false  // true if user reports bus is cancelled
)
