package dev.vehiclebridge.byd.bridge

import org.json.JSONObject

/** OPENAIR payload, based on jkaberg/pyBYD and Overdrive-release examples.
 * China-region Qin PLUS DM-i compatibility remains UNVERIFIED.
 */
internal object ClimateControlParams {
    fun fixed26C30Minutes(): String = JSONObject().apply {
        put("mainSettingTemp", 12) // 26 C - 14 C offset
        put("copilotSettingTemp", 12)
        put("cycleMode", 2)
        put("timeSpan", 5) // 30 minutes
        put("remoteMode", 4)
        put("airAccuracy", 1)
        put("airConditioningMode", 1)
        put("airSet", JSONObject.NULL)
    }.toString()
}
