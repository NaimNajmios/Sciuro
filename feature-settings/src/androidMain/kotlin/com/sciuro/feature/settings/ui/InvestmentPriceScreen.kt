package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.theme.BrandPrimaryDark
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.InvestmentPriceItem
import com.sciuro.feature.settings.viewmodel.InvestmentPriceViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun InvestmentPriceScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: InvestmentPriceViewModel = koinViewModel()
) {
    val uiState by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.investment_price_title),
            heroFigure = { Text(stringResource(R.string.investment_price_title), style = MaterialTheme.typography.headlineLarge, color = BrandPrimaryDark) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.investment_price_back),
                        tint = BrandPrimaryDark
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Text(
                        stringResource(R.string.investment_price_manage),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                if (uiState.items.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.investment_price_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                        )
                    }
                }

                items(uiState.items) { item ->
                    InvestmentPriceCard(
                        item = item,
                        onSetPrice = { symbol, price -> viewModel.setManualPrice(item.assetType, symbol, price) },
                        onClear = { viewModel.clearManualPrice(item.assetType, item.assetSymbol) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun InvestmentPriceCard(
    item: InvestmentPriceItem,
    onSetPrice: (String, Double) -> Unit,
    onClear: () -> Unit
) {
    var priceText by remember(item.id) { mutableStateOf(item.manualPrice) }

    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.assetSymbol, style = MaterialTheme.typography.titleMedium)
                    Text(item.assetName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("RM ${"%.2f".format(item.bookValue)}", style = MaterialTheme.typography.bodyMedium, fontFamily = IBMPlexMono)
                    if (item.liveValue > 0) {
                        Text("Live: RM ${"%.2f".format(item.liveValue)}", style = MaterialTheme.typography.labelSmall, fontFamily = IBMPlexMono, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SciuroTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = "Set manual price",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull()
                        if (price != null && price > 0) onSetPrice(item.assetSymbol, price)
                    },
                    enabled = priceText.toDoubleOrNull() != null && priceText.toDoubleOrNull()!! > 0,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Set")
                }
                if (item.hasManualOverride) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.height(48.dp)) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}
