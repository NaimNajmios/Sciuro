package com.sciuro.core.classifier.di

import com.sciuro.core.classifier.orchestrator.SciuroIngestionOrchestrator
import com.sciuro.core.classifier.rule.CategoryResolver
import com.sciuro.core.classifier.rule.RuleLearner
import org.koin.dsl.module

val classifierModule = module {
    single { RuleLearner(get(), get()) }
    single { CategoryResolver(get()) }
    single { 
        SciuroIngestionOrchestrator(
            ingestionSource = get(),
            parserPipeline = get(),
            transactionRepository = get(),
            accountRepository = get(),
            rawEventRepository = get(),
            transferDetectionEngine = get(),
            obligationCycleMatcher = get(),
            budgetEngine = get(),
            debtEngine = get(),
            investmentEngine = get(),
            obligationDetectionEngine = get(),
            categoryResolver = get()
        ) 
    }
}
