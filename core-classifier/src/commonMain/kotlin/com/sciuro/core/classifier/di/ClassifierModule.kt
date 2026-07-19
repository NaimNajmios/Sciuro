package com.sciuro.core.classifier.di

import com.sciuro.core.classifier.orchestrator.SciuroIngestionOrchestrator
import org.koin.dsl.module

val classifierModule = module {
    single { 
        SciuroIngestionOrchestrator(
            notificationSource = get(),
            parserPipeline = get(),
            transactionRepository = get(),
            accountRepository = get()
        ) 
    }
}
