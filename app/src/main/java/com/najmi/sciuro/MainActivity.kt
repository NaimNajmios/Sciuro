package com.najmi.sciuro

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.najmi.sciuro.core.ui.theme.SciuroTheme
import com.najmi.sciuro.core.ui.theme.SciuroMotion
import com.sciuro.feature.dashboard.ui.DashboardScreen
import com.sciuro.feature.wallet.ui.WalletScreen
import com.sciuro.feature.kanban.ui.KanbanScreen
import com.sciuro.feature.budgets.ui.BudgetsScreen
import com.sciuro.feature.debt.ui.DebtOverviewScreen
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Settings

import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.najmi.sciuro.worker.IngestionReconciliationWorker
import com.najmi.sciuro.worker.ReviewReminderWorker
import java.util.concurrent.TimeUnit
import com.sciuro.core.ledger.config.SettingsProvider
import org.koin.compose.koinInject

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

        val reconciliationWork = PeriodicWorkRequestBuilder<IngestionReconciliationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "IngestionReconciliation",
            ExistingPeriodicWorkPolicy.KEEP,
            reconciliationWork
        )
    }
}

@Composable
fun SciuroMainScreen() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val items = listOf(
        Pair("dashboard", Icons.Filled.Home),
        Pair("kanban", Icons.Filled.List),
        Pair("wallet", Icons.Filled.ShoppingCart),
        Pair("budgets", Icons.Filled.CheckCircle),
        Pair("settings", Icons.Filled.Settings)
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
    
    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
            if (onboardingState.isOnboardingComplete) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.second, contentDescription = screen.first) },
                        label = { Text(screen.first.replaceFirstChar { it.uppercase() }) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.first } == true,
                        onClick = {
                            navController.navigate(screen.first) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.6f),
                            unselectedTextColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.6f)
                        )
                    )
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
            Modifier.padding(innerPadding),
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
                // We'll pass the SavedStateHandle to the koinViewModel by defining it in the Koin module, 
                // but for now we just load the screen. Koin handles SavedStateHandle injection automatically.
                com.sciuro.feature.wallet.ui.AccountDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("budgets") { BudgetsScreen() }
            composable("debt_overview") { DebtOverviewScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("kanban") { KanbanScreen() }
            composable("settings") { 
                com.sciuro.feature.settings.ui.SettingsScreen(
                    onNavigateToDeveloperSettings = { navController.navigate("developer_settings") },
                    onNavigateToCategorySettings = { navController.navigate("category_settings") }
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
                "developer_settings",
                enterTransition = drillInEnter,
                popExitTransition = drillInPopExit
            ) { 
                com.sciuro.feature.settings.ui.DeveloperSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
        }
    }
    }
}

