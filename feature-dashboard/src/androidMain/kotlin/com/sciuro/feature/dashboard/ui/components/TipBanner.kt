package com.sciuro.feature.dashboard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.feature.dashboard.R

@Composable
fun TipBanner(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    val tips = listOf(
        stringResource(R.string.dashboard_tip_notifications),
        stringResource(R.string.dashboard_tip_swipe),
        stringResource(R.string.dashboard_tip_tap)
    )
    var currentTip by remember { mutableIntStateOf(0) }

    AnimatedVisibility(visible = visible) {
        Card(
            modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    tips[currentTip],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        val next = currentTip + 1
                        if (next < tips.size) {
                            currentTip = next
                        } else {
                            visible = false
                            onDismiss()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (currentTip < tips.size - 1) stringResource(R.string.dashboard_next) else stringResource(R.string.dashboard_got_it),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(
                    onClick = {
                        visible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.dashboard_dismiss_cd),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
