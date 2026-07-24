package com.sciuro.core.ingestion.di

import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.core.ingestion.source.MultiplexIngestionSource
import com.sciuro.core.ingestion.source.email.EmailSourceAdapter
import com.sciuro.core.ingestion.source.sms.SmsSourceAdapter
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import org.koin.dsl.module

val ingestionModule = module {
    single { MutableIngestionAllowlist(get()) }
    single { NotificationSourceAdapter() }
    single { SmsSourceAdapter() }
    single { EmailSourceAdapter() }
    single {
        MultiplexIngestionSource(
            adapters = listOf(
                get<NotificationSourceAdapter>(),
                get<SmsSourceAdapter>(),
                get<EmailSourceAdapter>()
            )
        )
    }
}
