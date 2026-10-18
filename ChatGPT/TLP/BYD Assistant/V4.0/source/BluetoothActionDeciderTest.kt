package dev.vehiclebridge.byd.bridge

import org.junit.Assert.*
import org.junit.Test

class BluetoothActionDeciderTest {
    @Test fun nothingHappensUnlessEnabledAndDeviceMatches() {
        val d=BluetoothActionDecider{100000L}
        val o=BluetoothOptions(selectedDeviceAddress="AA:BB:CC:DD:EE:FF",enabledUnlockOnConnect=true)
        assertNull(d.evaluate("00:00:00:00:00:00",true,o))
        assertEquals(BluetoothAction.UNLOCK,d.evaluate("AA:BB:CC:DD:EE:FF",true,o))
        assertNull(d.evaluate("AA:BB:CC:DD:EE:FF",true,o))
    }
    @Test fun independentSwitches() {
        var clock=100000L
        val d=BluetoothActionDecider{clock}
        val o=BluetoothOptions(selectedDeviceAddress="AA",enabledLockOnDisconnect=true)
        assertNull(d.evaluate("AA",true,o))
        clock+=20000
        assertEquals(BluetoothAction.LOCK,d.evaluate("AA",false,o))
    }
    @Test fun reminderDoesNotClaimCurrentLockStatus() {
        val reminder=PossibleUnlockedReminder()
        val o=BluetoothOptions(enabledPossibleUnlockedReminder=true,reminderMinutes=5)
        reminder.onUnlockRequest(1000)
        assertFalse(reminder.shouldWarn(1000+4*60000L,o))
        assertTrue(reminder.shouldWarn(1000+5*60000L,o))
        reminder.onConfirmedLockReceipt()
        assertFalse(reminder.shouldWarn(1000+6*60000L,o))
    }
}
