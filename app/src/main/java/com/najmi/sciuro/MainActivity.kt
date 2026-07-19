package com.najmi.sciuro

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.najmi.sciuro.core.ui.theme.SciuroTheme
import com.sciuro.feature.dashboard.ui.DashboardScreen
import com.sciuro.feature.wallet.ui.WalletScreen
import com.sciuro.feature.kanban.ui.KanbanScreen
import com.sciuro.feature.budgets.ui.BudgetsScreen
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SciuroTheme {
                SciuroMainScreen()
            }
        }
    }
}

@Composable
fun SciuroMainScreen() {
    val navController = rememberNavController()
    
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
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
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
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "dashboard", Modifier.padding(innerPadding)) {
            composable("dashboard") { DashboardScreen() }
            composable("wallet") { WalletScreen() }
            composable("budgets") { BudgetsScreen() }
            composable("kanban") { KanbanScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
