package dev.vehiclebridge.byd.bridge

import dev.vehiclebridge.byd.protocol.DoorAction
import org.junit.Assert.*
import org.junit.Test

class ClimateCommandGateTest {
    @Test fun bareOnUnlocksButTemperatureNeverUnlocks() {
        var now=1_000_000_000L
        val gate=ClimateCommandGate("xiaodi005"){now}
        gate.ready(true)
        val bare=gate.accept("xiaodi005","on".toByteArray(),false,false)
        assertEquals(DoorAction.UNLOCK,bare?.action)
        assertNull(bare?.temperatureCelsius)
        gate.release()
        now+=4_000_000_000L
        val ac=gate.accept("xiaodi005","on#1#26#0#0#0".toByteArray(),false,false)
        assertEquals(DoorAction.CLIMATE,ac?.action)
        assertEquals(26,ac?.temperatureCelsius)
        gate.release()
        now+=4_000_000_000L
        val close=gate.accept("xiaodi005","off".toByteArray(),false,false)
        assertEquals(DoorAction.LOCK,close?.action)
    }
    @Test fun rejectRetainedDuplicateWrongTopicAndUnknownFrames() {
        val gate=ClimateCommandGate("xiaodi005")
        gate.ready(true)
        assertNull(gate.accept("xiaodi005","on".toByteArray(),true,false))
        assertNull(gate.accept("xiaodi005","on".toByteArray(),false,true))
        assertNull(gate.accept("other005","on".toByteArray(),false,false))
        assertNull(gate.accept("xiaodi005","on#1#26#1#0#0".toByteArray(),false,false))
    }
}
