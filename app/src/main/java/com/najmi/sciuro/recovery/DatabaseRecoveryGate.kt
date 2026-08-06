package com.najmi.sciuro.recovery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.cash.sqldelight.db.SqlDriver
import com.najmi.sciuro.MainActivity
import com.najmi.sciuro.export.EncryptedImporter
import com.sciuro.core.ledger.security.DatabaseRecoveryManager
import com.sciuro.feature.settings.ui.DatabaseRecoveryScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Full-screen gate rendered after the biometric lock. When a quarantined database is
 * waiting for a recovery decision it intercepts the normal app content and shows the
 * recovery interstitial instead. The user either restores an encrypted backup or
 * explicitly starts fresh.
 */
@Composable
fun DatabaseRecoveryGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val recoveryManager = remember { DatabaseRecoveryManager(appContext) }
    var pending by remember { mutableStateOf(recoveryManager.isRecoveryPending()) }

    if (!pending) {
        content()
        return
    }

    val scope = rememberCoroutineScope()
    val sqlDriver = koinInject<SqlDriver>()

    DatabaseRecoveryScreen(
        quarantineCount = recoveryManager.quarantineCount(),
        lastQuarantineTimestamp = recoveryManager.lastQuarantineTimestamp(),
        onImportBackup = { uri, password ->
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val inputStream = appContext.contentResolver.openInputStream(uri)
                            ?: return@runCatching Result.failure<Nothing>(Exception("Could not read file"))
                        try {
                            EncryptedImporter.import(appContext, password, inputStream, sqlDriver)
                        } finally {
                            inputStream.close()
                        }
                    }
                }
                result.fold(
                    onSuccess = { importResult ->
                        if (importResult.isSuccess) {
                            recoveryManager.markRecoveryAcknowledged()
                            restartApp(appContext)
                        } else {
                            Toast.makeText(
                                appContext,
                                "Import failed: ${importResult.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            appContext,
                            "Import failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        },
        onStartFresh = {
            recoveryManager.markRecoveryAcknowledged()
            restartApp(appContext)
        }
    )
}

private fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    context.startActivity(intent)
    if (context is Activity) context.finish()
}
