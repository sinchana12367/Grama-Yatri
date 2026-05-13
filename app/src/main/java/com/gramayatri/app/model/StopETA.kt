package com.gramayatri.app.model

/**
 * Computed ETA for a single stop, derived from the latest community ping.
 * This is a UI model — not stored in Firebase directly.
 */
class StopETA(val stop: BusStop) {

    var estimatedArrivalMillis: Long = 0L  // 0 = no data
    var busAlreadyPassed: Boolean = false
    var isCancelled: Boolean = false
    var reporterName: String? = null       // who triggered this ETA chain

    /**
     * Returns a human-readable ETA string.
     * e.g. "~12 mins", "Bus passed", "Cancelled", "No data yet"
     */
    fun getEtaLabel(): String {
        if (isCancelled) return "Cancelled today"
        if (busAlreadyPassed) return "Bus passed"
        if (estimatedArrivalMillis == 0L) return "No data yet"

        val diffMillis = estimatedArrivalMillis - System.currentTimeMillis()
        if (diffMillis <= 0) return "Arriving now"

        val mins = diffMillis / 60_000L
        if (mins < 1) return "< 1 min"
        return "~$mins min${if (mins == 1L) "" else "s"}"
    }
}
