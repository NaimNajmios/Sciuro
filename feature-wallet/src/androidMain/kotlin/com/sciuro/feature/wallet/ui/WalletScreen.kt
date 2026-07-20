package com.sciuro.feature.wallet.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WalletScreen(
    onAccountClick: (String) -> Unit,
    viewModel: WalletViewModel = koinViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val investments by viewModel.investments.collectAsState()
    val context = LocalContext.current
    
    var selectedAssetType by rememberSaveable { mutableStateOf("Liquid Cash") }
    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showAddInvestmentDialog by rememberSaveable { mutableStateOf(false) }
    
    // Account Form State
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var newAccountName by rememberSaveable { mutableStateOf("") }
    var newAccountType by rememberSaveable { mutableStateOf("Bank Account") }
    var newAccountPackage by rememberSaveable { mutableStateOf("") }
    var newAccountBalance by rememberSaveable { mutableStateOf("") }
    
    // Investment Form State
    var editingInvestmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var newAssetType by rememberSaveable { mutableStateOf("Stock") }
    var newAssetSymbol by rememberSaveable { mutableStateOf("") }
    var newAssetName by rememberSaveable { mutableStateOf("") }
    var newUnitsHeld by rememberSaveable { mutableStateOf("") }
    var newAvgBuyPrice by rememberSaveable { mutableStateOf("") }
    var newAssociatedAccountId by rememberSaveable { mutableStateOf("") }
    
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = packages.mapNotNull { info ->
                if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                    val appName = pm.getApplicationLabel(info).toString()
                    val icon = pm.getApplicationIcon(info)
                    AppInfo(appName, info.packageName, icon)
                } else null
            }.sortedBy { it.name }
            installedApps = appList
        }
    }
    
    val totalLiquidity = accounts.sumOf { it.balance }
    val totalInvestments = investments.sumOf { it.unitsHeld * it.averageBuyPrice }
    val displayTotal = if (selectedAssetType == "Liquid Cash") totalLiquidity else totalInvestments
    
    val allTransactions by viewModel.allTransactions.collectAsState()
    
    val accountPagerState = rememberPagerState(pageCount = { maxOf(1, accounts.size) })
    val investmentPagerState = rememberPagerState(pageCount = { maxOf(1, investments.size) })
    
    val currentAccountPage = accountPagerState.currentPage
    
    val activeAccount = accounts.getOrNull(currentAccountPage)
    val accountTx = allTransactions.filter { it.account_id == activeAccount?.id }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(bottom = 24.dp)) {
                    SegmentedButton(
                        selected = selectedAssetType == "Liquid Cash",
                        onClick = { selectedAssetType = "Liquid Cash" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Liquid Cash")
                    }
                    SegmentedButton(
                        selected = selectedAssetType == "Investments",
                        onClick = { selectedAssetType = "Investments" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Investments")
                    }
                }
                
                if (selectedAssetType == "Liquid Cash") {
                    if (accounts.isEmpty()) {
                        Text("No accounts found", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        HorizontalPager(
                            state = accountPagerState,
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            val account = accounts.getOrNull(page)
                            if (account != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onAccountClick(account.id) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = MaterialTheme.shapes.extraLarge
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(account.name, style = MaterialTheme.typography.titleLarge)
                                            val associatedApp = installedApps.find { it.packageName == account.associatedPackage }
                                            if (associatedApp != null) {
                                                Image(
                                                    bitmap = associatedApp.icon.toBitmap().asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (account.isEWallet) Icons.Filled.AccountBalanceWallet else Icons.Filled.AccountBalance,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        Text(
                                            "RM ${"%.2f".format(account.balance)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontFamily = com.najmi.sciuro.core.ui.theme.IBMPlexMono
                                        )
                                        Text(
                                            if (account.isEWallet) "E-Wallet" else "Bank Account",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (investments.isEmpty()) {
                        Text("No investments found", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        HorizontalPager(
                            state = investmentPagerState,
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            val inv = investments.getOrNull(page)
                            if (inv != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(180.dp).clickable {
                                        editingInvestmentId = inv.id
                                        newAssetType = inv.assetType
                                        newAssetSymbol = inv.assetSymbol
                                        newAssetName = inv.assetName
                                        newUnitsHeld = inv.unitsHeld.toString()
                                        newAvgBuyPrice = inv.averageBuyPrice.toString()
                                        newAssociatedAccountId = inv.associatedAccountId ?: ""
                                        showAddInvestmentDialog = true
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = MaterialTheme.shapes.extraLarge
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(inv.assetSymbol, style = MaterialTheme.typography.titleLarge)
                                            Icon(
                                                imageVector = if (inv.assetType == "Gold") Icons.Filled.Toll else Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null
                                            )
                                        }
                                        val valNow = inv.unitsHeld * inv.averageBuyPrice
                                        Text(
                                            "RM ${"%.2f".format(valNow)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontFamily = com.najmi.sciuro.core.ui.theme.IBMPlexMono
                                        )
                                        Text(
                                            "${inv.assetName} • ${inv.unitsHeld} units",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    
                    if (selectedAssetType == "Liquid Cash" && accounts.isNotEmpty()) {
                        if (activeAccount != null) {
                            if (accountTx.isEmpty()) {
                                com.najmi.sciuro.core.ui.components.EmptyStateView(message = "No transactions for this account.")
                            } else {
                                accountTx.take(20).forEach { tx ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                                                Icon(
                                                    imageVector = if (tx.direction == "INFLOW") Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = if (tx.direction == "INFLOW") Color(0xFF4CAF50) else Color(0xFFE53935)
                                                )
                                                Column {
                                                    Text(tx.merchant ?: "Unknown Merchant", style = MaterialTheme.typography.titleMedium)
                                                    Text(if (tx.is_reviewed == 1L) "Reviewed" else "Unreviewed", style = MaterialTheme.typography.bodySmall, color = if (tx.is_reviewed == 1L) Color.Gray else MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Text(
                                                "RM ${"%.2f".format(tx.amount)}", 
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (tx.direction == "INFLOW") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedAssetType == "Investments" && investments.isNotEmpty()) {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(message = "Investment transactions are currently tracked manually.")
                    } else {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(message = "No data available.")
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { 
                if (selectedAssetType == "Liquid Cash") {
                    editingAccountId = null
                    newAccountName = ""
                    newAccountType = "Bank Account"
                    newAccountPackage = ""
                    newAccountBalance = ""
                    showAddAccountDialog = true 
                } else {
                    editingInvestmentId = null
                    newAssetType = "Stock"
                    newAssetSymbol = ""
                    newAssetName = ""
                    newUnitsHeld = ""
                    newAvgBuyPrice = ""
                    newAssociatedAccountId = ""
                    showAddInvestmentDialog = true
                }
            },
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }

    if (showAddAccountDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddAccountDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .imePadding()
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
                        onValueChange = { newAccountPackage = it },
                        label = { Text("Associated App (Optional)") },
                        placeholder = { Text("Search apps...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    
                    val filteredApps = installedApps.filter { 
                        it.name.contains(newAccountPackage, ignoreCase = true) || it.packageName.contains(newAccountPackage, ignoreCase = true)
                    }.take(30)
                    
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = newAccountType == "Bank Account",
                            onClick = { newAccountType = "Bank Account" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Bank Account")
                        }
                        SegmentedButton(
                            selected = newAccountType == "E-Wallet",
                            onClick = { newAccountType = "E-Wallet" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("E-Wallet")
                        }
                    }
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
                                showAddAccountDialog = false
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
                            showAddAccountDialog = false
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

    if (showAddInvestmentDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddInvestmentDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .imePadding()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (editingInvestmentId == null) "Add Investment" else "Edit Investment",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = newAssetType == "Stock",
                            onClick = { newAssetType = "Stock" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Stock")
                        }
                        SegmentedButton(
                            selected = newAssetType == "Gold",
                            onClick = { newAssetType = "Gold" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Gold")
                        }
                    }
                }
                
                if (newAssetType == "Stock") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newAssetSymbol,
                            onValueChange = { newAssetSymbol = it.uppercase() },
                            label = { Text("Symbol") },
                            placeholder = { Text("e.g. AAPL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newAssetName,
                            onValueChange = { newAssetName = it },
                            label = { Text("Name") },
                            placeholder = { Text("e.g. Apple Inc.") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = newAssetName,
                        onValueChange = { newAssetName = it },
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Maybank Gold Account") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newUnitsHeld,
                        onValueChange = { newUnitsHeld = it },
                        label = { Text(if (newAssetType == "Gold") "Grams Held" else "Units Held") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newAvgBuyPrice,
                        onValueChange = { newAvgBuyPrice = it },
                        label = { Text("Avg Price (RM)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    val selectedAccount = accounts.find { it.id == newAssociatedAccountId }
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Funding Account (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                newAssociatedAccountId = ""
                                accountExpanded = false
                            }
                        )
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    newAssociatedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (editingInvestmentId != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteInvestment(editingInvestmentId!!)
                                showAddInvestmentDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    
                    Button(
                        onClick = {
                            val units = newUnitsHeld.toDoubleOrNull() ?: 0.0
                            val price = newAvgBuyPrice.toDoubleOrNull() ?: 0.0
                            val finalSymbol = if (newAssetType == "Gold") "XAU" else newAssetSymbol
                            
                            if (editingInvestmentId == null) {
                                viewModel.addInvestment(
                                    assetSymbol = finalSymbol,
                                    assetName = newAssetName,
                                    assetType = newAssetType,
                                    unitsHeld = units,
                                    averageBuyPrice = price,
                                    associatedAccountId = newAssociatedAccountId
                                )
                            } else {
                                viewModel.updateInvestment(
                                    id = editingInvestmentId!!,
                                    assetSymbol = finalSymbol,
                                    assetName = newAssetName,
                                    assetType = newAssetType,
                                    unitsHeld = units,
                                    averageBuyPrice = price,
                                    associatedAccountId = newAssociatedAccountId
                                )
                            }
                            showAddInvestmentDialog = false
                        },
                        modifier = Modifier.weight(if (editingInvestmentId != null) 1f else 2f),
                        enabled = (newAssetType == "Gold" || newAssetSymbol.isNotBlank()) && newAssetName.isNotBlank() && newUnitsHeld.isNotBlank() && newAvgBuyPrice.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
