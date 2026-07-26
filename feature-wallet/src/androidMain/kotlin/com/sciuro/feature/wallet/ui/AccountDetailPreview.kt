package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.theme.SciuroTheme

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Account Detail")
@Composable
private fun AccountDetailPreview() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Maybank Account",
                heroFigure = { HeroFigure(5240.50) },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                chartData = listOf(100f, 150f, 200f, 180f, 250f)
            )
            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "No transactions for this account.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
