package dev.vehiclebridge.byd.bridge

import org.junit.Assert.*
import org.junit.Test

class XiaodiCommandParserTest {
    @Test fun basicPowerCommands() {
        assertEquals(XiaodiCommand.PowerOnRequested, XiaodiCommandParser.parse("on"))
        assertEquals(XiaodiCommand.PowerOffRequested, XiaodiCommandParser.parse("off"))
    }
    @Test fun actualSixFieldAirConPayload() {
        assertEquals(XiaodiCommand.AirConditionerSettings(26),
            XiaodiCommandParser.parse("on#1#26#0#0#0"))
        assertEquals(XiaodiCommand.AirConditionerSettings(23),
            XiaodiCommandParser.parse("on#1#23#0#0#0"))
        assertEquals(XiaodiCommand.AirConditionerSettings(31),
            XiaodiCommandParser.parse("on#1#31#0#0#0"))
    }
    @Test fun badMessagesIgnored() {
        for (s in listOf("", "ON", "off#1#26#0#0#0", "on#2#26#0#0#0",
                "on#1#32#0#0#0", "on#1#14#0#0#0", "on#1#026#0#0#0",
                "on#1#x#0#0#0", "on#1#26#0#0", "on#1#26#0#0#1",
                "unlock", "on#1#26#0#0#0#0")) assertNull(s, XiaodiCommandParser.parse(s))
    }
}
