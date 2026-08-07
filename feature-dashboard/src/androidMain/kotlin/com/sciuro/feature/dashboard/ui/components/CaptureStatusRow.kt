package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.sciuro.feature.dashboard.R
import com.sciuro.feature.dashboard.viewmodel.CaptureHealth
import com.sciuro.feature.dashboard.viewmodel.CaptureStatus

@Composable
fun CaptureStatusRow(
    status: CaptureStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalSciuroSemanticTokens.current
    val dotColor = when (status.health) {
        CaptureHealth.HEALTHY -> t.signalIncome
        CaptureHealth.STALE -> t.signalWarning
        CaptureHealth.OFF -> t.signalDanger
    }
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val label = when (status.health) {
        CaptureHealth.HEALTHY -> stringResource(R.string.capture_status_active)
        CaptureHealth.STALE -> {
            val minutes = status.lastCapturedAt?.let { last ->
                ((System.currentTimeMillis() - last) / 60_000L).coerceAtLeast(1)
            }
            stringResource(R.string.capture_status_stale_minutes, minutes ?: 1)
        }
        CaptureHealth.OFF -> when {
            !status.isListenerEnabled -> stringResource(R.string.capture_status_off)
            status.lastCapturedAt == null -> stringResource(R.string.capture_status_no_capture)
            else -> stringResource(R.string.capture_status_stale)
        }
    }

    val tapCd = stringResource(R.string.capture_status_tap_cd)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .semantics { contentDescription = tapCd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = onPrimary.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.capture_status_activity_log),
            style = MaterialTheme.typography.labelMedium,
            color = onPrimary.copy(alpha = 0.7f)
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = onPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}
