package com.sciuro.feature.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.sciuro.feature.wallet.R

@Composable
fun AccountDetailHero(
    accountName: String,
    accountBalance: Double,
    accountColor: String?,
    accountType: String,
    accountNumber: String?,
    qrImagePath: String?,
    isCashWallet: Boolean,
    onNavigateBack: () -> Unit,
    onQrClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onEditDetails: () -> Unit,
    onChangeColor: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    isSystem: Boolean,
    modifier: Modifier = Modifier
) {
    val heroColor = parseAccountColor(accountColor) ?: MaterialTheme.colorScheme.primary
    val isOnDarkSurface = heroColor.luminance() < 0.5f
    val onHero = if (isOnDarkSurface) Color.White else Color.Black
    val statusBarDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(heroColor)
            .padding(top = statusBarDp + 24.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = SciuroIcons.Back,
                    contentDescription = stringResource(R.string.wallet_back_cd),
                    tint = onHero
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero.copy(alpha = 0.7f)
                    )
                    AccountTypeBadge(type = accountType, textColor = onHero)
                }
            }

            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.wallet_more_options_cd),
                        tint = onHero
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wallet_edit_details)) },
                        leadingIcon = { Icon(SciuroIcons.Edit, contentDescription = null) },
                        onClick = { expanded = false; onEditDetails() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wallet_change_color)) },
                        leadingIcon = { Icon(SciuroIcons.Tune, contentDescription = null) },
                        onClick = { expanded = false; onChangeColor() }
                    )
                    if (!isSystem) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wallet_archive_account)) },
                            leadingIcon = { Icon(SciuroIcons.Close, contentDescription = null) },
                            onClick = { expanded = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wallet_delete_account_title), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(SciuroIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            HeroFigure(accountBalance)
        }

        if (!accountNumber.isNullOrBlank()) {
            Text(
                text = "**** ${accountNumber.takeLast(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = onHero.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (qrImagePath != null && !isCashWallet) {
                FilledTonalButton(
                    onClick = onQrClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = onHero.copy(alpha = 0.15f),
                        contentColor = onHero
                    )
                ) {
                    Icon(
                        SciuroIcons.QrCodeScanner,
                        contentDescription = stringResource(R.string.wallet_view_qr_cd),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            FilledTonalButton(
                onClick = onAdjustClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = onHero.copy(alpha = 0.15f),
                    contentColor = onHero
                )
            ) {
                Icon(
                    SciuroIcons.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.wallet_adjust_balance))
            }
        }
    }
}

@Composable
private fun AccountTypeBadge(type: String, textColor: Color) {
    val label = when {
        type.lowercase().contains("cash") || type.lowercase().contains("personal") -> "Cash"
        type.lowercase().contains("credit") -> "Credit"
        type.lowercase().contains("investment") -> "Investment"
        type.lowercase().contains("wallet") -> "EWallet"
        type.lowercase().contains("saving") -> "Savings"
        type.lowercase().contains("current") -> "Current"
        else -> type
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = textColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun parseAccountColor(hex: String?): Color? {
    if (hex == null) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}
