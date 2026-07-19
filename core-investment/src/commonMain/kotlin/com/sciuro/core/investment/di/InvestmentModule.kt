package com.sciuro.core.investment.di

import com.sciuro.core.investment.engine.InvestmentEngine
import com.sciuro.core.investment.repository.InvestmentRepository
import org.koin.dsl.module

val investmentModule = module {
    single { InvestmentRepository(get(), get()) }
    single { InvestmentEngine(get(), get()) }
}
