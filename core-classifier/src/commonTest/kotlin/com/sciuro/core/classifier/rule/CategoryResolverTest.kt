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
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Domino Pizza"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Pizza Hut"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Subway"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Texas Chicken"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Marrybrown"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("OldTown White Coffee"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Secret Recipe"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Restoran ABC"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("Mamak Stall"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_6 for grocery merchants`() {
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Jaya Grocer"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Speedmart"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Mydin"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Lotus"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Aeon Big"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Village Grocer"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("NSK Grocer"))
        assertEquals("cat_exp_6", CategoryResolver.guessFromStaticHeuristic("Pasar Malam"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_2 for transport merchants`() {
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Grab"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("GrabPay"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("GRAB FOOD"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Petronas"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Shell"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Caltex"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Toll PLUS"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Parking"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("Touch n Go"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_3 for utility merchants`() {
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Tenaga Nasional"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("TNB"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Air Selangor"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Indah Water"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Unifi"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Celcom"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Maxis"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("Digi"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_4 for shopping merchants`() {
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("Shopee"))
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("Lazada"))
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("Uniqlo"))
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("IKEA"))
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("Mr DIY"))
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("Guardian"))
        assertEquals("cat_exp_4", CategoryResolver.guessFromStaticHeuristic("Popular Bookstore"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_5 for entertainment merchants`() {
        assertEquals("cat_exp_5", CategoryResolver.guessFromStaticHeuristic("Netflix"))
        assertEquals("cat_exp_5", CategoryResolver.guessFromStaticHeuristic("Spotify"))
        assertEquals("cat_exp_5", CategoryResolver.guessFromStaticHeuristic("Disney Plus"))
        assertEquals("cat_exp_5", CategoryResolver.guessFromStaticHeuristic("Steam"))
        assertEquals("cat_exp_5", CategoryResolver.guessFromStaticHeuristic("Cinema TGV"))
        assertEquals("cat_exp_5", CategoryResolver.guessFromStaticHeuristic("Apple Subscription"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_7 for health merchants`() {
        assertEquals("cat_exp_7", CategoryResolver.guessFromStaticHeuristic("Klinik Kesihatan"))
        assertEquals("cat_exp_7", CategoryResolver.guessFromStaticHeuristic("Hospital Pantai"))
        assertEquals("cat_exp_7", CategoryResolver.guessFromStaticHeuristic("Farmasi"))
        assertEquals("cat_exp_7", CategoryResolver.guessFromStaticHeuristic("Dental Clinic"))
        assertEquals("cat_exp_7", CategoryResolver.guessFromStaticHeuristic("Gym Fitness"))
    }

    @Test
    fun `guessFromStaticHeuristic returns cat_exp_8 for education merchants`() {
        assertEquals("cat_exp_8", CategoryResolver.guessFromStaticHeuristic("Tuition Centre"))
        assertEquals("cat_exp_8", CategoryResolver.guessFromStaticHeuristic("University Malaya"))
        assertEquals("cat_exp_8", CategoryResolver.guessFromStaticHeuristic("College"))
        assertEquals("cat_exp_8", CategoryResolver.guessFromStaticHeuristic("Coursera"))
        assertEquals("cat_exp_8", CategoryResolver.guessFromStaticHeuristic("Udemy"))
    }

    @Test
    fun `guessFromStaticHeuristic is case insensitive`() {
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("starbucks"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("STARBUCKS"))
        assertEquals("cat_exp_1", CategoryResolver.guessFromStaticHeuristic("StArBuCkS"))
        assertEquals("cat_exp_2", CategoryResolver.guessFromStaticHeuristic("GRAB"))
        assertEquals("cat_exp_3", CategoryResolver.guessFromStaticHeuristic("tnb"))
    }

    @Test
    fun `guessFromStaticHeuristic returns null for unknown merchant`() {
        assertNull(CategoryResolver.guessFromStaticHeuristic("Unknown Vendor"))
        assertNull(CategoryResolver.guessFromStaticHeuristic(""))
    }

    @Test
    fun `guessFromStaticHeuristic returns null for non-merchant strings`() {
        assertNull(CategoryResolver.guessFromStaticHeuristic("airasia"))
        assertNull(CategoryResolver.guessFromStaticHeuristic("random string"))
    }
}
