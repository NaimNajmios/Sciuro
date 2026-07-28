package com.sciuro.core.obligations.engine

import com.sciuro.core.obligations.model.ObligationFrequency

object FrequencyDetector {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val TOLERANCE = 0.3

    data class DetectionResult(
        val frequency: ObligationFrequency,
        val intervalMs: Long,
        val confidence: Double
    )

    fun detect(timestamps: List<Long>): DetectionResult? {
        if (timestamps.size < 3) return null

        val sorted = timestamps.sorted()
        val intervals = sorted.zipWithNext { a, b -> b - a }

        if (intervals.isEmpty()) return null

        val medianInterval = intervals.sorted().let { sorted_intervals ->
            val mid = sorted_intervals.size / 2
            if (sorted_intervals.size % 2 == 0) {
                (sorted_intervals[mid - 1] + sorted_intervals[mid]) / 2
            } else {
                sorted_intervals[mid]
            }
        }

        val frequency = classifyInterval(medianInterval) ?: return null

        val expectedMs = frequencyToMs(frequency)
        val matches = intervals.count { interval ->
            val ratio = interval.toDouble() / expectedMs
            ratio in (1.0 - TOLERANCE)..(1.0 + TOLERANCE)
        }
        val confidence = matches.toDouble() / intervals.size

        return DetectionResult(
            frequency = frequency,
            intervalMs = medianInterval,
            confidence = confidence
        )
    }

    private fun classifyInterval(intervalMs: Long): ObligationFrequency? {
        val days = intervalMs.toDouble() / DAY_MS
        return when {
            days < 10.0 -> ObligationFrequency.WEEKLY
            days < 18.0 -> ObligationFrequency.BIWEEKLY
            days < 45.0 -> ObligationFrequency.MONTHLY
            days < 120.0 -> ObligationFrequency.QUARTERLY
            days < 400.0 -> ObligationFrequency.YEARLY
            else -> null
        }
    }

    private fun frequencyToMs(frequency: ObligationFrequency): Long = when (frequency) {
        ObligationFrequency.WEEKLY -> 7L * DAY_MS
        ObligationFrequency.BIWEEKLY -> 14L * DAY_MS
        ObligationFrequency.MONTHLY -> 30L * DAY_MS
        ObligationFrequency.QUARTERLY -> 90L * DAY_MS
        ObligationFrequency.YEARLY -> 365L * DAY_MS
    }

    fun nextDueDate(lastTimestamp: Long, frequency: ObligationFrequency): Long {
        return lastTimestamp + frequencyToMs(frequency)
    }
}
