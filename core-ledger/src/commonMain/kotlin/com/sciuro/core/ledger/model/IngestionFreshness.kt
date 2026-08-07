package com.sciuro.core.ledger.model

enum class CaptureFreshness {
    FRESH,
    STALE,
    OFF
}

object IngestionFreshness {
    const val FRESH_WINDOW_MS = 5L * 60L * 1000L
    const val STALE_WINDOW_MS = 24L * 60L * 60L * 1000L

    fun freshness(isListenerEnabled: Boolean, lastCapturedAt: Long?, now: Long): CaptureFreshness {
        if (!isListenerEnabled) return CaptureFreshness.OFF
        if (lastCapturedAt == null) return CaptureFreshness.OFF
        return when {
            now - lastCapturedAt <= FRESH_WINDOW_MS -> CaptureFreshness.FRESH
            now - lastCapturedAt <= STALE_WINDOW_MS -> CaptureFreshness.STALE
            else -> CaptureFreshness.OFF
        }
    }
}
