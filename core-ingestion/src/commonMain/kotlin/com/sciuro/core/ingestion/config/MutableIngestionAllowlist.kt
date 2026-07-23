package com.sciuro.core.ingestion.config

import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MutableIngestionAllowlist(
    private val settingsProvider: SettingsProvider
) {
    val directBankPackages: Set<String> = IngestionDefaults.directBankPackages
    val aggregatorPackages: Set<String> = IngestionDefaults.aggregatorPackages
    val defaultAllowedPackages: Set<String> = IngestionDefaults.defaultAllowedPackages

    private val _effectivePackages = MutableStateFlow(computeEffective())
    val effectivePackages: StateFlow<Set<String>> = _effectivePackages.asStateFlow()

    fun allows(packageName: String): Boolean = packageName in _effectivePackages.value

    fun isDefaultBankPackage(packageName: String): Boolean = packageName in directBankPackages

    fun isDefaultAggregatorPackage(packageName: String): Boolean = packageName in aggregatorPackages

    fun isDefaultPackage(packageName: String): Boolean = packageName in defaultAllowedPackages

    fun isUserAddedPackage(packageName: String): Boolean =
        packageName in settingsProvider.getIngestionAllowlistAdditions()

    fun addPackage(packageName: String) {
        val additions = settingsProvider.getIngestionAllowlistAdditions().toMutableSet()
        val removals = settingsProvider.getIngestionAllowlistRemovals().toMutableSet()
        removals.remove(packageName)
        additions.add(packageName)
        settingsProvider.setIngestionAllowlistAdditions(additions)
        settingsProvider.setIngestionAllowlistRemovals(removals)
        _effectivePackages.value = computeEffective()
    }

    fun removePackage(packageName: String) {
        val additions = settingsProvider.getIngestionAllowlistAdditions().toMutableSet()
        val removals = settingsProvider.getIngestionAllowlistRemovals().toMutableSet()
        additions.remove(packageName)
        removals.add(packageName)
        settingsProvider.setIngestionAllowlistAdditions(additions)
        settingsProvider.setIngestionAllowlistRemovals(removals)
        _effectivePackages.value = computeEffective()
    }

    private fun computeEffective(): Set<String> {
        val defaults = defaultAllowedPackages
        val additions = settingsProvider.getIngestionAllowlistAdditions()
        val removals = settingsProvider.getIngestionAllowlistRemovals()
        return (defaults + additions) - removals
    }
}
