package com.sciuro.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SheetList


import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.NotificationSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuietHoursPicker by remember { mutableStateOf(false) }
    var showBackupConfig by remember { mutableStateOf(false) }
    var showLargeTxnConfig by remember { mutableStateOf(false) }
    var showDebtDueConfig by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.notification_settings_title),
            heroFigure = { Text(stringResource(R.string.notification_settings_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.linked_accounts_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
            ) {
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_quiet_hours))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showQuietHoursPicker = !showQuietHoursPicker },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_section_quiet_hours), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (uiState.isQuietHoursEnabled) stringResource(R.string.settings_quiet_hours_suppressed, uiState.quietHoursStart, uiState.quietHoursEnd)
                                        else stringResource(R.string.settings_quiet_hours_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (showQuietHoursPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = showQuietHoursPicker,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(R.string.settings_section_quiet_hours), style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = uiState.isQuietHoursEnabled,
                                            onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                                        )
                                    }
                                    if (uiState.isQuietHoursEnabled) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(stringResource(R.string.settings_quiet_hours_start), style = MaterialTheme.typography.labelSmall)
                                                Text("${uiState.quietHoursStart}:00", style = MaterialTheme.typography.titleMedium)
                                                Row {
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursStart > 0) viewModel.setQuietHoursStart(uiState.quietHoursStart - 1)
                                                    }) { Text("\u2212") }
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursStart < 23) viewModel.setQuietHoursStart(uiState.quietHoursStart + 1)
                                                    }) { Text("+") }
                                                }
                                            }
                                            Text(stringResource(R.string.settings_quiet_hours_to), style = MaterialTheme.typography.bodyMedium)
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(stringResource(R.string.settings_quiet_hours_end), style = MaterialTheme.typography.labelSmall)
                                                Text("${uiState.quietHoursEnd}:00", style = MaterialTheme.typography.titleMedium)
                                                Row {
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursEnd > 0) viewModel.setQuietHoursEnd(uiState.quietHoursEnd - 1)
                                                    }) { Text("\u2212") }
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursEnd < 23) viewModel.setQuietHoursEnd(uiState.quietHoursEnd + 1)
                                                    }) { Text("+") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_notifications))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.notif_group_data_safety),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showBackupConfig = !showBackupConfig },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.notif_backup_reminder), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        stringResource(R.string.notif_backup_reminder_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.notif_interval_days, uiState.notifBackupInterval),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        if (showBackupConfig) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = uiState.notifBackupReminder,
                                    onCheckedChange = { viewModel.setNotifBackupReminder(it) }
                                )
                            }

                            AnimatedVisibility(
                                visible = showBackupConfig,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stringResource(R.string.notif_interval_days, uiState.notifBackupInterval),
                                        style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = uiState.notifBackupInterval.toFloat(),
                                        onValueChange = { viewModel.setNotifBackupInterval(it.toInt()) },
                                        valueRange = 1f..30f,
                                        steps = 28,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_runway_alert),
                                description = stringResource(R.string.notif_runway_alert_desc),
                                checked = uiState.notifRunwayAlert,
                                onCheckedChange = { viewModel.setNotifRunwayAlert(it) }
                            )
                        }
                    }
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.notif_group_spending),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showLargeTxnConfig = !showLargeTxnConfig },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.notif_large_txn), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        stringResource(R.string.notif_large_txn_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.notif_threshold_rm, uiState.notifLargeTxnThreshold),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        if (showLargeTxnConfig) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = uiState.notifLargeTxn,
                                    onCheckedChange = { viewModel.setNotifLargeTxn(it) }
                                )
                            }

                            AnimatedVisibility(
                                visible = showLargeTxnConfig,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.notif_threshold_rm, uiState.notifLargeTxnThreshold),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Slider(
                                        value = (uiState.notifLargeTxnThreshold / 100).toFloat(),
                                        onValueChange = { viewModel.setNotifLargeTxnThreshold((it * 100).toDouble().coerceAtLeast(100.0)) },
                                        valueRange = 1f..50f,
                                        steps = 48,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.notif_group_reminders),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showDebtDueConfig = !showDebtDueConfig },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.notif_debt_due), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        stringResource(R.string.notif_debt_due_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.notif_days_before, uiState.notifDebtDueDaysBefore),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        if (showDebtDueConfig) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = uiState.notifDebtDue,
                                    onCheckedChange = { viewModel.setNotifDebtDue(it) }
                                )
                            }

                            AnimatedVisibility(
                                visible = showDebtDueConfig,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.notif_days_before, uiState.notifDebtDueDaysBefore),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Slider(
                                        value = uiState.notifDebtDueDaysBefore.toFloat(),
                                        onValueChange = { viewModel.setNotifDebtDueDaysBefore(it.toInt()) },
                                        valueRange = 1f..30f,
                                        steps = 28,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_income_not_arrived),
                                description = stringResource(R.string.notif_income_not_arrived_desc),
                                checked = uiState.notifIncomeNotArrived,
                                onCheckedChange = { viewModel.setNotifIncomeNotArrived(it) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_review_reminder),
                                description = stringResource(R.string.notif_review_reminder_desc),
                                checked = uiState.notifReviewReminder,
                                onCheckedChange = { viewModel.setNotifReviewReminder(it) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_bill_autopay),
                                description = stringResource(R.string.notif_bill_autopay_desc),
                                checked = uiState.notifBillAutopay,
                                onCheckedChange = { viewModel.setNotifBillAutopay(it) }
                            )
                        }
                    }
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.notif_group_insights),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_weekly_digest),
                                description = stringResource(R.string.notif_weekly_digest_desc),
                                checked = uiState.notifWeeklyDigest,
                                onCheckedChange = { viewModel.setNotifWeeklyDigest(it) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_milestone),
                                description = stringResource(R.string.notif_milestone_desc),
                                checked = uiState.notifMilestone,
                                onCheckedChange = { viewModel.setNotifMilestone(it) }
                            )
                        }
                    }
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.notif_group_risk),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_bnpl_risk),
                                description = stringResource(R.string.notif_bnpl_risk_desc),
                                checked = uiState.notifBnplRisk,
                                onCheckedChange = { viewModel.setNotifBnplRisk(it) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_cash_anomaly),
                                description = stringResource(R.string.notif_cash_anomaly_desc),
                                checked = uiState.notifCashAnomaly,
                                onCheckedChange = { viewModel.setNotifCashAnomaly(it) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NotifToggleRow(
                                label = stringResource(R.string.notif_transfer_review),
                                description = stringResource(R.string.notif_transfer_review_desc),
                                checked = uiState.notifTransferReview,
                                onCheckedChange = { viewModel.setNotifTransferReview(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
