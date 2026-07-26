package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.theme.SciuroTheme

@Preview(showBackground = true, showSystemUi = true, name = "Wallet - Empty")
@Composable
private fun WalletPreviewEmpty() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Total Liquidity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                HeroFigure(amount = 0.0)
                Spacer(modifier = Modifier.height(24.dp))
                PillToggle(
                    options = listOf("Liquid Cash", "Investments"),
                    selectedOption = "Liquid Cash",
                    onOptionSelected = {},
                    isOnDarkSurface = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "No accounts yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Wallet - With Data")
@Composable
private fun WalletPreviewWithData() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Total Liquidity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                HeroFigure(amount = 8540.50)
                Spacer(modifier = Modifier.height(24.dp))
                PillToggle(
                    options = listOf("Liquid Cash", "Investments"),
                    selectedOption = "Liquid Cash",
                    onOptionSelected = {},
                    isOnDarkSurface = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
