package com.najmi.sciuro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.najmi.sciuro.ui.theme.SciuroTheme
import com.sciuro.feature.dashboard.ui.DashboardScreen
import com.sciuro.feature.wallet.ui.WalletScreen
import com.sciuro.feature.kanban.ui.KanbanScreen
import com.sciuro.feature.budgets.ui.BudgetsScreen

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
        Pair("wallet", Icons.Filled.ShoppingCart),
        Pair("budgets", Icons.Filled.CheckCircle),
        Pair("kanban", Icons.Filled.List)
    )
    
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
        }
    }
}