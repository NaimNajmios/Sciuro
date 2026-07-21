package com.sciuro.feature.wallet.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
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
import com.najmi.sciuro.core.ui.components.AdjustmentCard
import com.najmi.sciuro.core.ui.components.AdjustmentBottomSheet
import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.AdjustmentReasonPresets
import com.najmi.sciuro.core.ui.components.PillToggle
import kotlinx.coroutines.launch
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
    
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteInvestmentDialog by remember { mutableStateOf(false) }
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
    var newAccountColor by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Investment Form State
    var editingInvestmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var newAssetType by rememberSaveable { mutableStateOf("Stock") }
    var newAssetSymbol by rememberSaveable { mutableStateOf("") }
    var newAssetName by rememberSaveable { mutableStateOf("") }
    var newUnitsHeld by rememberSaveable { mutableStateOf("") }
    var newAvgBuyPrice by rememberSaveable { mutableStateOf("") }
    var newAssociatedAccountId by rememberSaveable { mutableStateOf("") }
    
    // Recount State
    var showRecountDialog by rememberSaveable { mutableStateOf(false) }
    var recountAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var recountDeclaredBalance by rememberSaveable { mutableStateOf("") }
    var recountReason by rememberSaveable { mutableStateOf("") }
    var recountReasonExpanded by remember { mutableStateOf(false) }

    // Transaction list filter
    var txFilter by rememberSaveable { mutableStateOf("All") }

    // Transaction Edit State
    var showEditTransactionDialog by rememberSaveable { mutableStateOf(false) }
    var editingTxId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTxAmount by rememberSaveable { mutableStateOf("") }
    var editTxMerchant by rememberSaveable { mutableStateOf("") }
    var editTxAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTxDirection by rememberSaveable { mutableStateOf("OUTFLOW") }
    var editTxCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
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
    val allAdjustments by viewModel.allAdjustments.collectAsState()
    
    val accountPagerState = rememberPagerState(pageCount = { maxOf(1, accounts.size) })
    val investmentPagerState = rememberPagerState(pageCount = { maxOf(1, investments.size) })
    
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.najmi.sciuro.core.ui.theme.SurfaceHero)
                .padding(top = 48.dp, bottom = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = if (selectedAssetType == "Liquid Cash") "Total Liquidity" else "Total Investments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RM ${"%.2f".format(displayTotal)}",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontFamily = com.najmi.sciuro.core.ui.theme.IBMPlexMono
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            com.najmi.sciuro.core.ui.components.PillToggle(
                options = listOf("Liquid Cash", "Investments"),
                selectedOption = selectedAssetType,
                onOptionSelected = { selectedAssetType = it },
                isOnDarkSurface = true,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (selectedAssetType == "Liquid Cash") {
                if (accounts.isEmpty()) {
                    com.najmi.sciuro.core.ui.components.EmptyStateView(
                        message = "No accounts yet — add your first wallet to start tracking.",
                        primaryCtaText = "Add Account",
                        onPrimaryCtaClick = {
                            editingAccountId = null
                            newAccountName = ""
                            newAccountType = "Bank Account"
                            newAccountPackage = ""
                            newAccountBalance = ""
                            showAddAccountDialog = true
                        }
                    )
                } else {
                    HorizontalPager(
                        state = accountPagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        val account = accounts.getOrNull(page)
                        if (account != null) {
                            val containerCol = if (account.color != null) {
                                try { Color(android.graphics.Color.parseColor(account.color)) } catch(e: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            val contentCol = if (account.color != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                colors = CardDefaults.cardColors(containerColor = containerCol, contentColor = contentCol),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(24.dp).clickable { onAccountClick(account.id) },
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (account.isEWallet) "E-Wallet" else "Bank Account",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentCol.copy(alpha = 0.7f)
                                        )
                                        IconButton(
                                            onClick = {
                                                recountAccountId = account.id
                                                recountDeclaredBalance = ""
                                                recountReason = ""
                                                showRecountDialog = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Calculate,
                                                contentDescription = "Recount",
                                                tint = contentCol.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (investments.isEmpty()) {
                    com.najmi.sciuro.core.ui.components.EmptyStateView(
                        message = "No investments yet — add your first investment to start tracking.",
                        primaryCtaText = "Add Investment",
                        onPrimaryCtaClick = {
                            editingInvestmentId = null
                            newAssetType = "Stock"
                            newAssetSymbol = ""
                            newAssetName = ""
                            newUnitsHeld = ""
                            newAvgBuyPrice = ""
                            newAssociatedAccountId = ""
                            showAddInvestmentDialog = true
                        }
                    )
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
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
                                }
                            }
                        }
                    }
                }
            }
        }
        
        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            ) {
                PillToggle(
                    options = listOf("All", "Transactions", "Adjustments"),
                    selectedOption = txFilter,
                    onOptionSelected = { txFilter = it },
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true
                )
            }
            
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 16.dp))
                }
                    
                    if (selectedAssetType == "Liquid Cash" && accounts.isNotEmpty()) {
                        val currentAccountPage = accountPagerState.currentPage
                        val activeAccount = accounts.getOrNull(currentAccountPage)
                        val accountTx = if (activeAccount != null) allTransactions.filter { it.account_id == activeAccount.id } else emptyList()

                        val accountAdjustments = if (activeAccount != null) allAdjustments.filter { it.account_id == activeAccount.id } else emptyList()

                        if (activeAccount != null) {
                            if (txFilter == "Adjustments") {
                                if (accountAdjustments.isEmpty()) {
                                    item { com.najmi.sciuro.core.ui.components.EmptyStateView(message = "No adjustments for this account.") }
                                } else {
                                    items(accountAdjustments.size) { idx ->
                                        val adj = accountAdjustments[idx]
                                        AdjustmentCard(
                                            reason = adj.reason,
                                            amount = adj.amount,
                                            formattedTime = "",
                                            onClick = {
                                                viewModel.recordCorrection(activeAccount.id, 0.0, adj.reason)
                                            }
                                        )
                                    }
                                }
                            } else if (accountTx.isEmpty()) {
                                item { com.najmi.sciuro.core.ui.components.EmptyStateView(message = "No transactions for this account.") }
                            } else {
                                items(accountTx.take(20).size) { idx ->
                                    val tx = accountTx[idx]
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            editingTxId = tx.id
                                            editTxAmount = tx.amount.toString()
                                            editTxMerchant = tx.merchant ?: ""
                                            editTxAccountId = tx.account_id
                                            editTxDirection = tx.direction
                                            editTxCategoryId = tx.category_id
                                            showEditTransactionDialog = true
                                        },
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
                                                    Text(
                                                        tx.merchant ?: "Unknown Merchant",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        if (tx.is_reviewed == 1L) "Reviewed" else "Unreviewed",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (tx.is_reviewed == 1L) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error
                                                    )
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
                    } else if (selectedAssetType == "Investments") {
                        item { com.najmi.sciuro.core.ui.components.EmptyStateView(message = "Investment transactions are currently tracked manually.") }
                    } else {
                        item { com.najmi.sciuro.core.ui.components.EmptyStateView(message = "No data available.") }
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
                    }
                    
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
                
                Spacer(modifier = Modifier.height(4.dp))
                Text("Account Color", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetColors = listOf("#4CAF50", "#2196F3", "#F44336", "#9C27B0", "#FF9800", "#607D8B", "#1A1A1A", "#795548")
                    items(presetColors.size) { i ->
                        val hex = presetColors[i]
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { newAccountColor = hex }
                                .padding(2.dp)
                        ) {
                            if (newAccountColor == hex) {
                                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                            }
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
                                showDeleteAccountDialog = true
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
                                    initialBalance = balance,
                                    color = newAccountColor
                                )
                            } else {
                                viewModel.updateAccount(
                                    id = editingAccountId!!,
                                    name = newAccountName,
                                    type = newAccountType,
                                    associatedPackage = newAccountPackage,
                                    balance = balance,
                                    color = newAccountColor
                                )
                            }
                            showAddAccountDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (editingAccountId == null) "Account created" else "Account updated")
                            }
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
                                showDeleteInvestmentDialog = true
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
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (editingInvestmentId == null) "Investment created" else "Investment updated")
                            }
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

    if (showEditTransactionDialog) {
        ModalBottomSheet(
            onDismissRequest = { showEditTransactionDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Transaction", style = MaterialTheme.typography.headlineSmall)
                
                OutlinedTextField(
                    value = editTxAmount,
                    onValueChange = { editTxAmount = it },
                    label = { Text("Amount (RM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = editTxMerchant,
                    onValueChange = { editTxMerchant = it },
                    label = { Text("Merchant / Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = editTxDirection == "OUTFLOW",
                        onClick = { editTxDirection = "OUTFLOW"; editTxCategoryId = null },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Expense")
                    }
                    SegmentedButton(
                        selected = editTxDirection == "INFLOW",
                        onClick = { editTxDirection = "INFLOW"; editTxCategoryId = null },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Income")
                    }
                }
                
                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val selAcc = accounts.find { it.id == editTxAccountId }
                    OutlinedTextField(
                        value = selAcc?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    editTxAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Text("Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val expenseCats by viewModel.expenseCategories.collectAsState()
                val incomeCats by viewModel.incomeCategories.collectAsState()
                val relevantCategories = if (editTxDirection == "OUTFLOW") expenseCats else incomeCats
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(relevantCategories) { cat ->
                        FilterChip(
                            selected = editTxCategoryId == cat.id,
                            onClick = { editTxCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteTransaction(editingTxId!!)
                            showEditTransactionDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                    
                    Button(
                        onClick = {
                            val amt = editTxAmount.toDoubleOrNull() ?: 0.0
                            viewModel.editTransaction(
                                transactionId = editingTxId!!,
                                amount = amt,
                                direction = editTxDirection,
                                merchant = editTxMerchant,
                                categoryId = editTxCategoryId,
                                accountId = editTxAccountId
                            )
                            showEditTransactionDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = editTxAmount.isNotBlank() && editTxAccountId != null
                    ) {
                        Text("Save")
                    }
                }
            }
    }

    if (showDeleteAccountDialog) {
        SciuroConfirmationDialog(
            title = "Delete Account",
            message = "Are you sure you want to delete this account? This will also remove any related transactions.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccount(editingAccountId!!)
                showDeleteAccountDialog = false
                showAddAccountDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Account deleted") }
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    if (showDeleteInvestmentDialog) {
        SciuroConfirmationDialog(
            title = "Delete Investment",
            message = "Are you sure you want to delete this investment asset? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteInvestment(editingInvestmentId!!)
                showDeleteInvestmentDialog = false
                showAddInvestmentDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Investment deleted") }
            },
            onDismiss = { showDeleteInvestmentDialog = false }
        )
    }

    if (showRecountDialog && recountAccountId != null) {
        val recountAccount = accounts.find { it.id == recountAccountId }
        val currentBalance = recountAccount?.balance ?: 0.0
        val parsedDeclared = recountDeclaredBalance.toDoubleOrNull()
        val variance = if (parsedDeclared != null) parsedDeclared - currentBalance else null

        SciuroBottomSheet(onDismissRequest = { showRecountDialog = false }) {
            Text("Recount Balance", style = MaterialTheme.typography.headlineSmall)

            Text(
                "Current Balance: RM ${"%.2f".format(currentBalance)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SciuroTextField(
                value = recountDeclaredBalance,
                onValueChange = { recountDeclaredBalance = it },
                label = "Actual Balance (RM)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (variance != null && kotlin.math.abs(variance) > 0.01) {
                val varianceColor = if (variance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                Text(
                    "Variance: ${if (variance >= 0) "+" else ""}RM ${"%.2f".format(variance)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = varianceColor,
                    fontFamily = com.najmi.sciuro.core.ui.theme.IBMPlexMono
                )
            }

            ExposedDropdownMenuBox(
                expanded = recountReasonExpanded,
                onExpandedChange = { recountReasonExpanded = !recountReasonExpanded }
            ) {
                OutlinedTextField(
                    value = recountReason.ifBlank { "Select reason..." },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reason") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recountReasonExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = recountReasonExpanded,
                    onDismissRequest = { recountReasonExpanded = false }
                ) {
                    AdjustmentReasonPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset) },
                            onClick = {
                                recountReason = preset
                                recountReasonExpanded = false
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
                OutlinedButton(
                    onClick = { showRecountDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (variance != null && recountReason.isNotBlank() && kotlin.math.abs(variance) > 0.01) {
                            viewModel.recountBalance(recountAccountId!!, parsedDeclared!!, recountReason)
                            showRecountDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Balance recounted")
                            }
                        } else if (variance != null && kotlin.math.abs(variance) <= 0.01) {
                            showRecountDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Balance already matches — no adjustment needed")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = recountDeclaredBalance.isNotBlank() && recountReason.isNotBlank()
                ) {
                    Text("Save Recount")
                }
            }
        }
    }
}
}
