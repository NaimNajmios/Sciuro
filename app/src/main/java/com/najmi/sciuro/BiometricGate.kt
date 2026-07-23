package com.najmi.sciuro

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

@Composable
fun BiometricGate(
    activity: FragmentActivity,
    lockEnabled: Boolean,
    onAuthenticated: @Composable () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var deviceSecurityReady by remember { mutableStateOf<Boolean?>(null) }
    var authAttempt by remember { mutableIntStateOf(0) }
    var backgroundedAt by remember { mutableStateOf<Long?>(null) }

    if (!lockEnabled) {
        LaunchedEffect(Unit) { isAuthenticated = true }
    }

    if (isAuthenticated) {
        onAuthenticated()
        return
    }

    val context = LocalContext.current

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundedAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val bgTime = backgroundedAt
                    if (bgTime != null && System.currentTimeMillis() - bgTime >= 30_000L) {
                        isAuthenticated = false
                        authError = null
                        deviceSecurityReady = null
                        authAttempt++
                    }
                }
                else -> {}
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(authAttempt) {
        if (!lockEnabled) return@LaunchedEffect

        deviceSecurityReady = null
        authError = null

        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            deviceSecurityReady = true
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        authError = errString.toString()
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                        authError = null
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        authError = "Authentication failed. Please try again."
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Sciuro")
                .setSubtitle("Authenticate to access your wallet")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            deviceSecurityReady = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                deviceSecurityReady == null -> {
                    CircularProgressIndicator()
                }
                deviceSecurityReady == false -> {
                    Text(
                        text = "Device security not set up",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Enable a screen lock (PIN, pattern, or password) on your device to use this feature.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { activity.finish() }) {
                        Text("Exit")
                    }
                }
                authError != null -> {
                    Text(
                        text = authError!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            authError = null
                            authAttempt++
                        }) {
                            Text("Retry")
                        }
                        Button(onClick = { activity.finish() }) {
                            Text("Exit")
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
