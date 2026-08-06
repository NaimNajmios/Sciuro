package com.sciuro.feature.settings.di

import com.sciuro.feature.settings.viewmodel.DataSettingsViewModel
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import com.sciuro.feature.settings.viewmodel.IntelligenceSettingsViewModel
import com.sciuro.feature.settings.viewmodel.InvestmentPriceViewModel
import com.sciuro.feature.settings.viewmodel.LinkedAccountsViewModel
import com.sciuro.feature.settings.viewmodel.MerchantRulesViewModel
import com.sciuro.feature.settings.viewmodel.NotificationSettingsViewModel
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { NotificationSettingsViewModel(get(), get()) }
    viewModel { DataSettingsViewModel(get()) }
    viewModel { IntelligenceSettingsViewModel(get(), get()) }
    viewModel { DeveloperSettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { LinkedAccountsViewModel(get()) }
    viewModel { MerchantRulesViewModel(get(), get()) }
    viewModel { InvestmentPriceViewModel(get(), get(), get()) }
}
