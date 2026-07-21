package com.sciuro.feature.dashboard.di

import com.sciuro.feature.dashboard.viewmodel.DashboardViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dashboardModule = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get()) }
}
