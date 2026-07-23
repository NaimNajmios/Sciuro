package com.sciuro.feature.wallet.ui

import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingBatteryScreen(
    guideSteps: List<String>,
    isAggressiveOem: Boolean,
    autostartIntent: android.content.Intent?,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val isExempt = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    LaunchedEffect(isExempt) {
        if (isExempt) {
            onComplete()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Stay Reliable",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Sciuro needs to run in the background to capture your bank notifications passively. " +
                "Battery optimization may prevent this, causing missed transactions.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isAggressiveOem) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Your device needs extra steps",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (step in guideSteps) {
                            Text(
                                step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Allow Background Activity")
            }

            if (autostartIntent != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(autostartIntent)
                        } catch (e: Exception) {
                            // Intent may not resolve on some devices
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Autostart Settings")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onSkip) {
                Text(
                    "Skip for now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
