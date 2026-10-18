package dev.vehiclebridge.byd.bridge

/**
 * Pure decision engine for opt-in Bluetooth automation. Decisions require
 * EXACT saved bonded-device MAC match and ignore duplicate events.
 * The external caller must check Android BLUETOOTH_CONNECT permission,
 * a verified vehicle/cloud session, VIN binding, and command completion.
 *
 * Bluetooth events are NOT authoritative evidence of a locked door.
 */
internal enum class BluetoothAction { UNLOCK, LOCK }
internal data class BluetoothOptions(
    val enabledUnlockOnConnect: Boolean = false,
    val enabledLockOnDisconnect: Boolean = false,
    val enabledPossibleUnlockedReminder: Boolean = false,
    val selectedDeviceAddress: String = "",
    val configuredDeviceName: String = "秦Plus",
    val reminderMinutes: Int = 5
)
internal class BluetoothActionDecider(
    private val now: () -> Long = System::currentTimeMillis
) {
    private var lastDeviceConnected: Boolean? = null
    private var lastCommandAt = Long.MIN_VALUE
    fun evaluate(address: String?, connected: Boolean, options: BluetoothOptions): BluetoothAction? {
        if (options.selectedDeviceAddress.isBlank() ||
            !options.selectedDeviceAddress.equals(address, ignoreCase=true)) return null
        if (lastDeviceConnected == connected) return null
        lastDeviceConnected = connected
        if (lastCommandAt != Long.MIN_VALUE && now()-lastCommandAt < 10_000L) return null
        val action = if (connected && options.enabledUnlockOnConnect) BluetoothAction.UNLOCK
            else if (!connected && options.enabledLockOnDisconnect) BluetoothAction.LOCK else null
        if (action != null) lastCommandAt=now()
        return action
    }
}

/** Reminder model is deliberately conservative:
 * "may be unlocked" ≠ verified unlocked.
 * A reminder must not say "vehicle definitely not locked" without an
 * actual, freshly read cloud door-lock status.
 */
internal class PossibleUnlockedReminder {
    private var observedUnlockAt: Long? = null
    fun onUnlockRequest(timeMs: Long) { observedUnlockAt = timeMs }
    fun onConfirmedLockReceipt() { observedUnlockAt = null }
    fun shouldWarn(timeMs: Long, options: BluetoothOptions): Boolean =
        options.enabledPossibleUnlockedReminder &&
            observedUnlockAt?.let { timeMs-it >= options.reminderMinutes*60_000L } == true
}
