package dev.vehiclebridge.byd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePhraseMatcherTest {
    @Test fun defaultPhrasesMatchExactly() {
        assertEquals(VoiceTask.LOCK, VoicePhraseMatcher.match("小笛锁车。", VoicePhraseMatcher.defaults))
        assertEquals(VoiceTask.UNLOCK, VoicePhraseMatcher.match("小笛解锁", VoicePhraseMatcher.defaults))
        assertEquals(VoiceTask.LOCK_CLOSEAIR, VoicePhraseMatcher.match("小笛锁车熄火！", VoicePhraseMatcher.defaults))
    }
    @Test fun overlappingPhrasesDoNotMisfire() {
        assertNull(VoicePhraseMatcher.match("小笛锁车熄火然后解锁", VoicePhraseMatcher.defaults))
        assertNull(VoicePhraseMatcher.match("小笛锁车熄火了吗", VoicePhraseMatcher.defaults))
        assertNull(VoicePhraseMatcher.match("请帮我小笛解锁", VoicePhraseMatcher.defaults))
    }
    @Test fun punctuationDoesNotMatterButCommandsStayDistinct() {
        assertEquals(VoiceTask.UNLOCK, VoicePhraseMatcher.match("小笛、解锁", VoicePhraseMatcher.defaults))
        assertFalse(VoicePhraseMatcher.distinctValid(mapOf(
            VoiceTask.LOCK to "小笛锁车",
            VoiceTask.UNLOCK to "小笛 锁车",
            VoiceTask.LOCK_CLOSEAIR to "小笛锁车熄火"
        )))
        assertTrue(VoicePhraseMatcher.distinctValid(VoicePhraseMatcher.defaults))
    }
}
