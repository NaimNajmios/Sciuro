package com.sciuro.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.sciuro.feature.dashboard.R
import com.sciuro.feature.dashboard.util.PackageLabelResolver
import com.sciuro.feature.dashboard.viewmodel.ActivityLogViewModel
import com.sciuro.feature.dashboard.viewmodel.ActivityLogStatus
import com.sciuro.feature.dashboard.viewmodel.IngestionActivityEntry
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityLogScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ActivityLogViewModel = koinViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val context = LocalContext.current
    val labelResolver = remember { PackageLabelResolver(context) }
    val timeFormat = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.activity_log_title),
            heroFigure = {
                Text(
                    text = stringResource(R.string.activity_log_subtitle),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.activity_log_back_cd),
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
                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.activity_log_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                } else {
                    items(entries.size) { index ->
                        ActivityLogRow(
                            entry = entries[index],
                            sourceLabel = labelResolver.label(entries[index].sourceType, entries[index].sourcePackageOrAddress),
                            timeText = timeFormat.format(Date(entries[index].timestamp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogRow(
    entry: IngestionActivityEntry,
    sourceLabel: String,
    timeText: String,
    modifier: Modifier = Modifier
) {
    val t = com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens.current
    val dotColor = when (entry.status) {
        ActivityLogStatus.PARSED -> t.signalIncome
        ActivityLogStatus.NEEDS_REVIEW -> t.signalWarning
        ActivityLogStatus.DROPPED -> t.signalDanger
    }
    val statusText = when (entry.status) {
        ActivityLogStatus.PARSED -> stringResource(R.string.activity_status_parsed)
        ActivityLogStatus.NEEDS_REVIEW -> stringResource(R.string.activity_status_needs_review)
        ActivityLogStatus.DROPPED -> stringResource(R.string.activity_status_dropped)
    }

    SciuroCard(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.status == ActivityLogStatus.DROPPED && entry.reason != null) {
                    Text(
                        text = sanitizedReason(entry.reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = dotColor
            )
        }
    }
}

private fun sanitizedReason(reason: String): String = when {
    reason.contains("timeout") -> "Timed out"
    reason.contains("network") -> "Network error"
    reason.contains("no_key", ignoreCase = true) || reason.contains("disabled", ignoreCase = true) -> "Parser unavailable"
    reason.contains("circuit") -> "Parser temporarily unavailable"
    reason.contains("malformed") || reason.contains("empty_response") -> "Unexpected response"
    reason.contains("invalid_amount") -> "Invalid amount"
    reason.contains("unknown_direction") -> "Couldn't determine direction"
    else -> "Couldn't be parsed"
}
