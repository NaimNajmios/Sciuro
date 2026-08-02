package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.budget.engine.BudgetEngine
import com.sciuro.core.debt.engine.BnplRiskDetector
import com.sciuro.core.debt.engine.DebtEngine
import com.sciuro.core.investment.engine.InvestmentEngine
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.obligations.engine.ObligationCycleMatcher
import com.sciuro.core.obligations.engine.ObligationDetectionEngine
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.transfer.engine.TransferDetectionEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

interface EngineTriggerUseCase {
    suspend fun triggerAll(transaction: Transaction, draft: StructuredDraft, rawEventId: String)
}

open class DefaultEngineTriggerUseCase(
    private val transferDetectionEngine: TransferDetectionEngine,
    private val obligationCycleMatcher: ObligationCycleMatcher,
    private val budgetEngine: BudgetEngine,
    private val debtEngine: DebtEngine,
    private val investmentEngine: InvestmentEngine,
    private val obligationDetectionEngine: ObligationDetectionEngine,
    private val bnplRiskDetector: BnplRiskDetector,
    private val tracer: PipelineTracer,
    private val settingsProvider: SettingsProvider
) : EngineTriggerUseCase {
    companion object {
        private const val ENGINE_DEBOUNCE_MS = 15_000L
    }

    private val debounceMutex = Mutex()
    private val engineMutexes = mutableMapOf<String, Mutex>()

    private fun engineMutex(name: String): Mutex = engineMutexes.getOrPut(name) { Mutex() }

    private fun loadLastRunMs(engineName: String): Long {
        return settingsProvider.getEngineLastRunMs(engineName)
    }

    private fun persistLastRunMs(engineName: String, timestampMs: Long) {
        settingsProvider.setEngineLastRunMs(engineName, timestampMs)
    }

    private suspend fun shouldRun(engineName: String, now: Long): Boolean {
        debounceMutex.withLock {
            val lastRunMs = loadLastRunMs(engineName)
            if (now - lastRunMs > ENGINE_DEBOUNCE_MS) {
                persistLastRunMs(engineName, now)
                return true
            }
            return false
        }
    }

    override suspend fun triggerAll(
        transaction: Transaction,
        draft: StructuredDraft,
        rawEventId: String
    ) {
        val now = currentTimeMillis()

        if (shouldRun("transfer", now)) {
            engineMutex("transfer").withLock {
                runEngine(rawEventId, transaction.id, "transfer") {
                    transferDetectionEngine.onTransactionBooked(
                        newTxId = transaction.id,
                        newTxAccountId = transaction.accountId,
                        newTxAmount = transaction.amount,
                        newTxDirection = transaction.direction,
                        newTxTimestamp = transaction.timestamp,
                        counterpartyAccountNumber = draft.counterpartyAccountNumber
                    )
                }
            }
        }

        if (shouldRun("obligation_cycle", now)) {
            engineMutex("obligation_cycle").withLock {
                runEngine(rawEventId, transaction.id, "obligation_cycle") {
                    obligationCycleMatcher.onTransactionBooked(
                        transactionId = transaction.id,
                        amount = transaction.amount,
                        direction = transaction.direction,
                        categoryId = transaction.categoryId,
                        merchant = transaction.merchant
                    )
                }
            }
        }

        if (shouldRun("budget", now)) {
            engineMutex("budget").withLock {
                runEngine(rawEventId, transaction.id, "budget") {
                    budgetEngine.processBudgets()
                }
            }
        }

        if (shouldRun("debt", now)) {
            engineMutex("debt").withLock {
                runEngine(rawEventId, transaction.id, "debt") {
                    debtEngine.processDebtPayments()
                }
            }
        }

        if (shouldRun("investment", now)) {
            engineMutex("investment").withLock {
                runEngine(rawEventId, transaction.id, "investment") {
                    investmentEngine.processInvestments()
                }
            }
        }

        if (shouldRun("obligation_detect", now)) {
            engineMutex("obligation_detect").withLock {
                runEngine(rawEventId, transaction.id, "obligation_detect") {
                    obligationDetectionEngine.runDetection()
                }
            }
        }

        if (shouldRun("bnpl", now)) {
            engineMutex("bnpl").withLock {
                runEngine(rawEventId, transaction.id, "bnpl") {
                    bnplRiskDetector.evaluate()
                }
            }
        }
    }

    private suspend fun runEngine(rawEventId: String, transactionId: String?, name: String, block: suspend () -> Unit) {
        try {
            block()
            tracer.trace(rawEventId, transactionId, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to name))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            tracer.trace(rawEventId, transactionId, TraceStage.ENGINE, TraceOutcome.FAILURE,
                detail = mapOf("engine" to name, "error" to (e.message ?: "")))
        }
    }
}
