package com.najmi.sciuro

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.najmi.sciuro.core.ui.theme.SciuroTheme
import com.najmi.sciuro.core.ui.theme.SciuroMotion
import com.najmi.sciuro.core.ui.theme.DarkSurfaceSheet
import com.najmi.sciuro.core.ui.theme.LightSurfaceSheet
import com.sciuro.feature.dashboard.ui.DashboardScreen
import com.sciuro.feature.wallet.ui.WalletScreen
import com.sciuro.feature.kanban.ui.KanbanScreen
import com.sciuro.feature.budgets.ui.BudgetsScreen
import com.sciuro.feature.debt.ui.DebtOverviewScreen
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import org.koin.androidx.compose.koinViewModel
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext

import android.os.Build
import android.Manifest
import android.os.PowerManager
import android.provider.Settings as SystemSettings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.najmi.sciuro.worker.ReviewReminderWorker
import java.util.concurrent.TimeUnit
import com.sciuro.core.ledger.config.SettingsProvider
import org.koin.compose.koinInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.withContext

sealed class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Dashboard : NavItem("dashboard", "Home", Icons.Filled.Home)
    data object Kanban : NavItem("kanban", "Tasks", Icons.Filled.Assignment)
    data object Wallet : NavItem("wallet", "Wallet", Icons.Filled.AccountBalanceWallet)
    data object Budgets : NavItem("budgets", "Budgets", Icons.Filled.PieChart)
    data object Settings : NavItem("settings", "Settings", Icons.Filled.Settings)
}

class MainActivity : FragmentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        setupWorkers()

        setContent {
            SciuroTheme {
                val settingsProvider: SettingsProvider = koinInject()
                val lockEnabled = settingsProvider.isLockEnabled()
                BiometricGate(activity = this@MainActivity, lockEnabled = lockEnabled) {
                    SciuroMainScreen()
                }
            }
        }
    }
    
    private fun setupWorkers() {
        val reminderWork = PeriodicWorkRequestBuilder<ReviewReminderWorker>(30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ReviewReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )
    }
}

