package com.sciuro.core.ingestion.config

import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.ledger.config.SettingsProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeSettingsProvider : SettingsProvider {
    private var additions = mutableSetOf<String>()
    private var removals = mutableSetOf<String>()

    override fun getIngestionAllowlistAdditions(): Set<String> = additions.toSet()
    override fun setIngestionAllowlistAdditions(packages: Set<String>) { additions = packages.toMutableSet() }
    override fun getIngestionAllowlistRemovals(): Set<String> = removals.toSet()
    override fun setIngestionAllowlistRemovals(packages: Set<String>) { removals = packages.toMutableSet() }

    override fun getLlmModelName(): String = ""
    override fun setLlmModelName(name: String) {}
    override fun getQuickLabels(): List<String> = emptyList()
    override fun setQuickLabels(labels: List<String>) {}
    override fun getBudgetWarningThreshold(): Float = 0.8f
    override fun setBudgetWarningThreshold(threshold: Float) {}
    override fun isLlmEnabled(): Boolean = true
    override fun setLlmEnabled(enabled: Boolean) {}
    override fun getApiKey(): String? = null
    override fun setApiKey(apiKey: String) {}
    override fun isLockEnabled(): Boolean = false
    override fun setLockEnabled(enabled: Boolean) {}
}

class MutableIngestionAllowlistTest {

    private val settings = FakeSettingsProvider()

    @Test
    fun `effective allowlist contains all defaults when no additions or removals`() {
        val allowlist = MutableIngestionAllowlist(settings)
        val effective = allowlist.effectivePackages.value
        assertEquals(IngestionDefaults.defaultAllowedPackages, effective)
        assertTrue(effective.contains("com.cimbmalaysia"))
        assertTrue(effective.contains("my.com.tngdigital.ewallet"))
        assertTrue(effective.contains("com.google.android.gm"))
    }

    @Test
    fun `addPackage adds to effective allowlist`() {
        val allowlist = MutableIngestionAllowlist(settings)
        allowlist.addPackage("com.example.wallet")
        assertTrue(allowlist.allows("com.example.wallet"))
        assertTrue(allowlist.isUserAddedPackage("com.example.wallet"))
        assertTrue(settings.getIngestionAllowlistAdditions().contains("com.example.wallet"))
    }

    @Test
    fun `removePackage removes from effective allowlist`() {
        val allowlist = MutableIngestionAllowlist(settings)
        allowlist.removePackage("com.cimbmalaysia")
        assertFalse(allowlist.allows("com.cimbmalaysia"))
        assertTrue(settings.getIngestionAllowlistRemovals().contains("com.cimbmalaysia"))
    }

    @Test
    fun `addPackage then removePackage results in empty diff`() {
        val allowlist = MutableIngestionAllowlist(settings)
        allowlist.addPackage("com.example.wallet")
        allowlist.removePackage("com.example.wallet")
        assertFalse(allowlist.allows("com.example.wallet"))
        assertFalse(allowlist.isUserAddedPackage("com.example.wallet"))
    }

    @Test
    fun `remove then add restores default`() {
        val allowlist = MutableIngestionAllowlist(settings)
        allowlist.removePackage("com.cimbmalaysia")
        assertFalse(allowlist.allows("com.cimbmalaysia"))
        allowlist.addPackage("com.cimbmalaysia")
        assertTrue(allowlist.allows("com.cimbmalaysia"))
    }

    @Test
    fun `isDefaultBankPackage returns true for known banks`() {
        val allowlist = MutableIngestionAllowlist(settings)
        assertTrue(allowlist.isDefaultBankPackage("com.cimbmalaysia"))
        assertTrue(allowlist.isDefaultBankPackage("my.com.tngdigital.ewallet"))
        assertFalse(allowlist.isDefaultBankPackage("com.google.android.gm"))
        assertFalse(allowlist.isDefaultBankPackage("com.unknown.app"))
    }

    @Test
    fun `isDefaultAggregatorPackage returns true for aggregators`() {
        val allowlist = MutableIngestionAllowlist(settings)
        assertTrue(allowlist.isDefaultAggregatorPackage("com.google.android.gm"))
        assertTrue(allowlist.isDefaultAggregatorPackage("com.microsoft.office.outlook"))
        assertFalse(allowlist.isDefaultAggregatorPackage("com.cimbmalaysia"))
    }

    @Test
    fun `addPackage deduplicates removals`() {
        val allowlist = MutableIngestionAllowlist(settings)
        allowlist.removePackage("com.cimbmalaysia")
        allowlist.addPackage("com.cimbmalaysia")
        assertFalse(settings.getIngestionAllowlistRemovals().contains("com.cimbmalaysia"))
    }

    @Test
    fun `removePackage deduplicates additions`() {
        val allowlist = MutableIngestionAllowlist(settings)
        allowlist.addPackage("com.example.wallet")
        allowlist.removePackage("com.example.wallet")
        assertFalse(settings.getIngestionAllowlistAdditions().contains("com.example.wallet"))
    }
}
