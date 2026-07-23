package com.sciuro.feature.settings.di

import com.sciuro.feature.settings.viewmodel.LinkedAccountsViewModel
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { LinkedAccountsViewModel(get()) }
}
