package com.sciuro.core.audit.events

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.UUID

object DomainEventCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun eventTypeOf(event: DomainEvent): String = when (event) {
        is DomainEvent.DebtBalanceUpdated -> "DebtBalanceUpdated"
        is DomainEvent.DebtFullyPaidOff -> "DebtFullyPaidOff"
        is DomainEvent.ObligationCycleSettled -> "ObligationCycleSettled"
        is DomainEvent.ObligationCreated -> "ObligationCreated"
        is DomainEvent.BudgetThresholdCrossed -> "BudgetThresholdCrossed"
        is DomainEvent.TransactionCategorized -> "TransactionCategorized"
        is DomainEvent.TransactionRecategorized -> "TransactionRecategorized"
        is DomainEvent.TransferMatched -> "TransferMatched"
        is DomainEvent.TransferUnmatchedFlagged -> "TransferUnmatchedFlagged"
        is DomainEvent.CashCredited -> "CashCredited"
        is DomainEvent.CashDebited -> "CashDebited"
        is DomainEvent.CashRecounted -> "CashRecounted"
        is DomainEvent.RecurringObligationProposed -> "RecurringObligationProposed"
        is DomainEvent.RecurringObligationConfirmed -> "RecurringObligationConfirmed"
        is DomainEvent.ObligationAmountDrifted -> "ObligationAmountDrifted"
        is DomainEvent.BnplRiskThresholdCrossed -> "BnplRiskThresholdCrossed"
        is DomainEvent.BudgetLimitSuggested -> "BudgetLimitSuggested"
        is DomainEvent.InvestmentTransactionRecorded -> "InvestmentTransactionRecorded"
        is DomainEvent.InvestmentPriceRefreshed -> "InvestmentPriceRefreshed"
        is DomainEvent.IncomeRecurrencePatternDetected -> "IncomeRecurrencePatternDetected"
        is DomainEvent.NewFinanceAppDetected -> "NewFinanceAppDetected"
        is DomainEvent.MerchantRuleLearned -> "MerchantRuleLearned"
        is DomainEvent.RecipientRuleLearned -> "RecipientRuleLearned"
        is DomainEvent.MerchantAccountRuleLearned -> "MerchantAccountRuleLearned"
        is DomainEvent.TransactionModified -> "TransactionModified"
        is DomainEvent.TransactionRejected -> "TransactionRejected"
        is DomainEvent.NetPositionMilestoneReached -> "NetPositionMilestoneReached"
    }

    fun isCritical(eventType: String): Boolean =
        eventType == "DebtFullyPaidOff" || eventType == "NetPositionMilestoneReached"

    fun newEventId(): String = UUID.randomUUID().toString()

    fun serialize(event: DomainEvent): String {
        val map = mutableMapOf<String, String>()
        when (event) {
            is DomainEvent.DebtBalanceUpdated -> {
                map["debtId"] = event.debtId
                map["newBalance"] = event.newBalance.toString()
                map["method"] = event.method
            }
            is DomainEvent.DebtFullyPaidOff -> map["debtId"] = event.debtId
            is DomainEvent.ObligationCycleSettled -> {
                map["obligationId"] = event.obligationId
                map["transactionId"] = event.transactionId
            }
            is DomainEvent.ObligationCreated -> map["obligationId"] = event.obligationId
            is DomainEvent.BudgetThresholdCrossed -> {
                map["categoryId"] = event.categoryId
                map["percentUsed"] = event.percentUsed.toString()
            }
            is DomainEvent.TransactionCategorized -> {
                map["transactionId"] = event.transactionId
                map["categoryId"] = event.categoryId
                map["confidence"] = event.confidence.toString()
                map["source"] = event.source
                event.merchant?.let { map["merchant"] = it }
            }
            is DomainEvent.TransactionRecategorized -> {
                map["transactionId"] = event.transactionId
                map["oldCategoryId"] = event.oldCategoryId
                map["newCategoryId"] = event.newCategoryId
                event.merchant?.let { map["merchant"] = it }
                event.accountId?.let { map["accountId"] = it }
            }
            is DomainEvent.TransferMatched -> {
                map["transferLinkId"] = event.transferLinkId
                map["sourceTxId"] = event.sourceTxId
                map["destTxId"] = event.destTxId
                map["matchMethod"] = event.matchMethod
            }
            is DomainEvent.TransferUnmatchedFlagged -> {
                map["transactionId"] = event.transactionId
                map["candidateRecipient"] = event.candidateRecipient
            }
            is DomainEvent.CashCredited -> {
                map["cashAccountId"] = event.cashAccountId
                map["amount"] = event.amount.toString()
                map["sourceEvent"] = event.sourceEvent
            }
            is DomainEvent.CashDebited -> {
                map["cashAccountId"] = event.cashAccountId
                map["amount"] = event.amount.toString()
                map["sourceEvent"] = event.sourceEvent
            }
            is DomainEvent.CashRecounted -> {
                map["adjustmentId"] = event.adjustmentId
                map["variance"] = event.variance.toString()
                map["adjustmentType"] = event.adjustmentType
            }
            is DomainEvent.RecurringObligationProposed -> {
                map["obligationId"] = event.obligationId
                map["confidence"] = event.confidence.toString()
            }
            is DomainEvent.RecurringObligationConfirmed -> map["obligationId"] = event.obligationId
            is DomainEvent.ObligationAmountDrifted -> {
                map["obligationId"] = event.obligationId
                map["oldAmount"] = event.oldAmount.toString()
                map["newAmount"] = event.newAmount.toString()
            }
            is DomainEvent.BnplRiskThresholdCrossed -> {
                map["activeBnplCount"] = event.activeBnplCount.toString()
            }
            is DomainEvent.BudgetLimitSuggested -> {
                map["categoryId"] = event.categoryId
                map["suggestedAmount"] = event.suggestedAmount.toString()
            }
            is DomainEvent.InvestmentTransactionRecorded -> {
                map["accountId"] = event.accountId
                map["action"] = event.action
                map["unitAmount"] = event.unitAmount.toString()
            }
            is DomainEvent.InvestmentPriceRefreshed -> {
                map["accountId"] = event.accountId
                map["newPricePerUnit"] = event.newPricePerUnit.toString()
            }
            is DomainEvent.IncomeRecurrencePatternDetected -> {
                map["incomeStreamId"] = event.incomeStreamId
                map["expectedNextDate"] = event.expectedNextDate.toString()
                map["amount"] = event.amount.toString()
            }
            is DomainEvent.NewFinanceAppDetected -> map["packageName"] = event.packageName
            is DomainEvent.MerchantRuleLearned -> {
                map["merchant"] = event.merchant
                map["categoryId"] = event.categoryId
            }
            is DomainEvent.RecipientRuleLearned -> {
                map["accountRef"] = event.accountRef
                map["classification"] = event.classification
            }
            is DomainEvent.MerchantAccountRuleLearned -> {
                map["merchant"] = event.merchant
                map["accountId"] = event.accountId
            }
            is DomainEvent.TransactionModified -> map["transactionId"] = event.transactionId
            is DomainEvent.TransactionRejected -> {
                map["transactionId"] = event.transactionId
                event.merchant?.let { map["merchant"] = it }
                map["amount"] = event.amount.toString()
                map["direction"] = event.direction
            }
            is DomainEvent.NetPositionMilestoneReached -> {
                map["netWorth"] = event.netWorth.toString()
                map["milestone"] = event.milestone.toString()
            }
        }
        return json.encodeToString(JsonObject.serializer(), map.toJsonObject())
    }

    fun deserialize(eventType: String, payloadJson: String): DomainEvent? {
        val map = json.parseToJsonElement(payloadJson).jsonObject
        return when (eventType) {
            "DebtBalanceUpdated" -> DomainEvent.DebtBalanceUpdated(
                debtId = map["debtId"]!!.jsonPrimitive.content,
                newBalance = map["newBalance"]!!.jsonPrimitive.double,
                method = map["method"]!!.jsonPrimitive.content
            )
            "DebtFullyPaidOff" -> DomainEvent.DebtFullyPaidOff(
                debtId = map["debtId"]!!.jsonPrimitive.content
            )
            "ObligationCycleSettled" -> DomainEvent.ObligationCycleSettled(
                obligationId = map["obligationId"]!!.jsonPrimitive.content,
                transactionId = map["transactionId"]!!.jsonPrimitive.content
            )
            "ObligationCreated" -> DomainEvent.ObligationCreated(
                obligationId = map["obligationId"]!!.jsonPrimitive.content
            )
            "BudgetThresholdCrossed" -> DomainEvent.BudgetThresholdCrossed(
                categoryId = map["categoryId"]!!.jsonPrimitive.content,
                percentUsed = map["percentUsed"]!!.jsonPrimitive.double
            )
            "TransactionCategorized" -> DomainEvent.TransactionCategorized(
                transactionId = map["transactionId"]!!.jsonPrimitive.content,
                categoryId = map["categoryId"]!!.jsonPrimitive.content,
                confidence = map["confidence"]!!.jsonPrimitive.double,
                source = map["source"]!!.jsonPrimitive.content,
                merchant = map["merchant"]?.jsonPrimitive?.content
            )
            "TransactionRecategorized" -> DomainEvent.TransactionRecategorized(
                transactionId = map["transactionId"]!!.jsonPrimitive.content,
                oldCategoryId = map["oldCategoryId"]!!.jsonPrimitive.content,
                newCategoryId = map["newCategoryId"]!!.jsonPrimitive.content,
                merchant = map["merchant"]?.jsonPrimitive?.content,
                accountId = map["accountId"]?.jsonPrimitive?.content
            )
            "TransferMatched" -> DomainEvent.TransferMatched(
                transferLinkId = map["transferLinkId"]!!.jsonPrimitive.content,
                sourceTxId = map["sourceTxId"]!!.jsonPrimitive.content,
                destTxId = map["destTxId"]!!.jsonPrimitive.content,
                matchMethod = map["matchMethod"]!!.jsonPrimitive.content
            )
            "TransferUnmatchedFlagged" -> DomainEvent.TransferUnmatchedFlagged(
                transactionId = map["transactionId"]!!.jsonPrimitive.content,
                candidateRecipient = map["candidateRecipient"]!!.jsonPrimitive.content
            )
            "CashCredited" -> DomainEvent.CashCredited(
                cashAccountId = map["cashAccountId"]!!.jsonPrimitive.content,
                amount = map["amount"]!!.jsonPrimitive.double,
                sourceEvent = map["sourceEvent"]!!.jsonPrimitive.content
            )
            "CashDebited" -> DomainEvent.CashDebited(
                cashAccountId = map["cashAccountId"]!!.jsonPrimitive.content,
                amount = map["amount"]!!.jsonPrimitive.double,
                sourceEvent = map["sourceEvent"]!!.jsonPrimitive.content
            )
            "CashRecounted" -> DomainEvent.CashRecounted(
                adjustmentId = map["adjustmentId"]!!.jsonPrimitive.content,
                variance = map["variance"]!!.jsonPrimitive.double,
                adjustmentType = map["adjustmentType"]!!.jsonPrimitive.content
            )
            "RecurringObligationProposed" -> DomainEvent.RecurringObligationProposed(
                obligationId = map["obligationId"]!!.jsonPrimitive.content,
                confidence = map["confidence"]!!.jsonPrimitive.double
            )
            "RecurringObligationConfirmed" -> DomainEvent.RecurringObligationConfirmed(
                obligationId = map["obligationId"]!!.jsonPrimitive.content
            )
            "ObligationAmountDrifted" -> DomainEvent.ObligationAmountDrifted(
                obligationId = map["obligationId"]!!.jsonPrimitive.content,
                oldAmount = map["oldAmount"]!!.jsonPrimitive.double,
                newAmount = map["newAmount"]!!.jsonPrimitive.double
            )
            "BnplRiskThresholdCrossed" -> DomainEvent.BnplRiskThresholdCrossed(
                activeBnplCount = map["activeBnplCount"]!!.jsonPrimitive.int
            )
            "BudgetLimitSuggested" -> DomainEvent.BudgetLimitSuggested(
                categoryId = map["categoryId"]!!.jsonPrimitive.content,
                suggestedAmount = map["suggestedAmount"]!!.jsonPrimitive.double
            )
            "InvestmentTransactionRecorded" -> DomainEvent.InvestmentTransactionRecorded(
                accountId = map["accountId"]!!.jsonPrimitive.content,
                action = map["action"]!!.jsonPrimitive.content,
                unitAmount = map["unitAmount"]!!.jsonPrimitive.double
            )
            "InvestmentPriceRefreshed" -> DomainEvent.InvestmentPriceRefreshed(
                accountId = map["accountId"]!!.jsonPrimitive.content,
                newPricePerUnit = map["newPricePerUnit"]!!.jsonPrimitive.double
            )
            "IncomeRecurrencePatternDetected" -> DomainEvent.IncomeRecurrencePatternDetected(
                incomeStreamId = map["incomeStreamId"]!!.jsonPrimitive.content,
                expectedNextDate = map["expectedNextDate"]!!.jsonPrimitive.long,
                amount = map["amount"]!!.jsonPrimitive.double
            )
            "NewFinanceAppDetected" -> DomainEvent.NewFinanceAppDetected(
                packageName = map["packageName"]!!.jsonPrimitive.content
            )
            "MerchantRuleLearned" -> DomainEvent.MerchantRuleLearned(
                merchant = map["merchant"]!!.jsonPrimitive.content,
                categoryId = map["categoryId"]!!.jsonPrimitive.content
            )
            "RecipientRuleLearned" -> DomainEvent.RecipientRuleLearned(
                accountRef = map["accountRef"]!!.jsonPrimitive.content,
                classification = map["classification"]!!.jsonPrimitive.content
            )
            "MerchantAccountRuleLearned" -> DomainEvent.MerchantAccountRuleLearned(
                merchant = map["merchant"]!!.jsonPrimitive.content,
                accountId = map["accountId"]!!.jsonPrimitive.content
            )
            "TransactionModified" -> DomainEvent.TransactionModified(
                transactionId = map["transactionId"]!!.jsonPrimitive.content
            )
            "TransactionRejected" -> DomainEvent.TransactionRejected(
                transactionId = map["transactionId"]!!.jsonPrimitive.content,
                merchant = map["merchant"]?.jsonPrimitive?.content,
                amount = map["amount"]!!.jsonPrimitive.double,
                direction = map["direction"]!!.jsonPrimitive.content
            )
            "NetPositionMilestoneReached" -> DomainEvent.NetPositionMilestoneReached(
                netWorth = map["netWorth"]!!.jsonPrimitive.double,
                milestone = map["milestone"]!!.jsonPrimitive.double
            )
            else -> null
        }
    }

    private fun Map<String, String>.toJsonObject(): JsonObject =
        JsonObject(this.mapValues { (_, v) -> JsonPrimitive(v) })
}
