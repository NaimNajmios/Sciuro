package com.sciuro.feature.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.db.Budget_record
import com.sciuro.core.ledger.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import org.koin.java.KoinJavaComponent

class SciuroBudgetWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val budgetRepo = KoinJavaComponent.get<BudgetRepository>(BudgetRepository::class.java)
        val categoryRepo = KoinJavaComponent.get<CategoryRepository>(CategoryRepository::class.java)
        val budgets: List<Budget_record> = budgetRepo.observeBudgets().first()
        val categories = categoryRepo.observeCategories().first().associateBy { it.id }

        data class BudgetPct(val name: String, val pct: Float)

        val budgetPcts: List<BudgetPct> = budgets.map { budget ->
            val alloc = budget.allocated_amount
            val pct = if (alloc > 0.0) (budget.current_spent / alloc).toFloat() else 0f
            val name = categories[budget.category_id]?.name ?: budget.category_id
            BudgetPct(name, pct)
        }.sortedByDescending { it.pct }
        .take(3)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12)
            ) {
                Text(
                    text = "Budget Watch",
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(12))

                budgetPcts.forEach { item ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4)
                    ) {
                        Text(
                            text = item.name,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "${(item.pct * 100).toInt()}%",
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (budgetPcts.isEmpty()) {
                    Text(text = "All budgets on track")
                }
            }
        }
    }
}

class SciuroBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SciuroBudgetWidget()
}
