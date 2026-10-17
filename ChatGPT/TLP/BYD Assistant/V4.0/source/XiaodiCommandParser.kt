package dev.vehiclebridge.byd.bridge

/** Messages observed from the user's Bemfa cloud air-conditioner topic:
 * on#1#26#0#0#0 = set AC to 26°C.
 * Run duration is deliberately NOT part of the MQTT frame; app fixes 30 min.
 */
internal sealed interface XiaodiCommand {
    data object PowerOnRequested : XiaodiCommand
    data object PowerOffRequested : XiaodiCommand
    data class AirConditionerSettings(val temperatureCelsius: Int) : XiaodiCommand
}

internal object XiaodiCommandParser {
    fun parse(payload: String): XiaodiCommand? {
        if (payload == "on") return XiaodiCommand.PowerOnRequested
        if (payload == "off") return XiaodiCommand.PowerOffRequested
        val fields = payload.split('#')
        // Other AC device modes/fields are unknown and MUST be ignored.
        if (fields.size != 6 || fields[0] != "on" || fields[1] != "1" ||
            fields.drop(3).any { it != "0" }) return null
        val celsius = fields[2].toIntOrNull() ?: return null
        if (celsius !in 15..31 || celsius.toString() != fields[2]) return null
        return XiaodiCommand.AirConditionerSettings(celsius)
    }
}
