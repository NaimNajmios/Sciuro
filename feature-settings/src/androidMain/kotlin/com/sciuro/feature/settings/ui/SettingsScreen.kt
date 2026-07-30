package com.sciuro.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import com.najmi.sciuro.core.ui.util.SciuroIcons

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip


import com.najmi.sciuro.core.ui.theme.PalettePreference
import com.najmi.sciuro.core.ui.theme.ThemeManager
import com.najmi.sciuro.core.ui.theme.ThemePreference
import org.koin.compose.koinInject
import com.najmi.sciuro.core.ui.theme.paletteColors
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroNavigationCard
import com.najmi.sciuro.core.ui.components.SciuroSectionHeader
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
    val themeManager: ThemeManager = koinInject()
    val themePref by themeManager.themePreference.collectAsState()
    val palettePref by themeManager.palettePreference.collectAsState()
    var showPaletteSheet by remember { mutableStateOf(false) }
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
            heroFigure = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
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
                    SciuroSectionHeader(stringResource(R.string.settings_section_appearance), Icons.Filled.Settings)
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

                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = { showPaletteSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Text(stringResource(R.string.settings_palette), style = MaterialTheme.typography.titleMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    drawCircle(color = paletteColors(palettePref, false).primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    paletteDisplayName(palettePref),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = stringResource(R.string.settings_palette),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Dark mode schedule
                item {
                    val isScheduleEnabled = themeManager.isDarkModeScheduleEnabled()
                    var showScheduleSheet by remember { mutableStateOf(false) }
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.settings_dark_schedule),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        stringResource(R.string.settings_dark_schedule_summary),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isScheduleEnabled,
                                    onCheckedChange = { themeManager.setDarkModeScheduleEnabled(it) }
                                )
                            }
                            if (isScheduleEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showScheduleSheet = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.settings_dark_schedule_start),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        formatTime(themeManager.getDarkModeScheduleStart()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showScheduleSheet = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.settings_dark_schedule_end),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        formatTime(themeManager.getDarkModeScheduleEnd()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    if (showScheduleSheet) {
                        DarkModeScheduleSheet(
                            themeManager = themeManager,
                            onDismiss = { showScheduleSheet = false }
                        )
                    }
                }

                // Section: Background Reliability
                item {
                    SciuroSectionHeader(stringResource(R.string.settings_section_background_reliability), icon = SciuroIcons.Info)
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
                    SciuroSectionHeader(stringResource(R.string.settings_section_security), Icons.Filled.Lock)
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
                    SciuroSectionHeader(stringResource(R.string.settings_section_navigation), icon = SciuroIcons.Search)
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_notifications),
                        summary = buildString {
                            append(stringResource(R.string.settings_notifications_summary, enabledNotifCount, 12))
                            if (uiState.isQuietHoursEnabled) {
                                append(" · ")
                                append(stringResource(R.string.settings_quiet_hours_summary, uiState.quietHoursStart, uiState.quietHoursEnd))
                            }
                        },
                        leadingIcon = Icons.Filled.Notifications,
                        onClick = onNavigateToNotificationSettings
                    )
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_data_privacy),
                        summary = stringResource(R.string.settings_budget_summary, (uiState.budgetWarningThreshold * 100).toInt()),
                        leadingIcon = SciuroIcons.Lock,
                        onClick = onNavigateToDataSettings
                    )
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_intelligence),
                        summary = buildString {
                            append(if (uiState.isLlmEnabled) stringResource(R.string.settings_summary_llm_on) else stringResource(R.string.settings_summary_llm_off))
                            append(" · ")
                            append(if (uiState.isObligationAutoConfirmEnabled) stringResource(R.string.settings_summary_autoconfirm_on) else stringResource(R.string.settings_summary_autoconfirm_off))
                        },
                        leadingIcon = SciuroIcons.Star,
                        onClick = onNavigateToIntelligenceSettings
                    )
                }

                // Section: Developer
                item {
                    SciuroSectionHeader(
                        title = stringResource(R.string.settings_developer_options),
                        icon = Icons.Filled.Settings,
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
                        SciuroNavigationCard(
                            title = stringResource(R.string.settings_developer_options),
                            summary = "",
                            leadingIcon = Icons.Filled.Settings,
                            onClick = onNavigateToDeveloperSettings
                        )
                    }
                }
            }
        }
    }

    if (showPaletteSheet) {
        SciuroBottomSheet(onDismissRequest = { showPaletteSheet = false }) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_palette_choose),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PalettePreference.entries.filter {
                it != PalettePreference.DYNAMIC || android.os.Build.VERSION.SDK_INT >= 31
            }.forEach { palette ->
                val isSelected = palette == palettePref
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            themeManager.setPalette(palette)
                            showPaletteSheet = false
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(paletteColors(palette, false).primary)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        paletteDisplayName(palette),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isSelected) {
                        Icon(
                            SciuroIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatTime(time: java.time.LocalTime): String = time.format(timeFormatter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkModeScheduleSheet(
    themeManager: ThemeManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val startTime = remember { themeManager.getDarkModeScheduleStart() }
    val endTime = remember { themeManager.getDarkModeScheduleEnd() }

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.settings_dark_schedule),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            themeManager.setDarkModeScheduleStart(java.time.LocalTime.of(hour, minute))
                        },
                        startTime.hour,
                        startTime.minute,
                        false
                    ).show()
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_dark_schedule_start))
                Text(
                    formatTime(startTime),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            themeManager.setDarkModeScheduleEnd(java.time.LocalTime.of(hour, minute))
                        },
                        endTime.hour,
                        endTime.minute,
                        false
                    ).show()
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_dark_schedule_end))
                Text(
                    formatTime(endTime),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SciuroPrimaryButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun paletteDisplayName(palette: PalettePreference): String = when (palette) {
    PalettePreference.MONOCHROME -> stringResource(R.string.palette_monochrome)
    PalettePreference.AMBER -> stringResource(R.string.palette_amber)
    PalettePreference.OCEAN -> stringResource(R.string.palette_ocean)
    PalettePreference.FOREST -> stringResource(R.string.palette_forest)
    PalettePreference.PLUM -> stringResource(R.string.palette_plum)
    PalettePreference.SLATE -> stringResource(R.string.palette_slate)
    PalettePreference.DYNAMIC -> stringResource(R.string.palette_dynamic)
}


