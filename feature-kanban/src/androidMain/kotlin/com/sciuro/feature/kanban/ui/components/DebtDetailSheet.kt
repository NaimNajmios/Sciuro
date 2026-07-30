package com.sciuro.feature.kanban.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.sciuro.core.debt.engine.CreditCardStatementEngine
import com.sciuro.core.debt.engine.StatementSummary
import com.sciuro.core.debt.model.DebtType
import com.sciuro.feature.kanban.R
import com.sciuro.feature.kanban.model.DebtTask
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailSheet(
    debt: DebtTask,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val engine: CreditCardStatementEngine = koinInject()
    val statement by produceState<StatementSummary?>(initialValue = null, key1 = debt.id) {
        value = if (debt.type == DebtType.CREDIT_CARD) engine.getStatementSummary(debt.id) else null
    }

    SciuroFormSheet(
        title = stringResource(R.string.kanban_debt_details_title),
        onDismissRequest = onDismiss,
        icon = SciuroIcons.Payments
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (debt.counterpartyName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = debt.counterpartyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "RM ${"%.2f".format(debt.remainingBalance)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (statement != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.kanban_cc_statement_title, "${"%.0f".format(statement!!.statementBalance)}"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.kanban_cc_statement_detail, "${"%.0f".format(statement!!.minPaymentDue)}", statement!!.paymentCount, statement!!.daysRemaining),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val progressColor = if (debt.progress > 0.75f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        LinearProgressIndicator(
            progress = { if (debt.progress > 1f) 1f else debt.progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Remaining: RM ${"%.2f".format(debt.remainingBalance)}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Total: RM ${"%.2f".format(debt.debt.principalAmount)}",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = SciuroIcons.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.kanban_delete))
            }

            Button(
                onClick = onEditClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = SciuroIcons.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.kanban_edit))
            }
        }
    }
}
