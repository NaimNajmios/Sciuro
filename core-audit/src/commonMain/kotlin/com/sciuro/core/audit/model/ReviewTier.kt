package com.sciuro.core.audit.model

enum class ReviewTier(val label: String) {
    MANUAL("manual"),
    AUTO_SILENT("auto_silent"),
    AUTO_UNDO("auto_undo")
}
