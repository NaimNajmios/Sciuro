package com.sciuro.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.najmi.sciuro.core.ui.theme.BrandPrimaryDark
import com.najmi.sciuro.core.ui.theme.ThemeManager
import com.najmi.sciuro.core.ui.theme.ThemePreference
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import android.os.Build
import android.os.PowerManager
import com.najmi.sciuro.core.ui.util.OemAutostartHelper
import com.sciuro.feature.settings.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToDataSettings: () -> Unit = {},
    onNavigateToIntelligenceSettings: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val themePref by themeManager.themePreference.collectAsState()
    var developerTapCount by remember { mutableIntStateOf(0) }
    val snackbarHostState = LocalSnackbarHostState.current
    val devGateSnackbarText = stringResource(R.string.dev_gate_snackbar)
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val enabledNotifCount = listOf(
        uiState.notifBackupReminder,
        uiState.notifRunwayAlert,
        uiState.notifLargeTxn,
        uiState.notifDebtDue,
        uiState.notifIncomeNotArrived,
        uiState.notifReviewReminder,
        uiState.notifBillAutopay,
        uiState.notifWeeklyDigest,
        uiState.notifMilestone,
        uiState.notifBnplRisk,
        uiState.notifCashAnomaly,
        uiState.notifTransferReview
    ).count { it }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.settings_title),
            heroFigure = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, color = BrandPrimaryDark) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {}
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
            ) {
                // Section: Appearance
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val themeLabels = listOf(
                                stringResource(R.string.settings_theme_system),
                                stringResource(R.string.settings_theme_light),
                                stringResource(R.string.settings_theme_dark)
                            )
                            val themeSystemLabel = stringResource(R.string.settings_theme_system)
                            val themeLightLabel = stringResource(R.string.settings_theme_light)
                            val themeDarkLabel = stringResource(R.string.settings_theme_dark)
                            val selectedLabel = when (themePref) {
                                ThemePreference.SYSTEM_DEFAULT -> themeSystemLabel
                                ThemePreference.LIGHT -> themeLightLabel
                                ThemePreference.DARK -> themeDarkLabel
                            }
                            PillToggle(
                                options = themeLabels,
                                selectedOption = selectedLabel,
                                onOptionSelected = { label ->
                                    val pref = when (label) {
                                        themeSystemLabel -> ThemePreference.SYSTEM_DEFAULT
                                        themeLightLabel -> ThemePreference.LIGHT
                                        themeDarkLabel -> ThemePreference.DARK
                                        else -> ThemePreference.SYSTEM_DEFAULT
                                    }
                                    themeManager.setTheme(pref)
                                },
                                fillWidth = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Section: Background Reliability
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_background_reliability))
                }

                item {
                    var isBatteryExempt by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        isBatteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                            pm.isIgnoringBatteryOptimizations(context.packageName)
                        } else {
                            true
                        }
                    }
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (isBatteryExempt) stringResource(R.string.settings_battery_optimization_disabled)
                                else stringResource(R.string.settings_battery_optimization_active),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            if (!isBatteryExempt) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.settings_disable_battery_optimization))
                                }
                            }
                            val autostartIntent = remember { OemAutostartHelper.getAutostartIntent() }
                            if (autostartIntent != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(autostartIntent)
                                        } catch (_: Exception) { }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.settings_open_autostart))
                                }
                            }
                        }
                    }
                }

                // Section: Security
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_security))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_lock_app_on_launch), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.settings_lock_app_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = uiState.isLockEnabled,
                                onCheckedChange = { viewModel.setLockEnabled(it) }
                            )
                        }
                    }
                }

                // Navigation: Notifications
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_navigation))
                }

                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = onNavigateToNotificationSettings
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.settings_notifications_summary, enabledNotifCount, 12),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.isQuietHoursEnabled) {
                                    Text(
                                        stringResource(R.string.settings_quiet_hours_summary, uiState.quietHoursStart, uiState.quietHoursEnd),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_notifications))
                        }
                    }
                }

                // Navigation: Data & Privacy
                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = onNavigateToDataSettings
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_data_privacy), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.settings_budget_summary, (uiState.budgetWarningThreshold * 100).toInt()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_data_privacy))
                        }
                    }
                }

                // Navigation: Intelligence & Automation
                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = onNavigateToIntelligenceSettings
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_intelligence), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    buildString {
                                        append(if (uiState.isLlmEnabled) stringResource(R.string.settings_summary_llm_on) else stringResource(R.string.settings_summary_llm_off))
                                        append(" · ")
                                        append(if (uiState.isObligationAutoConfirmEnabled) stringResource(R.string.settings_summary_autoconfirm_on) else stringResource(R.string.settings_summary_autoconfirm_off))
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_intelligence))
                        }
                    }
                }

                // Section: Developer
                item {
                    SettingsSectionHeader(
                        title = stringResource(R.string.settings_developer_options),
                        modifier = Modifier.clickable {
                            developerTapCount++
                            if (developerTapCount >= 7) {
                                developerTapCount = 0
                                viewModel.setDeveloperOptionsVisible(true)
                                scope.launch {
                                    snackbarHostState.showSnackbar(devGateSnackbarText)
                                }
                            }
                        }
                    )
                }

                if (uiState.isDeveloperOptionsVisible) {
                    item {
                        SciuroCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            onClick = onNavigateToDeveloperSettings
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.settings_developer_options), style = MaterialTheme.typography.titleMedium)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_developer_options))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp)
    )
}
