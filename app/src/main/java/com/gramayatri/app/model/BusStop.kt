package com.gramayatri.app.model

/**
 * Represents a single village stop on a bus route.
 * avgMinutesFromPrev: average travel time in minutes from the previous stop.
 */
data class BusStop(
    var stopId: String = "",
    var villageName: String = "",
    var stopOrder: Int = 0,
    var avgMinutesFromPrev: Int = 0
)
