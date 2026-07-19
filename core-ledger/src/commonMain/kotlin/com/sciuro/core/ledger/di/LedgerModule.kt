package com.sciuro.core.ledger.di

import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.audit.SqlDelightAuditRepository
import org.koin.dsl.module

val ledgerModule = module {
    single<AuditRepository> { SqlDelightAuditRepository(get()) }
}
