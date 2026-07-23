package com.sciuro.core.transfer.di

import com.sciuro.core.transfer.engine.TransferDetectionEngine
import com.sciuro.core.transfer.repository.TransferRepository
import org.koin.dsl.module

val transferModule = module {
    single { TransferRepository(get(), get(), get()) }
    single { TransferDetectionEngine(get(), get(), get()) }
}
