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
                    Text("Theme, dark mode scheduling", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Security", style = MaterialTheme.typography.titleMedium)
                    Text("Biometric lock, data encryption", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Linked Accounts", style = MaterialTheme.typography.titleMedium)
                    Text("Manage notification sources", style = MaterialTheme.typography.bodySmall)
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Text("Customize transaction categories", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
