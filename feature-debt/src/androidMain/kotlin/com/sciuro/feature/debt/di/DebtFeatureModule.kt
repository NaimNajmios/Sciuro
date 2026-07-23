package com.sciuro.feature.debt.di

import com.sciuro.feature.debt.viewmodel.DebtViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val debtFeatureModule = module {
    viewModel { DebtViewModel(get()) }
}
