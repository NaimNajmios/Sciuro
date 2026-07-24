package com.sciuro.core.classifier.rule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryResolverTest {

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_1 for restaurant merchants`() {
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Starbucks"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("McDonalds"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("KFC"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Burger King"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Tealive"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Warung Pak Ali"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_6 for grocery merchants`() {
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Jaya Grocer"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Speedmart"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Mydin"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_2 for Grab`() {
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Grab"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("GrabPay"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("GRAB FOOD"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_3 for TNB`() {
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Tenaga Nasional"))
    }

    @Test
    fun `guessFromStaticHeuristic is case insensitive`() {
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("starbucks"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("STARBUCKS"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("StArBuCkS"))
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
