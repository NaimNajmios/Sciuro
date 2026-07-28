package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.budget.engine.BudgetEngine
import com.sciuro.core.debt.engine.BnplRiskDetector
import com.sciuro.core.debt.engine.DebtEngine
import com.sciuro.core.investment.engine.InvestmentEngine
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.obligations.engine.ObligationCycleMatcher
import com.sciuro.core.obligations.engine.ObligationDetectionEngine
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.transfer.engine.TransferDetectionEngine
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
    private val tracer: PipelineTracer
) : EngineTriggerUseCase {
    companion object {
        private const val ENGINE_DEBOUNCE_MS = 15_000L
    }

    private var lastTransferRunMs = 0L
    private var lastObligationCycleRunMs = 0L
    private var lastBudgetRunMs = 0L
    private var lastDebtRunMs = 0L
    private var lastInvestmentRunMs = 0L
    private var lastObligationRunMs = 0L
    private var lastBnplRunMs = 0L

    override suspend fun triggerAll(
        transaction: Transaction,
        draft: StructuredDraft,
        rawEventId: String
    ) {
        val now = currentTimeMillis()

        if (now - lastTransferRunMs > ENGINE_DEBOUNCE_MS) {
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
            lastTransferRunMs = now
        }

        if (now - lastObligationCycleRunMs > ENGINE_DEBOUNCE_MS) {
            runEngine(rawEventId, transaction.id, "obligation_cycle") {
                obligationCycleMatcher.onTransactionBooked(
                    transactionId = transaction.id,
                    amount = transaction.amount,
                    direction = transaction.direction,
                    categoryId = transaction.categoryId,
                    merchant = transaction.merchant
                )
            }
            lastObligationCycleRunMs = now
        }

        if (now - lastBudgetRunMs > ENGINE_DEBOUNCE_MS) {
            runEngine(rawEventId, transaction.id, "budget") {
                budgetEngine.processBudgets()
            }
            lastBudgetRunMs = now
        }

        if (now - lastDebtRunMs > ENGINE_DEBOUNCE_MS) {
            runEngine(rawEventId, transaction.id, "debt") {
                debtEngine.processDebtPayments()
            }
            lastDebtRunMs = now
        }

        if (now - lastInvestmentRunMs > ENGINE_DEBOUNCE_MS) {
            runEngine(rawEventId, transaction.id, "investment") {
                investmentEngine.processInvestments()
            }
            lastInvestmentRunMs = now
        }

        if (now - lastObligationRunMs > ENGINE_DEBOUNCE_MS) {
            runEngine(rawEventId, transaction.id, "obligation_detect") {
                obligationDetectionEngine.runDetection()
            }
            lastObligationRunMs = now
        }

        if (now - lastBnplRunMs > ENGINE_DEBOUNCE_MS) {
            runEngine(rawEventId, transaction.id, "bnpl") {
                bnplRiskDetector.evaluate()
            }
            lastBnplRunMs = now
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
