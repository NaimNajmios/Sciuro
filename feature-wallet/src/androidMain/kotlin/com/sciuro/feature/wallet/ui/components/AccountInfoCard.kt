package com.sciuro.feature.wallet.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard

@Composable
fun AccountInfoCard(
    accountNumber: String?,
    accountHolderName: String?,
    bankInstitutionCode: String?,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    val hasBasicInfo = !accountNumber.isNullOrBlank() || !accountHolderName.isNullOrBlank() || !bankInstitutionCode.isNullOrBlank()
    if (!hasBasicInfo) return

    val context = LocalContext.current

    SciuroCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val parts = mutableListOf<String>()
                if (!accountHolderName.isNullOrBlank()) parts.add(accountHolderName)
                if (!accountNumber.isNullOrBlank()) parts.add(accountNumber)
                if (!bankInstitutionCode.isNullOrBlank()) parts.add(bankInstitutionCode)
                if (parts.isNotEmpty()) {
                    copyToClipboard(context, "account_info", parts.joinToString("\n"))
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Account Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (!accountHolderName.isNullOrBlank()) {
                DetailRow(label = "Holder", value = accountHolderName)
            }
            if (!accountNumber.isNullOrBlank()) {
                DetailRow(label = "Account", value = accountNumber)
            }
            if (!bankInstitutionCode.isNullOrBlank()) {
                DetailRow(label = "Bank Code", value = bankInstitutionCode)
            }

            if (transactionCount > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$transactionCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
