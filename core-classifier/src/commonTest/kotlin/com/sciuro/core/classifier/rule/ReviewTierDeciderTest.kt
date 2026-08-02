package com.sciuro.core.classifier.rule

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithReturn
import app.cash.sqldelight.TransactionWithoutReturn
import com.sciuro.core.audit.model.ReviewTier
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewTierDeciderTest {

    private val fakeDb = object : SciuroDatabase {
        override val accountQueries get() = throw UnsupportedOperationException()
        override val auditLogQueries get() = throw UnsupportedOperationException()
        override val budgetQueries get() = throw UnsupportedOperationException()
        override val cashAdjustmentQueries get() = throw UnsupportedOperationException()
        override val categoryQueries get() = throw UnsupportedOperationException()
        override val debtQueries get() = throw UnsupportedOperationException()
        override val debtPaymentLinkQueries get() = throw UnsupportedOperationException()
        override val investmentQueries get() = throw UnsupportedOperationException()
        override val merchantAccountRuleQueries get() = throw UnsupportedOperationException()
        override val merchantCategoryRuleQueries get() = throw UnsupportedOperationException()
        override val obligationQueries get() = throw UnsupportedOperationException()
        override val pipelineTraceQueries get() = throw UnsupportedOperationException()
        override val rawEventStagingQueries get() = throw UnsupportedOperationException()
        override val transactionCorroborationQueries get() = throw UnsupportedOperationException()
        override val transactionRecordQueries get() = throw UnsupportedOperationException()
        override val transferLinkQueries get() = throw UnsupportedOperationException()
        override val domainEventLogQueries get() = throw UnsupportedOperationException()
        override val domainEventDeliveryQueries get() = throw UnsupportedOperationException()
        override fun transaction(noEnclosing: Boolean, body: TransactionWithoutReturn.() -> Unit) {
            body.invoke(object : TransactionWithoutReturn {
                override fun afterCommit(function: () -> Unit) {}
                override fun afterRollback(function: () -> Unit) {}
                override fun rollback(): Nothing { throw Exception("rollback") }
                override fun transaction(body: TransactionWithoutReturn.() -> Unit) { body.invoke(this) }
            })
        }

        override fun <R> transactionWithResult(noEnclosing: Boolean, bodyWithReturn: TransactionWithReturn<R>.() -> R): R {
            return bodyWithReturn.invoke(object : TransactionWithReturn<R> {
                override fun afterCommit(function: () -> Unit) {}
                override fun afterRollback(function: () -> Unit) {}
                override fun rollback(returnValue: R): Nothing { throw Exception("rollback") }
                override fun <R2> transaction(body: TransactionWithReturn<R2>.() -> R2): R2 { throw Exception("not implemented") }
            })
        }
    }

    private lateinit var decider: ReviewTierDecider

    @BeforeTest
    fun setUp() {
        decider = ReviewTierDecider(
            database = fakeDb,
            silentConfidenceThreshold = 0.95f,
            autoConfidenceThreshold = 0.7f,
            autoConfirmEnabled = true
        )
    }

    @Test
    fun `decide returns AUTO_SILENT when confidence equals 1`() = runBlocking {
        val tier = decider.decide(confidence = 1.0f, categoryId = "cat_exp_1", accountId = "acc_1", merchant = "Starbucks")
        assertEquals(ReviewTier.AUTO_SILENT, tier)
    }

    @Test
    fun `decide returns AUTO_SILENT when confidence equals 1 without category`() = runBlocking {
        val tier = decider.decide(confidence = 1.0f, categoryId = null, accountId = "acc_1", merchant = "Starbucks")
        assertEquals(ReviewTier.AUTO_SILENT, tier)
    }

    @Test
    fun `decide returns AUTO_SILENT when confidence equals 1 without account`() = runBlocking {
        val tier = decider.decide(confidence = 1.0f, categoryId = "cat_exp_1", accountId = null, merchant = "Starbucks")
        assertEquals(ReviewTier.AUTO_SILENT, tier)
    }

    @Test
    fun `decide returns MANUAL when auto confirm is disabled`() = runBlocking {
        val manualDecider = ReviewTierDecider(database = fakeDb, autoConfirmEnabled = false)
        val tier = manualDecider.decide(confidence = 0.85f, categoryId = "cat_exp_1", accountId = "acc_1", merchant = "Anywhere")
        assertEquals(ReviewTier.MANUAL, tier)
    }

    @Test
    fun `decide returns AUTO_UNDO when confidence 0_7 with both category and account`() = runBlocking {
        val tier = decider.decide(confidence = 0.7f, categoryId = "cat_exp_1", accountId = "acc_1", merchant = "NewMerchant")
        assertEquals(ReviewTier.AUTO_UNDO, tier)
    }

    @Test
    fun `decide returns AUTO_UNDO when confidence above threshold with both category and account`() = runBlocking {
        val tier = decider.decide(confidence = 0.85f, categoryId = "cat_exp_1", accountId = "acc_1", merchant = "SomePlace")
        assertEquals(ReviewTier.AUTO_UNDO, tier)
    }

    @Test
    fun `decide returns MANUAL when confidence below threshold`() = runBlocking {
        val tier = decider.decide(confidence = 0.5f, categoryId = "cat_exp_1", accountId = "acc_1", merchant = "LowConfMerchant")
        assertEquals(ReviewTier.MANUAL, tier)
    }

    @Test
    fun `decide returns MANUAL when confidence above threshold but no category`() = runBlocking {
        val tier = decider.decide(confidence = 0.85f, categoryId = null, accountId = "acc_1", merchant = "NoCatMerchant")
        assertEquals(ReviewTier.MANUAL, tier)
    }

    @Test
    fun `decide returns MANUAL when confidence above threshold but no account`() = runBlocking {
        val tier = decider.decide(confidence = 0.85f, categoryId = "cat_exp_1", accountId = null, merchant = "NoAccMerchant")
        assertEquals(ReviewTier.MANUAL, tier)
    }

    @Test
    fun `decide returns AUTO_SILENT when confidence 1_0 even with auto confirm disabled`() = runBlocking {
        val manualDecider = ReviewTierDecider(database = fakeDb, autoConfirmEnabled = false)
        val tier = manualDecider.decide(confidence = 1.0f, categoryId = "cat_exp_1", accountId = "acc_1", merchant = "Anything")
        assertEquals(ReviewTier.AUTO_SILENT, tier)
    }
}
