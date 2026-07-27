package com.sciuro.core.budget.di

import com.sciuro.core.budget.engine.BudgetEngine
import com.sciuro.core.budget.engine.BudgetLimitSuggester
import com.sciuro.core.budget.engine.BudgetReconciler
import com.sciuro.core.budget.repository.BudgetRepository
import org.koin.dsl.module

val budgetModule = module {
    single { BudgetRepository(get(), get()) }
    single { BudgetEngine(get(), get(), get()) }
    single { BudgetLimitSuggester(get(), get()) }
    single { BudgetReconciler(get(), get()) }
}
