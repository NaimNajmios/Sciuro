package com.najmi.sciuro

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.compose.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.najmi.sciuro.core.ui.theme.SciuroTheme
import com.najmi.sciuro.core.ui.theme.SciuroMotion
import com.najmi.sciuro.navigation.SciuroNavGraph
import com.najmi.sciuro.navigation.SciuroRoute
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
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
import kotlinx.coroutines.launch

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
    
    val startDest = if (onboardingState.isOnboardingComplete) SciuroRoute.Dashboard.route else SciuroRoute.Onboarding.route
    
    LaunchedEffect(onboardingState.isOnboardingComplete) {
        if (onboardingState.isOnboardingComplete && navController.currentDestination?.route == SciuroRoute.Onboarding.route) {
            navController.navigate(SciuroRoute.Dashboard.route) {
                popUpTo(SciuroRoute.Onboarding.route) { inclusive = true }
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
            navController.navigate(SciuroRoute.DeveloperSettings.createRoute(tab)) {
                popUpTo(SciuroRoute.Dashboard.route) { inclusive = false }
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
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val selectedPillColor = MaterialTheme.colorScheme.primary
                    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
                    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant

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
                                    val weight by animateFloatAsState(
                                        targetValue = if (isSelected) 1.5f else 1f,
                                        animationSpec = tween(SciuroMotion.TRANSITION_DURATION_MS)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
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
                                            AnimatedVisibility(
                                                visible = isSelected,
                                                enter = fadeIn(tween(SciuroMotion.TRANSITION_DURATION_MS)) + expandHorizontally(tween(SciuroMotion.TRANSITION_DURATION_MS)),
                                                exit = fadeOut(tween(SciuroMotion.TRANSITION_DURATION_MS)) + shrinkHorizontally(tween(SciuroMotion.TRANSITION_DURATION_MS))
                                            ) {
                                                Text(
                                                    text = item.label,
                                                    color = selectedContentColor,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(start = 6.dp)
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
            SciuroNavGraph(
                startDestination = startDest,
                onboardingViewModel = onboardingViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            )
    }
    }
}

