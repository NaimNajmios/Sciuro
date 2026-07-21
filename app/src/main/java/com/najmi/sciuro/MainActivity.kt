package com.najmi.sciuro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.najmi.sciuro.core.ui.theme.SciuroTheme
import com.sciuro.feature.dashboard.ui.DashboardScreen
import com.sciuro.feature.wallet.ui.WalletScreen
import com.sciuro.feature.kanban.ui.KanbanScreen
import com.sciuro.feature.budgets.ui.BudgetsScreen
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Settings

import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.najmi.sciuro.worker.ReviewReminderWorker
import java.util.concurrent.TimeUnit

private data class NavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    
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
                SciuroMainScreen()
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
        NavItem("dashboard", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("kanban", Icons.Filled.List, Icons.Outlined.List),
        NavItem("wallet", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
        NavItem("budgets", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
        NavItem("settings", Icons.Filled.Settings, Icons.Outlined.Settings)
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
    
    if (!onboardingState.isOnboardingComplete) {
        com.sciuro.feature.wallet.ui.OnboardingScreen(viewModel = onboardingViewModel)
        return
    }
    
    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.route
                            )
                        },
                        label = {
                            Text(
                                text = item.route.replaceFirstChar { it.uppercase() },
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (selected) 13.sp else 12.sp
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "dashboard", Modifier.padding(innerPadding)) {
            composable("dashboard") { DashboardScreen() }
            composable("wallet") { 
                WalletScreen(onAccountClick = { accountId ->
                    navController.navigate("account_detail/$accountId")
                }) 
            }
            composable(
                "account_detail/{accountId}",
                arguments = listOf(androidx.navigation.navArgument("accountId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: return@composable
                // We'll pass the SavedStateHandle to the koinViewModel by defining it in the Koin module, 
                // but for now we just load the screen. Koin handles SavedStateHandle injection automatically.
                com.sciuro.feature.wallet.ui.AccountDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("budgets") { BudgetsScreen() }
            composable("kanban") { KanbanScreen() }
            composable("settings") { 
                com.sciuro.feature.settings.ui.SettingsScreen(
                    onNavigateToDeveloperSettings = { navController.navigate("developer_settings") }
                ) 
            }
            composable("developer_settings") { 
                com.sciuro.feature.settings.ui.DeveloperSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
        }
    }
    }
}
