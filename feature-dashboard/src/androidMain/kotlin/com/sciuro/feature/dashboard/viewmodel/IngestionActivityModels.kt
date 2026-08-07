package com.sciuro.feature.dashboard.viewmodel

import com.sciuro.core.ledger.model.CaptureFreshness
import com.sciuro.core.ledger.model.IngestionFreshness

enum class CaptureHealth {
    HEALTHY,
    STALE,
    OFF
}

data class CaptureStatus(
    val health: CaptureHealth,
    val lastCapturedAt: Long?,
    val isListenerEnabled: Boolean
) {
    companion object {
        fun compute(
            isListenerEnabled: Boolean,
            lastCapturedAt: Long?,
            now: Long
        ): CaptureStatus {
            val health = when (IngestionFreshness.freshness(isListenerEnabled, lastCapturedAt, now)) {
                CaptureFreshness.FRESH -> CaptureHealth.HEALTHY
                CaptureFreshness.STALE -> CaptureHealth.STALE
                CaptureFreshness.OFF -> CaptureHealth.OFF
            }
            return CaptureStatus(health = health, lastCapturedAt = lastCapturedAt, isListenerEnabled = isListenerEnabled)
        }
    }
}

enum class ActivityLogStatus {
    PARSED,
    NEEDS_REVIEW,
    DROPPED
}

data class IngestionActivityEntry(
    val rawEventId: String,
    val sourceType: String,
    val sourcePackageOrAddress: String,
    val timestamp: Long,
    val status: ActivityLogStatus,
    val reason: String?
)

data class ParseFailureAlert(
    val rawEventId: String,
    val sourceType: String,
    val sourcePackageOrAddress: String,
    val capturedAt: Long
)

fun String?.toActivityLogStatus(): ActivityLogStatus = when (this) {
    "PARSED" -> ActivityLogStatus.PARSED
    "NEEDS_REVIEW" -> ActivityLogStatus.NEEDS_REVIEW
    else -> ActivityLogStatus.DROPPED
}
