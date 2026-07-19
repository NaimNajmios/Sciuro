package com.sciuro.core.ingestion.di

import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import org.koin.dsl.module

val ingestionModule = module {
    single { NotificationSourceAdapter() }
}
