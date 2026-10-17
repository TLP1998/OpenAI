package dev.vehiclebridge.byd.bridge

import org.json.JSONObject

/** Experimental OPENAIR params based on pyBYD.
 * Runtime is always 30 min (timeSpan = 5). Temperature comes from MQTT/UI.
 * Qin PLUS DM-i China region compatibility still requires real-vehicle test.
 */
internal object ClimateControlParams {
    const val DURATION_MINUTES: Int = 30
    fun forTemperature(celsius: Int): String {
        require(celsius in 15..31) { "Temperature must be 15..31 Celsius" }
        val raw = celsius - 14
        return JSONObject().apply {
            put("mainSettingTemp", raw)
            put("copilotSettingTemp", raw)
            put("cycleMode", 2)
            put("timeSpan", 5) // always 30 minutes
            put("remoteMode", 4)
            put("airAccuracy", 1)
            put("airConditioningMode", 1)
            put("airSet", JSONObject.NULL)
        }.toString()
    }
}
