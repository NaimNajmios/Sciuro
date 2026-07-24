package com.sciuro.core.classifier.di

import com.sciuro.core.classifier.orchestrator.SciuroIngestionOrchestrator
import com.sciuro.core.classifier.rule.CategoryResolver
import com.sciuro.core.classifier.rule.ReviewTierDecider
import com.sciuro.core.classifier.rule.RuleLearner
import com.sciuro.core.ledger.config.SettingsProvider
import org.koin.dsl.module

val classifierModule = module {
    single { RuleLearner(get(), get()) }
    single { CategoryResolver(get()) }
    single {
        val settings = get<SettingsProvider>()
        ReviewTierDecider(
            database = get(),
            silentConfidenceThreshold = settings.getSilentAutoConfirmThreshold(),
            autoConfidenceThreshold = 0.7f,
            autoConfirmEnabled = settings.isTransactionAutoConfirmEnabled()
        )
    }
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
            categoryResolver = get(),
            bnplRiskDetector = get(),
            reviewTierDecider = get(),
            tracer = get()
        ) 
    }
}
