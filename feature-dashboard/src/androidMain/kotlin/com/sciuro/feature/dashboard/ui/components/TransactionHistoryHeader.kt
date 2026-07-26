package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.PillToggle
import com.sciuro.feature.dashboard.R

@Composable
fun TransactionHistoryHeader(
    typeFilter: String?,
    filterOptions: List<String>,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.dashboard_transaction_history),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        PillToggle(
            options = filterOptions,
            selectedOption = typeFilter ?: "All",
            onOptionSelected = onFilterSelected,
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true
        )
    }
}