package com.najmi.sciuro.core.ui.components

private val keyValueRegex = """(\w[\w_]*)=([^,)]+)""".toRegex()

private fun parseState(state: String): Map<String, String> {
    return keyValueRegex.findAll(state).associate { match ->
        match.groupValues[1] to match.groupValues[2].trim().removeSurrounding("\"")
    }
}

fun formatAuditLogDetail(
    action: String,
    source: String,
    beforeState: String?,
    afterState: String?,
    categoryNames: Map<String, String> = emptyMap()
): String {
    return when (action) {
        "CREATE" -> formatCreate(source, afterState, categoryNames)
        "RECLASSIFY" -> formatReclassify(beforeState, afterState, categoryNames)
        "UPDATE" -> formatUpdate(beforeState, afterState, categoryNames)
        "DELETE" -> formatDelete(beforeState)
        else -> afterState ?: beforeState ?: ""
    }
}

private fun resolveCategory(categoryId: String?, names: Map<String, String>): String? {
    if (categoryId == null) return null
    return names[categoryId] ?: categoryId
        .removePrefix("cat_")
        .replaceFirstChar { it.uppercase() }
}

private fun formatCreate(
    source: String,
    afterState: String?,
    categoryNames: Map<String, String>
): String {
    val after = afterState ?: return ""
    val values = parseState(after)
    val merchant = values["merchant"] ?: "Unknown"
    val direction = when (values["direction"]) {
        "INFLOW" -> "received"
        "OUTFLOW" -> "paid"
        else -> "recorded"
    }
    val amount = values["amount"]?.toDoubleOrNull()
    val amountStr = if (amount != null) "RM ${"%.2f".format(amount)}" else ""
    val categoryStr = resolveCategory(values["categoryId"], categoryNames)

    return when (source) {
        "SYSTEM_AUTO" -> {
            val parts = mutableListOf("Auto-parsed")
            if (categoryStr != null) parts.add("as $categoryStr")
            if (merchant != "Unknown") parts.add("at $merchant")
            parts.add("($direction $amountStr)")
            parts.joinToString(" ")
        }
        "LLM_INFERRED" -> {
            val parts = mutableListOf("AI-assisted")
            if (categoryStr != null) parts.add("as $categoryStr")
            if (merchant != "Unknown") parts.add("at $merchant")
            parts.add("($direction $amountStr)")
            parts.joinToString(" ")
        }
        else -> "Manually entered: $merchant ($direction $amountStr)"
    }
}

private fun formatReclassify(
    beforeState: String?,
    afterState: String?,
    categoryNames: Map<String, String>
): String {
    val after = afterState ?: return ""
    val afterValues = parseState(after)
    val newCategoryId = afterValues["category_id"]
    val newCategoryStr = resolveCategory(newCategoryId, categoryNames)

    val before = beforeState ?: ""
    val beforeValues = parseState(before)
    val oldCategoryId = beforeValues["category_id"]
    val oldCategoryStr = resolveCategory(oldCategoryId, categoryNames)

    return if (oldCategoryStr != null && newCategoryStr != null && oldCategoryStr != newCategoryStr) {
        "Recategorized: $oldCategoryStr → $newCategoryStr"
    } else if (newCategoryStr != null) {
        "Recategorized to $newCategoryStr"
    } else {
        "Recategorized"
    }
}

private fun formatUpdate(
    beforeState: String?,
    afterState: String?,
    categoryNames: Map<String, String>
): String {
    val after = afterState ?: return ""

    val afterValues = parseState(after)

    if (afterValues["is_reviewed"] == "1" && afterValues.size <= 2) {
        return "Approved"
    }

    val beforeValues = beforeState?.let { parseState(it) } ?: emptyMap()
    val changes = mutableListOf<String>()

    for ((key, newVal) in afterValues) {
        if (key == "is_reviewed") continue
        val oldVal = beforeValues[key]
        val displayKey = when (key) {
            "category_id" -> "category"
            "amount" -> "amount"
            "merchant" -> "merchant"
            "account" -> "account"
            else -> key
        }
        val formattedNew = if (key == "amount") {
            "RM ${"%.2f".format(newVal.toDoubleOrNull() ?: 0.0)}"
        } else if (key == "category_id") {
            resolveCategory(newVal, categoryNames) ?: newVal
        } else newVal

        if (oldVal != null && oldVal != newVal) {
            val formattedOld = if (key == "amount") {
                "RM ${"%.2f".format(oldVal.toDoubleOrNull() ?: 0.0)}"
            } else if (key == "category_id") {
                resolveCategory(oldVal, categoryNames) ?: oldVal
            } else oldVal
            changes.add("$displayKey $formattedOld → $formattedNew")
        } else if (oldVal == null) {
            changes.add("$displayKey set to $formattedNew")
        }
    }

    return if (changes.isNotEmpty()) {
        "Edited: ${changes.joinToString(", ")}"
    } else {
        "Edited"
    }
}

private fun formatDelete(beforeState: String?): String {
    if (beforeState == "Reject Transaction") return "Rejected"
    val values = beforeState?.let { parseState(it) } ?: return "Deleted"
    val merchant = values["merchant"] ?: ""
    return if (merchant.isNotEmpty()) "Deleted: $merchant" else "Deleted"
}
