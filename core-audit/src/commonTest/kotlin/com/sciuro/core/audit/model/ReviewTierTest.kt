package com.sciuro.core.audit.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewTierTest {

    @Test
    fun `auto tiers are auto reviewed`() {
        assertTrue(ReviewTier.AUTO_SILENT.isAutoReviewed)
        assertTrue(ReviewTier.AUTO_UNDO.isAutoReviewed)
    }

    @Test
    fun `manual and untrusted tiers require manual review`() {
        assertFalse(ReviewTier.MANUAL.isAutoReviewed)
        assertFalse(ReviewTier.UNTRUSTED.isAutoReviewed)
    }
}
