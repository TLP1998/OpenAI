package dev.vehiclebridge.byd.bridge

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ClimateControlParamsTest {
    @Test fun supportedDurationCodes() {
        for ((index, duration) in ClimateControlParams.ALLOWED_DURATIONS_MINUTES.withIndex()) {
            val value = JSONObject(ClimateControlParams.forTemperature(26, duration))
            assertEquals(index + 1, value.getInt("timeSpan"))
            assertEquals(12, value.getInt("mainSettingTemp"))
            assertEquals(12, value.getInt("copilotSettingTemp"))
            assertEquals(4, value.getInt("remoteMode"))
        }
    }

    @Test fun dynamicTemperatureWithSavedDuration() {
        val payload = JSONObject(ClimateControlParams.forTemperature(23, 20))
        assertEquals(9, payload.getInt("mainSettingTemp"))
        assertEquals(3, payload.getInt("timeSpan"))
    }

    @Test fun invalidSettingsRejected() {
        for (duration in listOf(0, 5, 40, 60)) {
            try { ClimateControlParams.forTemperature(26, duration); fail("duration $duration") }
            catch (_: IllegalArgumentException) {}
        }
        for (temp in listOf(14, 32)) {
            try { ClimateControlParams.forTemperature(temp, 30); fail("temp $temp") }
            catch (_: IllegalArgumentException) {}
        }
    }
}
