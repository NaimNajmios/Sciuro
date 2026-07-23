package com.sciuro.core.debt.di

import com.sciuro.core.debt.engine.DebtEngine
import com.sciuro.core.debt.repository.DebtRepository
import org.koin.dsl.module

val debtModule = module {
    single { DebtRepository(get(), get()) }
    single { DebtEngine(get(), get(), get()) }
}
