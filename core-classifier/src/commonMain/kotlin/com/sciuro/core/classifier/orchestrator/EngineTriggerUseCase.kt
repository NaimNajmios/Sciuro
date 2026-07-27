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

class EngineTriggerUseCase(
    private val transferDetectionEngine: TransferDetectionEngine,
    private val obligationCycleMatcher: ObligationCycleMatcher,
    private val budgetEngine: BudgetEngine,
    private val debtEngine: DebtEngine,
    private val investmentEngine: InvestmentEngine,
    private val obligationDetectionEngine: ObligationDetectionEngine,
    private val bnplRiskDetector: BnplRiskDetector,
    private val tracer: PipelineTracer
) {
    companion object {
        private const val ENGINE_DEBOUNCE_MS = 15_000L
    }

    private var lastBudgetRunMs = 0L
    private var lastDebtRunMs = 0L
    private var lastInvestmentRunMs = 0L
    private var lastObligationRunMs = 0L

    suspend fun triggerAll(
        transaction: Transaction,
        draft: StructuredDraft,
        rawEventId: String
    ) {
        transferDetectionEngine.onTransactionBooked(
            newTxId = transaction.id,
            newTxAccountId = transaction.accountId,
            newTxAmount = transaction.amount,
            newTxDirection = transaction.direction,
            newTxTimestamp = transaction.timestamp,
            counterpartyAccountNumber = draft.counterpartyAccountNumber
        )
        tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
            detail = mapOf("engine" to "transfer"))

        obligationCycleMatcher.onTransactionBooked(
            transactionId = transaction.id,
            amount = transaction.amount,
            direction = transaction.direction,
            categoryId = transaction.categoryId,
            merchant = transaction.merchant
        )
        tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
            detail = mapOf("engine" to "obligation_cycle"))

        val now = currentTimeMillis()

        if (now - lastBudgetRunMs > ENGINE_DEBOUNCE_MS) {
            budgetEngine.processBudgets()
            lastBudgetRunMs = now
            tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "budget"))
        }

        if (now - lastDebtRunMs > ENGINE_DEBOUNCE_MS) {
            debtEngine.processDebtPayments()
            lastDebtRunMs = now
            tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "debt"))
        }

        if (now - lastInvestmentRunMs > ENGINE_DEBOUNCE_MS) {
            investmentEngine.processInvestments()
            lastInvestmentRunMs = now
            tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "investment"))
        }

        if (now - lastObligationRunMs > ENGINE_DEBOUNCE_MS) {
            obligationDetectionEngine.runDetection()
            lastObligationRunMs = now
            tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "obligation_detect"))
        }

        bnplRiskDetector.evaluate()
        tracer.trace(rawEventId, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
            detail = mapOf("engine" to "bnpl"))
    }
}
