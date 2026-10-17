package dev.vehiclebridge.byd.bridge

/**
 * V4.0 standalone parsing prototype.
 * This is not yet integrated into BemfaService and sends NO vehicle commands.
 *
 * The official Bemfa air-conditioner topic message format must be observed
 * on the user's actual Xiaomi/Bemfa integration before enabling actuation.
 */
internal sealed interface XiaodiCommand {
    data object PowerOnRequested : XiaodiCommand
    data object PowerOffRequested : XiaodiCommand
    data class AirConditionerSettings(val temperatureCelsius: Int) : XiaodiCommand
}

internal object XiaodiCommandParser {
    /**
     * Strictly parse an incoming MQTT payload, never defaulting to unlock,
     * climate start, or other actuation when a payload is ambiguous.
     *
     * Important: "on" is a virtual AC power-on REQUEST, not verified
     * engine start. Actual vehicle start must remain disabled until a real
     * vehicle-start API is confirmed for the target VIN.
     */
    fun parse(payload: String): XiaodiCommand? {
        if (payload == "on") return XiaodiCommand.PowerOnRequested
        if (payload == "off") return XiaodiCommand.PowerOffRequested

        // PROVISIONAL parser for messages seen in some Bemfa air-con docs.
        // Do not wire to vehicle control before observing real Xiaomi messages.
        val parts = payload.split('#')
        if (parts.size !in 3..4 || parts[0] != "on") return null
        if (parts[1] !in listOf("1", "2", "3", "4")) return null
        val temperature = parts[2].toIntOrNull() ?: return null
        if (temperature !in 16..30) return null
        if (parts.size == 4 && !Regex("[0-9]").matches(parts[3])) return null
        return XiaodiCommand.AirConditionerSettings(temperature)
    }
}
