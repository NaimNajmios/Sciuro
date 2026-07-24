package com.sciuro.feature.budgets.di

import com.sciuro.feature.budgets.viewmodel.BudgetsViewModel
import com.sciuro.feature.budgets.viewmodel.CategoryDrilldownViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val budgetsModule = module {
    viewModel { BudgetsViewModel(get(), get()) }
    viewModel { CategoryDrilldownViewModel(get(), get(), get()) }
}
