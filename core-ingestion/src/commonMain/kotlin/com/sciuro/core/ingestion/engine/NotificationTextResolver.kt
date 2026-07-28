package com.sciuro.core.ingestion.engine

object NotificationTextResolver {
    val KNOWN_CONTENT_KEYS = mapOf(
        "com.maybank2u.life" to "full_desc"
    )

    val EXCLUDED_KEYS = setOf(
        "android.title",
        "soundName",
        "tranVerCode",
        "start_date",
        "android.reduced.images",
        "android.chronometerCountDown",
        "android.subText",
        "android.summaryText",
        "android.showChronometer"
    )

    fun resolveTextFallback(shortText: String, bigText: String, textLines: String): String =
        bigText.ifBlank { textLines.ifBlank { shortText } }

    fun resolveCustomExtrasFallback(
        packageName: String,
        extras: Map<String, String>
    ): String {
        KNOWN_CONTENT_KEYS[packageName]?.let { key ->
            extras[key]?.let { if (it.isNotBlank()) return it }
        }
        return extras.entries
            .filter { (key, value) ->
                key !in EXCLUDED_KEYS &&
                    !key.startsWith("android.") &&
                    value.isNotBlank() &&
                    AggregatorHeuristicFilter.isFinancial("", value)
            }
            .maxByOrNull { (_, value) -> value.length }
            ?.value ?: ""
    }
}
