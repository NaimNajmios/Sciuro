package com.sciuro.core.parsing.util

import com.sciuro.core.ingestion.model.RawEvent

fun matchesAggregatorForward(
    event: RawEvent,
    aggregatorPackages: Set<String>,
    bankSubjectMarkers: List<String>
): Boolean {
    if (event.sourcePackageOrAddress !in aggregatorPackages) return false
    val combined = "${event.title} ${event.text}".lowercase()
    return bankSubjectMarkers.any { combined.contains(it.lowercase()) }
}