@Composable
fun SciuroMainScreen() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val items = listOf(
        NavItem.Dashboard,
        NavItem.Kanban,
        NavItem.Wallet,
        NavItem.Budgets,
        NavItem.Settings
    )
    
    val context = LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
        )
    }

    if (!hasPermission) {
        // Show Onboarding Permission Screen
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                com.najmi.sciuro.core.ui.components.EmptyStateView(
                    message = "Sciuro needs access to your notifications to passively track your bank and e-wallet transactions.",
                    primaryCtaText = "Grant Permission",
                    onPrimaryCtaClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        hasPermission = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                            ?.contains(context.packageName) == true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("I've granted permission")
                }
            }
        }
        return
    }

    val settingsProvider: com.sciuro.core.ledger.config.SettingsProvider = org.koin.compose.koinInject()
    var isBatteryStepComplete by remember {
        mutableStateOf(
            settingsProvider.hasSeenBatteryPrompt() || if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    if (!isBatteryStepComplete) {
        val guideSteps = com.najmi.sciuro.core.ui.util.OemAutostartHelper.getGuideSteps()
        val isAggressiveOem = com.najmi.sciuro.core.ui.util.OemAutostartHelper.isKnownAggressiveOem()
        val autostartIntent = com.najmi.sciuro.core.ui.util.OemAutostartHelper.getAutostartIntent()
        com.sciuro.feature.wallet.ui.OnboardingBatteryScreen(
            guideSteps = guideSteps,
            isAggressiveOem = isAggressiveOem,
            autostartIntent = autostartIntent,
            onComplete = { 
                settingsProvider.setHasSeenBatteryPrompt(true)
                isBatteryStepComplete = true 
            },
            onSkip = { 
                settingsProvider.setHasSeenBatteryPrompt(true)
                isBatteryStepComplete = true 
            }
        )
        return
    }
    
    val onboardingViewModel: com.sciuro.feature.wallet.viewmodel.OnboardingViewModel = org.koin.androidx.compose.koinViewModel()
    val onboardingState by onboardingViewModel.state.collectAsState()
    
    if (onboardingState.isLoading) {
        // Show blank or loading while checking database
        return
    }
    
    val startDest = if (onboardingState.isOnboardingComplete) "dashboard" else "onboarding"
    
    LaunchedEffect(onboardingState.isOnboardingComplete) {
        if (onboardingState.isOnboardingComplete && navController.currentDestination?.route == "onboarding") {
            navController.navigate("dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    val activity = context as? Activity
    val intentOpenTab = activity?.intent?.getStringExtra("open_tab")
    val intentDeveloperTab = activity?.intent?.getStringExtra("developer_tab")
    LaunchedEffect(intentOpenTab, intentDeveloperTab) {
        if (!onboardingState.isOnboardingComplete) return@LaunchedEffect
        if (intentOpenTab == "settings") {
            val tabMap = mapOf(
                "simulator" to 0, "sources" to 1, "ingestion" to 2,
                "diagnostics" to 3, "data_tools" to 4, "health" to 5, "pipeline_trace" to 6
            )
            val tab = tabMap[intentDeveloperTab] ?: 0
            navController.navigate("developer_settings?initialTab=$tab") {
                popUpTo("dashboard") { inclusive = false }
            }
            activity?.intent?.removeExtra("open_tab")
            activity?.intent?.removeExtra("developer_tab")
        }
    }
    
    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        val showNav = onboardingState.isOnboardingComplete

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showNav) {
                    val isDarkTheme = isSystemInDarkTheme()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val selectedPillColor = if (isDarkTheme) Color.White else Color.Black
                    val selectedContentColor = if (isDarkTheme) Color.Black else Color.White
                    val unselectedContentColor = if (isDarkTheme) Color(0xFFBBBBBB) else Color(0xFF444444)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                                .fillMaxWidth()
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(100.dp)),
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                items.forEach { item ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(
                                                if (isSelected) selectedPillColor else Color.Transparent
                                            )
                                            .clickable {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                tint = if (isSelected) selectedContentColor else unselectedContentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            AnimatedVisibility(visible = isSelected) {
                                                Text(
                                                    text = item.label,
                                                    color = selectedContentColor,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val lateralEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                fadeIn(tween(SciuroMotion.TRANSITION_DURATION_MS)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SciuroMotion.TRANSITION_DURATION_MS))
            }
            val lateralExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                fadeOut(tween(SciuroMotion.TRANSITION_DURATION_MS)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SciuroMotion.TRANSITION_DURATION_MS))
            }
            val drillInEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SciuroMotion.TRANSITION_DURATION_MS))
            }
            val drillInPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SciuroMotion.TRANSITION_DURATION_MS))
            }

            NavHost(
                navController, 
                startDestination = startDest, 
                modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
                enterTransition = lateralEnter,
                exitTransition = lateralExit
            ) {
            composable("onboarding") {
                com.sciuro.feature.wallet.ui.OnboardingScreen(
                    viewModel = onboardingViewModel
                )
            }
            composable("dashboard") { DashboardScreen() }
            composable("wallet") { 
                WalletScreen(onAccountClick = { accountId ->
                    navController.navigate("account_detail/$accountId")
                }) 
            }
            composable(
                "account_detail/{accountId}",
                arguments = listOf(androidx.navigation.navArgument("accountId") { type = androidx.navigation.NavType.StringType }),
                enterTransition = drillInEnter,
                popExitTransition = drillInPopExit
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: return@composable
                com.sciuro.feature.wallet.ui.AccountDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("budgets") {
                com.sciuro.feature.budgets.ui.BudgetsScreen(
                    onNavigateToCategoryDrilldown = { navController.navigate("category_drilldown") }
                )
            }
            composable(
                "category_drilldown",
                enterTransition = drillInEnter,
                popExitTransition = drillInPopExit
            ) {
                com.sciuro.feature.budgets.ui.CategoryDrilldownScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("debt_overview") { DebtOverviewScreen() }
            composable("kanban") { KanbanScreen() }
            composable("settings") { 
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                com.sciuro.feature.settings.ui.SettingsScreen(
                    onNavigateToDeveloperSettings = { navController.navigate("developer_settings?initialTab=0") },
                    onNavigateToCategorySettings = { navController.navigate("category_settings") },
                    onNavigateToLinkedAccounts = { navController.navigate("linked_accounts") },
                    onExportBackup = { password ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = java.io.File(context.cacheDir, "sciuro_backup_${System.currentTimeMillis()}.scib")
                                val outputStream = java.io.FileOutputStream(tempFile)
                                val result = com.najmi.sciuro.export.EncryptedExporter.export(context, password, outputStream)
                                outputStream.close()
                                if (result.isSuccess) {
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
                    },
                    onImportBackup = { uri, password ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    val result = com.najmi.sciuro.export.EncryptedImporter.import(context, password, inputStream)
                                    inputStream.close()
                                    withContext(Dispatchers.Main) {
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_LONG).show()
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
                    }
                ) 
            }
            composable(
                "category_settings",
                enterTransition = drillInEnter,
                popExitTransition = drillInPopExit
            ) { 
                com.sciuro.feature.settings.ui.CategorySettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
            composable(
                "developer_settings?initialTab={initialTab}",
                arguments = listOf(navArgument("initialTab") { defaultValue = "0" }),
                enterTransition = drillInEnter,
                popExitTransition = drillInPopExit
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments?.getString("initialTab")?.toIntOrNull() ?: 0
                com.sciuro.feature.settings.ui.DeveloperSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    initialTab = initialTab
                )
            }
            composable(
                "linked_accounts",
                enterTransition = drillInEnter,
                popExitTransition = drillInPopExit
            ) {
                val linkedAccountsViewModel: com.sciuro.feature.settings.viewmodel.LinkedAccountsViewModel = koinViewModel()
                com.sciuro.feature.settings.ui.LinkedAccountsScreen(
                    viewModel = linkedAccountsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
        }
    }
    }
    }
}

