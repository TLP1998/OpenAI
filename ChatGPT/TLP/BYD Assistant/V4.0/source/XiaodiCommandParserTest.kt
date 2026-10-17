package dev.vehiclebridge.byd.bridge

import org.junit.Assert.*
import org.junit.Test

class XiaodiCommandParserTest {
    @Test fun recognizedSwitchCommands() {
        assertEquals(XiaodiCommand.PowerOnRequested, XiaodiCommandParser.parse("on"))
        assertEquals(XiaodiCommand.PowerOffRequested, XiaodiCommandParser.parse("off"))
    }

    @Test fun recognizedTemperatureOnlyWithFullFrame() {
        assertEquals(XiaodiCommand.AirConditionerSettings(26), XiaodiCommandParser.parse("on#2#26"))
        assertNull(XiaodiCommandParser.parse("26"))
        assertNull(XiaodiCommandParser.parse("on#2#26#foo"))
    }

    @Test fun rejectUnsafeAndMalformedPayload() {
        for (input in listOf("", "ON", "Off", " unlock", "unlock", "off#2#26", "on#2#100",
                "on#2#-1", "on#2#x", "on#2#26#11", "on#2#26#1#extra", "on#9#26"))
            assertNull("Expected reject: $input", XiaodiCommandParser.parse(input))
    }
}
