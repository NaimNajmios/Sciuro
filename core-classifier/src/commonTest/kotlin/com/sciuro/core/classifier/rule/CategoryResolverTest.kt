package com.sciuro.core.classifier.rule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryResolverTest {

    @Test
    fun `guessFromStaticHeuristic returns cat_dining for restaurant merchants`() {
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("Starbucks"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("McDonalds"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("KFC"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("Burger King"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("Tealive"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("Warung Pak Ali"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_groceries for grocery merchants`() {
        assertEquals("cat_groceries", CategoryResolver.guessFromStaticHeuristic("Jaya Grocer"))
        assertEquals("cat_groceries", CategoryResolver.guessFromStaticHeuristic("Speedmart"))
        assertEquals("cat_groceries", CategoryResolver.guessFromStaticHeuristic("Mydin"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_transport for Grab`() {
        assertEquals("cat_transport", CategoryResolver.guessFromStaticHeuristic("Grab"))
        assertEquals("cat_transport", CategoryResolver.guessFromStaticHeuristic("GrabPay"))
        assertEquals("cat_transport", CategoryResolver.guessFromStaticHeuristic("GRAB FOOD"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_utilities for TNB`() {
        assertEquals("cat_utilities", CategoryResolver.guessFromStaticHeuristic("Tenaga Nasional"))
    }

    @Test
    fun `guessFromStaticHeuristic is case insensitive`() {
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("starbucks"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("STARBUCKS"))
        assertEquals("cat_dining", CategoryResolver.guessFromStaticHeuristic("StArBuCkS"))
    }

    @Test
    fun `guessFromStaticHeuristic returns null for unknown merchant`() {
        assertNull(CategoryResolver.guessFromStaticHeuristic("Unknown Vendor"))
        assertNull(CategoryResolver.guessFromStaticHeuristic(""))
    }

    @Test
    fun `guessFromStaticHeuristic returns null for null-merchant-like strings`() {
        assertNull(CategoryResolver.guessFromStaticHeuristic("airasia"))
        assertNull(CategoryResolver.guessFromStaticHeuristic("netflix"))
    }
}
