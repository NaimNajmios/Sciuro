package com.sciuro.core.obligations.di

import com.sciuro.core.obligations.engine.IncomeRecurrencePatternDetector
import com.sciuro.core.obligations.engine.ObligationCycleMatcher
import com.sciuro.core.obligations.engine.ObligationDetectionEngine
import com.sciuro.core.obligations.repository.ObligationRepository
import org.koin.dsl.module

val obligationsModule = module {
    single { ObligationRepository(get(), get()) }
    single { ObligationDetectionEngine(get(), get(), get(), get()) }
    single { ObligationCycleMatcher(get(), get(), get()) }
    single { IncomeRecurrencePatternDetector(get(), get()) }
}
