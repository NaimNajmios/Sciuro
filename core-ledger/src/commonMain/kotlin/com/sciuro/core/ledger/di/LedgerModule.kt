package com.sciuro.core.ledger.di

import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.subscriber.NetPositionSubscriber
import com.sciuro.core.ledger.audit.SqlDelightAuditRepository
import org.koin.dsl.module

val ledgerModule = module {
    single<AuditRepository> { SqlDelightAuditRepository(get()) }
    single { DomainEventBus() }
    single { NetPositionSubscriber(get(), get()) }
    single { com.sciuro.core.ledger.repository.AccountRepository(get(), get()) }
    single { com.sciuro.core.ledger.repository.CategoryRepository(get(), get()) }
    single { com.sciuro.core.ledger.repository.TransactionRepository(get(), get(), get(), get()) }
    single { com.sciuro.core.ledger.repository.CashAdjustmentRepository(get(), get(), get(), get()) }
    single { com.sciuro.core.ledger.engine.ReconciliationEngine(get(), get(), get()) }
    single { com.sciuro.core.ledger.repository.RawEventRepository(get()) }
}
