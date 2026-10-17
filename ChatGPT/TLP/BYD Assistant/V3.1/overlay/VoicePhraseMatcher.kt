package dev.vehiclebridge.byd

import java.util.Locale

/** Exact, normalized matching prevents "解锁" being misinterpreted as "锁车". */
enum class VoiceTask { LOCK, UNLOCK, LOCK_CLOSEAIR }

object VoicePhraseMatcher {
    val defaults: Map<VoiceTask, String> = linkedMapOf(
        VoiceTask.LOCK to "小笛锁车",
        VoiceTask.UNLOCK to "小笛解锁",
        VoiceTask.LOCK_CLOSEAIR to "小笛锁车熄火"
    )

    fun normalize(value: String): String =
        value.lowercase(Locale.CHINA).filter { it.isLetterOrDigit() }

    fun distinctValid(phrases: Map<VoiceTask, String>): Boolean {
        if (VoiceTask.entries.any { task ->
                val phrase = phrases[task] ?: return false
                val canonical = normalize(phrase)
                canonical.length !in 2..24
            }) return false
        return phrases.values.map(::normalize).toSet().size == VoiceTask.entries.size
    }

    fun match(recognized: String, phrases: Map<VoiceTask, String>): VoiceTask? {
        val canonical = normalize(recognized)
        if (canonical.isBlank()) return null
        return VoiceTask.entries.singleOrNull { normalize(phrases[it] ?: "") == canonical }
    }
}
