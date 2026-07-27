package com.sciuro.core.classifier.di

import com.sciuro.core.classifier.orchestrator.EngineTriggerUseCase
import com.sciuro.core.classifier.orchestrator.SciuroIngestionOrchestrator
import com.sciuro.core.classifier.orchestrator.TransactionBookingUseCase
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
    single<TransactionBookingUseCase> {
        com.sciuro.core.classifier.orchestrator.DefaultTransactionBookingUseCase(
            parserPipeline = get(),
            transactionRepository = get(),
            accountRepository = get(),
            rawEventRepository = get(),
            categoryResolver = get(),
            reviewTierDecider = get(),
            tracer = get(),
            confidenceThreshold = com.sciuro.core.parsing.model.DEFAULT_CONFIDENCE_THRESHOLD
        )
    }
    single<EngineTriggerUseCase> {
        com.sciuro.core.classifier.orchestrator.DefaultEngineTriggerUseCase(
            transferDetectionEngine = get(),
            obligationCycleMatcher = get(),
            budgetEngine = get(),
            debtEngine = get(),
            investmentEngine = get(),
            obligationDetectionEngine = get(),
            bnplRiskDetector = get(),
            tracer = get()
        )
    }
    single {
        SciuroIngestionOrchestrator(
            ingestionSource = get(),
            rawEventRepository = get(),
            bookingUseCase = get(),
            engineTriggerUseCase = get(),
            tracer = get()
        )
    }
}
