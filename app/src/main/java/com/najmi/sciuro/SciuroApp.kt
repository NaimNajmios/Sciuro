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
import com.sciuro.core.transfer.di.transferModule
import com.sciuro.core.parsing.di.parsingModule
import com.sciuro.core.classifier.di.classifierModule
import com.sciuro.core.ingestion.di.ingestionModule
import com.sciuro.core.ledger.config.SettingsProvider
import com.najmi.sciuro.config.EncryptedSettingsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.sciuro.core.classifier.orchestrator.SciuroIngestionOrchestrator
import com.sciuro.core.classifier.rule.RuleLearner
import com.sciuro.core.ledger.subscriber.NetPositionSubscriber
import com.najmi.sciuro.worker.IngestionReconciliationWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

import com.sciuro.feature.debt.di.debtFeatureModule
import com.sciuro.core.obligations.di.obligationsModule
import com.sciuro.feature.settings.di.settingsModule

val appModule = module {
    single<SettingsProvider> { EncryptedSettingsProvider(get()) }
    single { com.najmi.sciuro.subscriber.FinanceAppSuggestionSubscriber(get(), get()) }
    single { com.najmi.sciuro.engine.NotificationSuppressionEngine(get(), get(), get(), get(), get(), get(), get()) }
}

class SciuroApp : Application(), KoinComponent {
    
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val orchestrator: SciuroIngestionOrchestrator by inject()
    private val ruleLearner: RuleLearner by inject()
    private val netPositionSubscriber: NetPositionSubscriber by inject()
    private val financeAppSuggestionSubscriber: com.najmi.sciuro.subscriber.FinanceAppSuggestionSubscriber by inject()
    private val notificationSuppressionEngine: com.najmi.sciuro.engine.NotificationSuppressionEngine by inject()
    
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
                debtFeatureModule,
                obligationsModule,
                settingsModule,
                appModule,
                parsingModule,
                classifierModule,
                ingestionModule,
                transferModule
            )
        }
        
        // Start the ingestion orchestrator to process raw events
        orchestrator.startListening(appScope)
        ruleLearner.start(appScope)
        netPositionSubscriber.start(appScope)
        financeAppSuggestionSubscriber.start()
        notificationSuppressionEngine.start()

        val reconciliationRequest = PeriodicWorkRequestBuilder<IngestionReconciliationWorker>(
            IngestionReconciliationWorker.REPEAT_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ingestion_reconciliation",
            ExistingPeriodicWorkPolicy.UPDATE,
            reconciliationRequest
        )
    }
}
