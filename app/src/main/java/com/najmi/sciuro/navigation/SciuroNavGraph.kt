package com.najmi.sciuro.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.cash.sqldelight.db.SqlDriver
import com.najmi.sciuro.core.ui.theme.SciuroMotion
import com.najmi.sciuro.export.EncryptedExporter
import com.najmi.sciuro.export.EncryptedImporter
import com.sciuro.feature.budgets.ui.BudgetsScreen
import com.sciuro.feature.budgets.ui.CategoryDrilldownScreen
import com.sciuro.feature.budgets.ui.CategoryTransactionsScreen
import com.sciuro.feature.dashboard.ui.ActivityLogScreen
import com.sciuro.feature.dashboard.ui.DashboardScreen
import com.sciuro.feature.debt.ui.DebtOverviewScreen
import com.sciuro.feature.kanban.ui.KanbanScreen
import com.sciuro.feature.settings.ui.CategorySettingsScreen
import com.sciuro.feature.settings.ui.DataSettingsScreen
import com.sciuro.feature.settings.ui.DeveloperSettingsScreen
import com.sciuro.feature.settings.ui.IntelligenceSettingsScreen
import com.sciuro.feature.settings.ui.InvestmentPriceScreen
import com.sciuro.feature.settings.ui.LinkedAccountsScreen
import com.sciuro.feature.settings.ui.MerchantRulesScreen
import com.sciuro.feature.settings.ui.NotificationSettingsScreen
import com.sciuro.feature.settings.ui.SettingsScreen
import com.sciuro.feature.settings.viewmodel.LinkedAccountsViewModel
import com.sciuro.feature.wallet.ui.AccountDetailScreen
import com.sciuro.feature.wallet.ui.OnboardingScreen
import com.sciuro.feature.wallet.ui.WalletScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.sciuro.core.ledger.config.SettingsProvider
import java.io.File
import java.io.FileOutputStream

@Composable
fun SciuroNavGraph(
    navController: NavHostController,
    startDestination: String,
    onboardingViewModel: com.sciuro.feature.wallet.viewmodel.OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(SciuroRoute.Onboarding.route) {
            OnboardingScreen(viewModel = onboardingViewModel)
        }

        composable(SciuroRoute.Dashboard.route) {
            DashboardScreen(
                onOpenActivityLog = {
                    navController.navigate(SciuroRoute.ActivityLog.route)
                }
            )
        }

        composable(SciuroRoute.ActivityLog.route) {
            ActivityLogScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SciuroRoute.Wallet.route) {
            WalletScreen(onAccountClick = { accountId ->
                navController.navigate(SciuroRoute.AccountDetail.createRoute(accountId))
            })
        }

        composable(
            route = SciuroRoute.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: return@composable
            AccountDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SciuroRoute.Budgets.route) {
            BudgetsScreen(
                onNavigateToCategoryDrilldown = {
                    navController.navigate(SciuroRoute.CategoryDrilldown.route)
                }
            )
        }

        composable(SciuroRoute.CategoryDrilldown.route) {
            CategoryDrilldownScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategoryTransactions = { categoryId, categoryName ->
                    navController.navigate(SciuroRoute.CategoryTransactions.createRoute(categoryId, categoryName))
                }
            )
        }

        composable(
            route = SciuroRoute.CategoryTransactions.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryTransactionsScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(SciuroRoute.DebtOverview.route) {
            DebtOverviewScreen()
        }

        composable(SciuroRoute.Kanban.route) {
            KanbanScreen()
        }

        composable(SciuroRoute.Settings.route) {
            SettingsScreen(
                onNavigateToNotificationSettings = {
                    navController.navigate(SciuroRoute.NotificationSettings.route)
                },
                onNavigateToDataSettings = {
                    navController.navigate(SciuroRoute.DataSettings.route)
                },
                onNavigateToIntelligenceSettings = {
                    navController.navigate(SciuroRoute.IntelligenceSettings.route)
                },
                onNavigateToDeveloperSettings = {
                    navController.navigate(SciuroRoute.DeveloperSettings.createRoute(0))
                }
            )
        }

        composable(SciuroRoute.NotificationSettings.route) {
            NotificationSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SciuroRoute.DataSettings.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val sqlDriver = koinInject<SqlDriver>()
            val settingsProvider = koinInject<SettingsProvider>()

            DataSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLinkedAccounts = {
                    navController.navigate(SciuroRoute.LinkedAccounts.route)
                },
                onNavigateToCategorySettings = {
                    navController.navigate(SciuroRoute.CategorySettings.route)
                },
                onNavigateToMerchantRules = {
                    navController.navigate(SciuroRoute.MerchantRules.route)
                },
                onNavigateToInvestmentPrice = {
                    navController.navigate(SciuroRoute.InvestmentPriceSettings.route)
                },
                onExportBackup = { password ->
                    scope.launch(Dispatchers.IO) {
                        exportBackup(context, password, sqlDriver, settingsProvider)
                    }
                },
                onImportBackup = { uri, password ->
                    scope.launch(Dispatchers.IO) {
                        importBackup(context, password, uri, sqlDriver)
                    }
                }
            )
        }

        composable(SciuroRoute.IntelligenceSettings.route) {
            IntelligenceSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SciuroRoute.CategorySettings.route) {
            CategorySettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = SciuroRoute.DeveloperSettings.route,
            arguments = listOf(navArgument("initialTab") { defaultValue = "0" })
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("initialTab")?.toIntOrNull() ?: 0
            DeveloperSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                initialTab = initialTab
            )
        }

        composable(SciuroRoute.MerchantRules.route) {
            MerchantRulesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SciuroRoute.InvestmentPriceSettings.route) {
            InvestmentPriceScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SciuroRoute.LinkedAccounts.route) {
            val linkedAccountsViewModel: LinkedAccountsViewModel = koinViewModel()
            LinkedAccountsScreen(
                viewModel = linkedAccountsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private suspend fun exportBackup(context: Context, password: String, sqlDriver: SqlDriver, settingsProvider: SettingsProvider) {
    try {
        val tempFile = File(context.cacheDir, "sciuro_backup_${System.currentTimeMillis()}.scib")
        val outputStream = FileOutputStream(tempFile)
        val result = EncryptedExporter.export(context, password, outputStream, sqlDriver)
        outputStream.close()
        if (result.isSuccess) {
            settingsProvider.setLastBackupTimestamp(System.currentTimeMillis())
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            withContext(Dispatchers.Main) {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                } catch (_: Exception) {
                    Toast.makeText(context, "No app available to share the backup", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

private suspend fun importBackup(context: Context, password: String, uri: android.net.Uri, sqlDriver: SqlDriver) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val result = EncryptedImporter.import(context, password, inputStream, sqlDriver)
            inputStream.close()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val intent = Intent(context, com.najmi.sciuro.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    if (context is Activity) context.finish()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(context, "Import failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: Could not read file", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
