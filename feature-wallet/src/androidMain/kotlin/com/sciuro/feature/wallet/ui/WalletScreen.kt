package com.sciuro.feature.wallet.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sciuro.feature.wallet.R
import androidx.core.graphics.drawable.toBitmap
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.AdjustmentCard
import com.najmi.sciuro.core.ui.components.AdjustmentBottomSheet
import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.theme.AccountColorGreen
import com.najmi.sciuro.core.ui.theme.AccountColorBlue
import com.najmi.sciuro.core.ui.theme.AccountColorRed
import com.najmi.sciuro.core.ui.theme.AccountColorPurple
import com.najmi.sciuro.core.ui.theme.AccountColorOrange
import com.najmi.sciuro.core.ui.theme.AccountColorGrey
import com.najmi.sciuro.core.ui.theme.AccountColorBlack
import com.najmi.sciuro.core.ui.theme.AccountColorBrown
import com.najmi.sciuro.core.ui.components.PillToggle
import com.sciuro.feature.wallet.ui.components.AccountCard
import com.sciuro.feature.wallet.ui.components.AccountPagerDots
import com.sciuro.feature.wallet.ui.components.InvestmentCard
import com.sciuro.feature.wallet.ui.components.QrCodeDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

private var cachedInstalledApps: List<AppInfo>? = null

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
    
    // Transaction list filter
    var txFilter by rememberSaveable { mutableStateOf("All") }

    val accountPagerState = rememberPagerState(pageCount = { accounts.size })

    var showQrDialog by rememberSaveable { mutableStateOf(false) }
    var selectedQrPath by rememberSaveable { mutableStateOf<String?>(null) }

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
        if (cachedInstalledApps == null) {
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
                cachedInstalledApps = appList
                installedApps = appList
            }
        } else {
            installedApps = cachedInstalledApps!!
        }
    }
    
    val totalLiquidity = accounts.sumOf { it.balance }
    val currentInvestmentTotal by viewModel.currentInvestmentTotal.collectAsState()
    val totalInvestments = if (currentInvestmentTotal > 0.0) currentInvestmentTotal else investments.sumOf { it.unitsHeld * it.averageBuyPrice }
    val displayTotal = if (selectedAssetType == "Liquid Cash") totalLiquidity else totalInvestments
    
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allAdjustments by viewModel.allAdjustments.collectAsState()
    
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier
        .nestedScroll(pullToRefreshState.nestedScrollConnection)
        .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
        ) {
            item {
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
                        text = if (selectedAssetType == "Liquid Cash") stringResource(R.string.wallet_total_liquidity) else stringResource(R.string.wallet_total_investments),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HeroFigure(amount = displayTotal)
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
                        message = stringResource(R.string.wallet_empty_no_accounts),
                        primaryCtaText = stringResource(R.string.wallet_add_account),
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
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        pageSpacing = 12.dp
                    ) { page ->
                        val account = accounts[page]
                        val pageOffset = (accountPagerState.currentPage - page + accountPagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        AccountCard(
                            account = account,
                            installedApps = installedApps,
                            pagerOffset = pageOffset,
                            onAccountClick = onAccountClick,
                            onQrClick = { path ->
                                selectedQrPath = path
                                showQrDialog = true
                            }
                        )
                    }
                    AccountPagerDots(
                        pageCount = accounts.size,
                        currentPage = accountPagerState.currentPage
                    )
                }
            } else {
                if (investments.isEmpty()) {
                    com.najmi.sciuro.core.ui.components.EmptyStateView(
                        message = stringResource(R.string.wallet_empty_no_investments),
                        primaryCtaText = stringResource(R.string.wallet_add_investment),
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
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        investments.forEach { inv ->
                            InvestmentCard(
                                investment = inv,
                                onClick = {
                                    editingInvestmentId = inv.id
                                    newAssetType = inv.assetType
                                    newAssetSymbol = inv.assetSymbol
                                    newAssetName = inv.assetName
                                    newUnitsHeld = inv.unitsHeld.toString()
                                    newAvgBuyPrice = inv.averageBuyPrice.toString()
                                    newAssociatedAccountId = inv.associatedAccountId ?: ""
                                    showAddInvestmentDialog = true
                                }
                            )
                        }
                    }
                }
            }
        
            }
        }
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
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
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.wallet_recent_transactions), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 16.dp))

                    if (selectedAssetType == "Liquid Cash" && accounts.isNotEmpty()) {
                        val selectedAccountId = accounts[accountPagerState.currentPage].id
                        val accountTx = allTransactions.filter { it.account_id == selectedAccountId }
                        val accountAdjustments = allAdjustments

                        if (txFilter == "Adjustments") {
                                if (accountAdjustments.isEmpty()) {
                                    com.najmi.sciuro.core.ui.components.EmptyStateView(message = stringResource(R.string.wallet_empty_no_adjustments))
                                } else {
                                    accountAdjustments.forEach { adj ->
                                        AdjustmentCard(
                                            reason = adj.reason,
                                            amount = adj.amount
                                        )
                                    }
                                }
                            } else if (accountTx.isEmpty()) {
                                com.najmi.sciuro.core.ui.components.EmptyStateView(message = stringResource(R.string.wallet_empty_no_transactions))
                            } else {
                                accountTx.take(20).forEach { tx ->
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
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), contentColor = MaterialTheme.colorScheme.onSurface)
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
                                                    tint = if (tx.direction == "INFLOW") com.najmi.sciuro.core.ui.theme.SignalIncome else com.najmi.sciuro.core.ui.theme.SignalDanger
                                                )
                                                Column {
                                                    Text(
                                                        tx.merchant ?: stringResource(R.string.wallet_unknown_merchant),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        if (tx.is_reviewed == 1L) stringResource(R.string.wallet_reviewed) else stringResource(R.string.wallet_unreviewed),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (tx.is_reviewed == 1L) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            Text(
                                                "RM ${"%.2f".format(tx.amount)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (tx.direction == "INFLOW") com.najmi.sciuro.core.ui.theme.SignalIncome else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (selectedAssetType == "Investments") {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(message = stringResource(R.string.wallet_empty_investment_tracked_manually))
                    } else {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(message = stringResource(R.string.wallet_empty_no_data))
                    }
                }
            }
        }
        }

        if (pullToRefreshState.isRefreshing) {
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            )
        }
        
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp, 
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.wallet_add_cd))
        }
    }

    if (showAddAccountDialog) {
        com.najmi.sciuro.core.ui.components.SciuroBottomSheet(
            onDismissRequest = { showAddAccountDialog = false }
        ) {
                Text(
                    if (editingAccountId == null) stringResource(R.string.wallet_add_account) else stringResource(R.string.wallet_edit_account),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                SciuroTextField(
                    value = newAccountName,
                    onValueChange = { newAccountName = it },
                    label = stringResource(R.string.wallet_account_name_hint)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedApp = installedApps.find { it.packageName == newAccountPackage }
                    val displayValue = selectedApp?.name ?: newAccountPackage

                    SciuroTextField(
                        value = displayValue,
                        onValueChange = { newAccountPackage = it },
                        label = stringResource(R.string.wallet_associated_app_optional),
                        placeholder = stringResource(R.string.wallet_search_apps),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
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
                
                SciuroAmountField(
                    value = newAccountBalance,
                    onValueChange = { newAccountBalance = it },
                    label = stringResource(R.string.wallet_initial_balance_rm)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillToggle(
                        options = listOf("Bank Account", "E-Wallet"),
                        selectedOption = newAccountType,
                        onOptionSelected = { newAccountType = it },
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.wallet_account_color), style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetColors = listOf(
                        "#4CAF50" to AccountColorGreen,
                        "#2196F3" to AccountColorBlue,
                        "#F44336" to AccountColorRed,
                        "#9C27B0" to AccountColorPurple,
                        "#FF9800" to AccountColorOrange,
                        "#607D8B" to AccountColorGrey,
                        "#1A1A1A" to AccountColorBlack,
                        "#795548" to AccountColorBrown
                    )
                    items(presetColors.size) { i ->
                        val (hex, color) = presetColors[i]
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
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
                            Text(stringResource(R.string.wallet_delete))
                        }
                    }
                    
                    SciuroPrimaryButton(
                        text = stringResource(R.string.wallet_save),
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
                                snackbarHostState.showSnackbar(context.getString(if (editingAccountId == null) R.string.wallet_account_created else R.string.wallet_account_updated))
                            }
                        },
                        modifier = Modifier.weight(if (editingAccountId != null) 1f else 2f),
                        enabled = newAccountName.isNotBlank()
                    )
                }
        }
    }

    if (showAddInvestmentDialog) {
        com.najmi.sciuro.core.ui.components.SciuroBottomSheet(
            onDismissRequest = { showAddInvestmentDialog = false }
        ) {
                Text(
                    if (editingInvestmentId == null) stringResource(R.string.wallet_add_investment) else stringResource(R.string.wallet_edit_investment),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillToggle(
                        options = listOf("Stock", "Gold"),
                        selectedOption = newAssetType,
                        onOptionSelected = { newAssetType = it },
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (newAssetType == "Stock") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SciuroTextField(
                            value = newAssetSymbol,
                            onValueChange = { newAssetSymbol = it.uppercase() },
                            label = stringResource(R.string.wallet_symbol),
                            placeholder = stringResource(R.string.wallet_symbol_hint),
                            modifier = Modifier.weight(1f)
                        )
                        SciuroTextField(
                            value = newAssetName,
                            onValueChange = { newAssetName = it },
                            label = stringResource(R.string.wallet_name),
                            placeholder = stringResource(R.string.wallet_name_hint_apple),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    SciuroTextField(
                        value = newAssetName,
                        onValueChange = { newAssetName = it },
                        label = stringResource(R.string.wallet_account_name_label),
                        placeholder = stringResource(R.string.wallet_name_hint_gold)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val unitsLabel = if (newAssetType == "Gold") stringResource(R.string.wallet_grams_held) else stringResource(R.string.wallet_units_held)
                    SciuroTextField(
                        value = newUnitsHeld,
                        onValueChange = { newUnitsHeld = it },
                        label = unitsLabel,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    SciuroAmountField(
                        value = newAvgBuyPrice,
                        onValueChange = { newAvgBuyPrice = it },
                        label = stringResource(R.string.wallet_avg_price_rm),
                        modifier = Modifier.weight(1f)
                    )
                }

                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    val selectedAccount = accounts.find { it.id == newAssociatedAccountId }
                    SciuroTextField(
                        value = selectedAccount?.name ?: stringResource(R.string.wallet_none),
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.wallet_funding_account_optional),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wallet_none)) },
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
                            Text(stringResource(R.string.wallet_delete))
                        }
                    }
                    
                    SciuroPrimaryButton(
                        text = stringResource(R.string.wallet_save),
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
                                snackbarHostState.showSnackbar(context.getString(if (editingInvestmentId == null) R.string.wallet_investment_created else R.string.wallet_investment_updated))
                            }
                        },
                        modifier = Modifier.weight(if (editingInvestmentId != null) 1f else 2f),
                        enabled = (newAssetType == "Gold" || newAssetSymbol.isNotBlank()) && newAssetName.isNotBlank() && newUnitsHeld.isNotBlank() && newAvgBuyPrice.isNotBlank()
                    )
                }
        }
    }

    if (showEditTransactionDialog) {
        com.najmi.sciuro.core.ui.components.SciuroBottomSheet(
            onDismissRequest = { showEditTransactionDialog = false }
        ) {
                Text(stringResource(R.string.wallet_edit_transaction), style = MaterialTheme.typography.headlineSmall)
                
                SciuroAmountField(
                    value = editTxAmount,
                    onValueChange = { editTxAmount = it },
                    label = stringResource(R.string.wallet_amount_rm)
                )

                SciuroTextField(
                    value = editTxMerchant,
                    onValueChange = { editTxMerchant = it },
                    label = stringResource(R.string.wallet_merchant_note)
                )
                
                PillToggle(
                    options = listOf("Expense", "Income"),
                    selectedOption = if (editTxDirection == "OUTFLOW") "Expense" else "Income",
                    onOptionSelected = { label ->
                        editTxDirection = if (label == "Expense") "OUTFLOW" else "INFLOW"
                        editTxCategoryId = null
                    },
                    fillWidth = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val selAcc = accounts.find { it.id == editTxAccountId }
                    SciuroTextField(
                        value = selAcc?.name ?: stringResource(R.string.wallet_select_account),
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.wallet_wallet_account),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor()
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
                
                Text(stringResource(R.string.wallet_category), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.wallet_delete))
                    }
                    
                    SciuroPrimaryButton(
                        text = stringResource(R.string.wallet_save),
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
                    )
                }
        }
    }

    if (showDeleteAccountDialog) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_delete_account_title),
            message = stringResource(R.string.wallet_delete_account_message),
            confirmText = stringResource(R.string.wallet_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccount(editingAccountId!!)
                showDeleteAccountDialog = false
                showAddAccountDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_deleted)) }
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    if (showDeleteInvestmentDialog) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_delete_investment_title),
            message = stringResource(R.string.wallet_delete_investment_message),
            confirmText = stringResource(R.string.wallet_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteInvestment(editingInvestmentId!!)
                showDeleteInvestmentDialog = false
                showAddInvestmentDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_investment_deleted)) }
            },
            onDismiss = { showDeleteInvestmentDialog = false }
        )
    }

    if (showQrDialog && selectedQrPath != null) {
        QrCodeDialog(
            qrPath = selectedQrPath!!,
            onDismiss = { showQrDialog = false; selectedQrPath = null }
        )
    }
}
