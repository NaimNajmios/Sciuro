package com.sciuro.core.investment.di

import com.sciuro.core.investment.price.PriceProvider
import com.sciuro.core.investment.price.YahooFinancePriceProvider
import org.koin.dsl.module

val platformInvestmentModule = module {
    single<PriceProvider> { YahooFinancePriceProvider(get()) }
}
