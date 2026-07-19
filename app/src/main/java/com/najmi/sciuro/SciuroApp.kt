package com.najmi.sciuro

import android.app.Application
import com.sciuro.core.ledger.di.ledgerModule
import com.sciuro.core.ledger.di.databaseModule
import com.sciuro.core.ledger.di.platformDatabaseModule
import com.sciuro.core.budget.di.budgetModule
import com.sciuro.core.debt.di.debtModule
import com.sciuro.core.investment.di.investmentModule
import com.sciuro.feature.dashboard.di.dashboardModule
import com.sciuro.feature.wallet.di.walletModule
import com.sciuro.feature.kanban.di.kanbanModule
import com.sciuro.feature.budgets.di.budgetsModule
import com.sciuro.core.parsing.di.parsingModule
import com.sciuro.core.classifier.di.classifierModule
import com.sciuro.core.ingestion.di.ingestionModule
import com.sciuro.core.parsing.config.SettingsProvider
import com.najmi.sciuro.config.EncryptedSettingsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single<SettingsProvider> { EncryptedSettingsProvider(get()) }
}

class SciuroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@SciuroApp)
            modules(
                platformDatabaseModule,
                databaseModule,
                ledgerModule,
                budgetModule,
                debtModule,
                investmentModule,
                dashboardModule,
                walletModule,
                kanbanModule,
                budgetsModule,
                appModule,
                parsingModule,
                classifierModule,
                ingestionModule
            )
        }
    }
}
