package com.sciuro.feature.wallet.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: WalletViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsState()
    val context = LocalContext.current
    
    var selectedAssetType by rememberSaveable { mutableStateOf("Liquid Cash") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Dialog Form State
    var newAccountName by rememberSaveable { mutableStateOf("") }
    var newAccountType by rememberSaveable { mutableStateOf("Bank Account") }
    var newAccountPackage by rememberSaveable { mutableStateOf("") }
    var newAccountBalance by rememberSaveable { mutableStateOf("") }
    
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = packages.mapNotNull { info ->
                // Only include apps that can be launched by the user (avoids system noise)
                if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                    val appName = pm.getApplicationLabel(info).toString()
                    val icon = pm.getApplicationIcon(info)
                    AppInfo(appName, info.packageName, icon)
                } else null
            }.sortedBy { it.name }
            installedApps = appList
        }
    }
    
    // In a real app, calculate actual totals for invested as well
    val totalLiquidity = accounts.sumOf { it.balance }
    val displayTotal = if (selectedAssetType == "Liquid Cash") totalLiquidity else 0.0 // Mock investment
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeroPanel(
                title = "Total $selectedAssetType",
                heroFigure = "RM ${"%.2f".format(displayTotal)}",
                toggleOptions = listOf("Liquid Cash", "Investments"),
                selectedToggle = selectedAssetType,
                onToggleSelected = { selectedAssetType = it }
            )
        }
        
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedAssetType == "Liquid Cash") {
                        if (accounts.isEmpty()) {
                            com.najmi.sciuro.core.ui.components.EmptyStateView(
                                message = "No cash tracked yet. Withdraw from an ATM and it'll show up here automatically."
                            )
                        } else {
                            accounts.forEach { account ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        editingAccountId = account.id
                                        newAccountName = account.name
                                        newAccountType = if (account.isEWallet) "E-Wallet" else "Bank Account"
                                        newAccountPackage = account.associatedPackage ?: ""
                                        newAccountBalance = account.balance.toString()
                                        showAddDialog = true
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Dynamic App Icon or Fallback
                                            val associatedApp = installedApps.find { it.packageName == account.associatedPackage }
                                            if (associatedApp != null) {
                                                Image(
                                                    bitmap = associatedApp.icon.toBitmap().asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (account.isEWallet) Icons.Filled.AccountBalanceWallet else Icons.Filled.AccountBalance,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            Column {
                                                Text(account.name, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    if (account.isEWallet) "E-Wallet" else "Bank Account", 
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            "RM ${"%.2f".format(account.balance)}", 
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = com.najmi.sciuro.core.ui.theme.IBMPlexMono
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Investments empty state
                        com.najmi.sciuro.core.ui.components.EmptyStateView(
                            message = "No investments tracked yet."
                        )
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { 
                editingAccountId = null
                newAccountName = ""
                newAccountType = "Bank Account"
                newAccountPackage = ""
                newAccountBalance = ""
                showAddDialog = true 
            },
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Account")
        }
    }

    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (editingAccountId == null) "Add Account" else "Edit Account",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = newAccountName,
                    onValueChange = { newAccountName = it },
                    label = { Text("Account Name (e.g. Maybank)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedApp = installedApps.find { it.packageName == newAccountPackage }
                    val displayValue = selectedApp?.name ?: newAccountPackage
                    
                    OutlinedTextField(
                        value = displayValue,
                        onValueChange = { newAccountPackage = it }, // Fallback for manual typing
                        label = { Text("Associated App (Optional)") },
                        placeholder = { Text("Search apps...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    
                    val filteredApps = installedApps.filter { 
                        it.name.contains(newAccountPackage, ignoreCase = true) || it.packageName.contains(newAccountPackage, ignoreCase = true)
                    }.take(30) // Cap to 30 to prevent UI lag on large dropdowns
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredApps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.name) },
                                leadingIcon = {
                                    Image(
                                        bitmap = app.icon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp).clip(CircleShape)
                                    )
                                },
                                onClick = {
                                    newAccountPackage = app.packageName
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = newAccountBalance,
                    onValueChange = { newAccountBalance = it },
                    label = { Text("Initial Balance (RM)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Simple toggle for Bank vs E-Wallet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = newAccountType == "Bank Account",
                        onClick = { newAccountType = "Bank Account" },
                        label = { Text("Bank") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = newAccountType == "E-Wallet",
                        onClick = { newAccountType = "E-Wallet" },
                        label = { Text("E-Wallet") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (editingAccountId != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteAccount(editingAccountId!!)
                                showAddDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    
                    Button(
                        onClick = {
                            val balance = newAccountBalance.toDoubleOrNull() ?: 0.0
                            if (editingAccountId == null) {
                                viewModel.addAccount(
                                    name = newAccountName,
                                    type = newAccountType,
                                    associatedPackage = newAccountPackage,
                                    initialBalance = balance
                                )
                            } else {
                                viewModel.updateAccount(
                                    id = editingAccountId!!,
                                    name = newAccountName,
                                    type = newAccountType,
                                    associatedPackage = newAccountPackage,
                                    balance = balance
                                )
                            }
                            showAddDialog = false
                        },
                        modifier = Modifier.weight(if (editingAccountId != null) 1f else 2f),
                        enabled = newAccountName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
