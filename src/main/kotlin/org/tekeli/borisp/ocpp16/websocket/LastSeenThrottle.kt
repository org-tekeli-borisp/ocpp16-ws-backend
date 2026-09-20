package org.tekeli.borisp.ocpp16.websocket

class LastSeenThrottle(
    private val intervalMillis: Long = 30_000,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var lastTouchMillis: Long? = null

    @Synchronized
    fun shouldTouch(): Boolean {
        val now = clock()
        val last = lastTouchMillis
        if (last == null || now - last >= intervalMillis) {
            lastTouchMillis = now
            return true
        }
        return false
    }
}
