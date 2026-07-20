package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    var customPackage by remember { mutableStateOf("com.google.android.gm") }
    var customTitle by remember { mutableStateOf("m2u Notification") }
    var customText by remember { mutableStateOf("Please find the details of the transfer below: A transfer of RM 50.00 has been successfully processed from my M2U account.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    "Developer Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dynamic Pipeline Simulator", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = customPackage,
                            onValueChange = { customPackage = it },
                            label = { Text("Package Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Notification Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it },
                            label = { Text("Notification Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.simulateNotification(customTitle, customText, customPackage)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send to Pipeline")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quick Simulators", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                viewModel.simulateNotification(
                                    title = "m2u Notification",
                                    text = "RM 50.00 transferred from M2U.",
                                    packageName = "com.google.android.gm"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate m2u Email")
                        }
                        
                        Button(
                            onClick = {
                                viewModel.simulateNotification(
                                    title = "Transfer to Ahmad",
                                    text = "You paid RM 12.00 to Ahmad",
                                    packageName = "com.maybank2u.life"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate MAE Push")
                        }
                        
                        Button(
                            onClick = {
                                viewModel.simulateNotification(
                                    title = "CIMB OCTO",
                                    text = "RM 8.50 paid to Starbucks",
                                    packageName = "my.com.cimb.octo"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate CIMB Push")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Database Tools", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.clearInbox() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Inbox (Unreviewed)", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }
    }
}
