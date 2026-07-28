package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.SciuroTheme

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Settings")
@Composable
private fun SettingsPreview() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Text("System / Light / Dark", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Security", style = MaterialTheme.typography.titleMedium)
                    Text("Biometric lock on launch", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Text("Manage alert preferences and quiet hours", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data & Privacy", style = MaterialTheme.typography.titleMedium)
                    Text("Backup, linked accounts, budget threshold", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Intelligence & Automation", style = MaterialTheme.typography.titleMedium)
                    Text("LLM classification, auto-confirm bills", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
