package dev.vehiclebridge.byd.bridge

import org.json.JSONObject

/** Experimental BYD OPENAIR parameters. Timeout is configurable in the app,
 * and is shared by both manual controls and Bemfa/Mi Home voice commands.
 * Tested against source conventions; real vehicle support not yet verified.
 */
internal object ClimateControlParams {
    val ALLOWED_DURATIONS_MINUTES = listOf(10, 15, 20, 25, 30)
    const val DEFAULT_DURATION_MINUTES = 30

    fun forTemperature(celsius: Int, durationMinutes: Int): String {
        require(celsius in 15..31) { "Temperature must be 15..31 Celsius" }
        val index = ALLOWED_DURATIONS_MINUTES.indexOf(durationMinutes)
        require(index >= 0) { "Unsupported duration: $durationMinutes" }
        val raw = celsius - 14
        return JSONObject().apply {
            put("mainSettingTemp", raw)
            put("copilotSettingTemp", raw)
            put("cycleMode", 2)
            put("timeSpan", index + 1)
            put("remoteMode", 4)
            put("airAccuracy", 1)
            put("airConditioningMode", 1)
            put("airSet", JSONObject.NULL)
        }.toString()
    }
}
