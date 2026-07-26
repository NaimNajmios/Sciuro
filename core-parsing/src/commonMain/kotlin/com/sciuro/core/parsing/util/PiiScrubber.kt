package com.sciuro.core.parsing.util

object PiiScrubber {
    // Matches 7-16 consecutive digits (common for account numbers and phone numbers)
    private val consecutiveDigitsRegex = """(?<!\d)\d{7,16}(?!\d)""".toRegex()
    
    // Matches NRIC format (YYMMDD-PB-####)
    private val nricRegex = """(?<!\d)\d{6}-\d{2}-\d{4}(?!\d)""".toRegex()

    fun scrub(text: String): String {
        var scrubbed = text

        // Redact NRICs
        scrubbed = scrubbed.replace(nricRegex) { matchResult ->
            val value = matchResult.value
            "*".repeat(value.length - 4) + value.takeLast(4)
        }

        // Redact account numbers / phone numbers
        scrubbed = scrubbed.replace(consecutiveDigitsRegex) { matchResult ->
            val value = matchResult.value
            "*".repeat(value.length - 4) + value.takeLast(4)
        }

        return scrubbed
    }
}
