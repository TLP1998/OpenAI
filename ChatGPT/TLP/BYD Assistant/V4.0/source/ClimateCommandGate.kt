package dev.vehiclebridge.byd.bridge

import dev.vehiclebridge.byd.protocol.DoorAction
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal data class AcceptedClimateCommand(val action: DoorAction, val temperatureCelsius: Int? = null)

/** One topic, strict payload, no retained/duplicate delivery, no concurrent operations. */
internal class ClimateCommandGate(
    private val topic: String,
    private val clock: () -> Long = System::nanoTime
) {
    private var ready = false
    private var busy = false
    private var last: AcceptedClimateCommand? = null
    private var lastTime = 0L
    @Synchronized fun ready(value: Boolean) { ready = value }
    @Synchronized fun release() { busy = false }
    @Synchronized fun accept(actualTopic: String, payload: ByteArray, retained: Boolean,
                             duplicate: Boolean): AcceptedClimateCommand? {
        if (!ready || busy || retained || duplicate || actualTopic != topic ||
            payload.size !in 2..64) return null
        val value = try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(payload)).toString()
        } catch (_: Exception) { return null }
        val parsed = XiaodiCommandParser.parse(value) ?: return null
        val command = when (parsed) {
            XiaodiCommand.PowerOnRequested -> AcceptedClimateCommand(DoorAction.UNLOCK)
            XiaodiCommand.PowerOffRequested -> AcceptedClimateCommand(DoorAction.LOCK)
            is XiaodiCommand.AirConditionerSettings ->
                AcceptedClimateCommand(DoorAction.CLIMATE, parsed.temperatureCelsius)
        }
        val now = clock()
        if (command == last && now - lastTime < 3_000_000_000L) return null
        last = command
        lastTime = now
        busy = true
        return command
    }
}
