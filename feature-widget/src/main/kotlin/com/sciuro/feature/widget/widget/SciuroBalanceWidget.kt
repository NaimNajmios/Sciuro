package com.sciuro.feature.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sciuro.core.ledger.repository.AccountRepository
import kotlinx.coroutines.flow.first
import org.koin.java.KoinJavaComponent

class SciuroBalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val accountRepo = KoinJavaComponent.get<AccountRepository>(AccountRepository::class.java)
        val accounts = accountRepo.observeAccounts().first()
        val netPosition = accounts.sumOf { it.balance }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Position",
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(8))
                Text(
                    text = "RM ${"%,.0f".format(netPosition)}",
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

class SciuroBalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SciuroBalanceWidget()
}
