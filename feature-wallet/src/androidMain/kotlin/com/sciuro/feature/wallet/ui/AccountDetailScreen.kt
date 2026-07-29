package com.sciuro.feature.wallet.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.net.Uri
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.AdjustmentCard
import com.najmi.sciuro.core.ui.components.AdjustmentBottomSheet
import com.najmi.sciuro.core.ui.components.AuditEventDisplay
import com.najmi.sciuro.core.ui.components.TransactionCard
import com.najmi.sciuro.core.ui.components.TransactionDetailSheet
import com.najmi.sciuro.core.ui.components.formatAuditLogDetail
import com.sciuro.feature.wallet.viewmodel.AccountDetailViewModel
import com.sciuro.feature.wallet.viewmodel.TimelineItem
import kotlinx.coroutines.launch
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.sciuro.feature.wallet.R
import org.koin.androidx.compose.koinViewModel

private val filterOptions = listOf("All", "Transactions", "Adjustments", "Income", "Expense")

@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<com.sciuro.core.ledger.db.Transaction_record?>(null) }
    var detailData by remember { mutableStateOf<com.sciuro.feature.wallet.viewmodel.TransactionDetailData?>(null) }
    var showQrFullScreen by remember { mutableStateOf(false) }

    // Confirmation Dialogs
    var showDeleteTxConfirmation by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<com.sciuro.core.ledger.db.Transaction_record?>(null) }

    var showAdjustConfirmation by remember { mutableStateOf(false) }
    var adjustAmountTemp by remember { mutableStateOf(0.0) }
    var adjustReasonTemp by remember { mutableStateOf("") }
    var adjustRemarkTemp by remember { mutableStateOf<String?>(null) }
    
    var showColorConfirmation by remember { mutableStateOf(false) }

    var showEditAccountConfirmation by remember { mutableStateOf(false) }
    var editAccountNumberTemp by remember { mutableStateOf<String?>(null) }
    var editAccountHolderNameTemp by remember { mutableStateOf<String?>(null) }
    var editBankCodeTemp by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val qrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val destFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val dir = java.io.File(context.filesDir, "qr_codes").apply { mkdirs() }
                    val file = java.io.File(dir, "${java.util.UUID.randomUUID()}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file
                }
                viewModel.updateQrImagePath(destFile.absolutePath)
                snackbarHostState.showSnackbar(context.getString(R.string.wallet_qr_code_saved))
            }
        }
    }

    LaunchedEffect(showDetailSheet, selectedTxForDetail) {
        if (showDetailSheet && selectedTxForDetail != null) {
            detailData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                viewModel.loadTransactionDetail(selectedTxForDetail!!)
            }
        } else {
            detailData = null
        }
    }

    val presetColors = listOf(
        null,
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#F44336", // Red
        "#9C27B0", // Purple
        "#FF9800", // Orange
        "#607D8B", // Blue Grey
        "#1A1A1A", // Dark
        "#795548"  // Brown
    )

    if (state.account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val account = state.account!!
    val isCashWallet = account.type.lowercase().contains("cash") || account.type.lowercase().contains("personal")

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HeroPanel(
                title = account.name,
                heroFigure = { HeroFigure(account.balance) },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.wallet_back_cd),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                content = {
                    val onPrimary = MaterialTheme.colorScheme.onPrimary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (account.qr_image_path != null && !isCashWallet) {
                            FilledTonalButton(
                                onClick = { showQrFullScreen = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = onPrimary.copy(alpha = 0.15f),
                                    contentColor = onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = stringResource(R.string.wallet_view_qr_cd),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        FilledTonalButton(
                            onClick = { showAdjustmentDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = onPrimary.copy(alpha = 0.15f),
                                contentColor = onPrimary
                            )
                        ) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.wallet_adjust_balance))
                        }
                    }
                }
            )

            Box(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 36.dp, end = 16.dp)) {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.wallet_more_options_cd),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wallet_edit_details)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            expanded = false
                            showEditDetailsDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wallet_change_color)) },
                        onClick = {
                            expanded = false
                            selectedColor = state.account?.color
                            showColorDialog = true
                        }
                    )
                    if (state.account?.is_system == 0L) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wallet_archive_account)) },
                            onClick = {
                                expanded = false
                                showArchiveDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wallet_delete_account_title), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        if (account.account_number != null || account.account_holder_name != null || account.bank_institution_code != null) {
            com.najmi.sciuro.core.ui.components.SciuroCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Account Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val accName = account.account_holder_name
                    val accNum = account.account_number
                    val accBank = account.bank_institution_code
                    
                    if (!accName.isNullOrBlank()) {
                        Row {
                            Text("Name: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accName, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        }
                    }
                    if (!accNum.isNullOrBlank()) {
                        Row {
                            Text("Account: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accNum, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        }
                    }
                    if (!accBank.isNullOrBlank()) {
                        Row {
                            Text("Bank Code: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accBank, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        }
                    }
                }
            }
        }

        SheetList(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    stringResource(R.string.wallet_transaction_history),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                PillToggle(
                    options = filterOptions,
                    selectedOption = state.selectedFilter,
                    onOptionSelected = { viewModel.setFilter(it) },
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                    scrollable = true
                )

                if (state.timeline.isEmpty()) {
                    com.najmi.sciuro.core.ui.components.EmptyStateView(
                        message = if (state.selectedFilter == "Adjustments") stringResource(R.string.wallet_empty_no_adjustments_recorded)
                                   else if (state.selectedFilter == "All" && state.transactions.isEmpty() && state.adjustments.isEmpty()) stringResource(R.string.wallet_empty_no_tx_or_adjustments)
                                   else stringResource(R.string.wallet_empty_no_items_filter)
                    )
                } else {
                    for (item in state.timeline) {
                        when (item) {
                            is TimelineItem.TransactionItem -> {
                                val tx = item.tx
                                val isTransfer = tx.category_id == "cat_transfer"
                                val statusText = if (tx.is_reviewed == 1L) stringResource(R.string.wallet_reviewed) else stringResource(R.string.wallet_unreviewed)
                                TransactionCard(
                                    merchantName = tx.merchant ?: stringResource(R.string.wallet_unknown_merchant),
                                    amount = "RM ${"%.2f".format(tx.amount)}",
                                    direction = tx.direction,
                                    statusText = statusText,
                                    isTransfer = isTransfer,
                                    confidence = tx.confidence,
                                    extractionMethod = tx.extraction_method,
                                    onClick = {
                                        selectedTxForDetail = tx
                                        showDetailSheet = true
                                    }
                                )
                            }
                            is TimelineItem.AdjustmentItem -> {
                                val adj = item.adjustment
                                AdjustmentCard(
                                    reason = adj.reason,
                                    amount = adj.amount
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailSheet && selectedTxForDetail != null) {
        val tx = selectedTxForDetail!!
        val isTransfer = detailData?.transferLink != null || tx.category_id == "cat_transfer"
        val rawEvent = detailData?.rawEvent
        val auditLogs = detailData?.auditLogs ?: emptyList()
        val formattedTimestamp = java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(tx.timestamp))
        val auditEvents = auditLogs.map { log ->
            val actionLabel = when (log.action.name) {
                "CREATE" -> "Created"
                "UPDATE" -> "Edited"
                "RECLASSIFY" -> "Recategorized"
                "DELETE" -> "Deleted"
                else -> log.action.name
            }
            val sourceLabel = when (log.source.name) {
                "SYSTEM_AUTO" -> "auto"
                "USER_MANUAL" -> "you"
                "LLM_INFERRED" -> "AI"
                else -> log.source.name
            }
            val detail = formatAuditLogDetail(
                action = log.action.name,
                source = log.source.name,
                beforeState = log.beforeState,
                afterState = log.afterState
            )
            AuditEventDisplay(
                label = "$actionLabel ($sourceLabel)",
                detail = detail,
                isCurrent = false
            )
        }

        TransactionDetailSheet(
            showSheet = showDetailSheet,
            onDismiss = { showDetailSheet = false },
            merchantName = tx.merchant ?: stringResource(R.string.wallet_unknown_merchant),
            amount = "RM ${"%.2f".format(tx.amount)}",
            direction = tx.direction,
            timestamp = formattedTimestamp,
            extractionMethod = tx.extraction_method,
            confidence = tx.confidence,
            rawEventTitle = rawEvent?.title,
            rawEventText = rawEvent?.text,
            hasTransferLink = isTransfer,
            auditEvents = auditEvents,
            onEditClick = {
                showDetailSheet = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.wallet_edit_tx_not_implemented))
                }
            },
            onDeleteClick = {
                showDetailSheet = false
                txToDelete = tx
                showDeleteTxConfirmation = true
            }
        )
    }

    if (showAdjustmentDialog && state.account != null) {
        AdjustmentBottomSheet(
            currentBalance = state.account!!.balance,
            onDismiss = { showAdjustmentDialog = false },
            onConfirm = { amount, reason, remark ->
                adjustAmountTemp = amount
                adjustReasonTemp = reason
                adjustRemarkTemp = remark
                showAdjustmentDialog = false
                showAdjustConfirmation = true
            }
        )
    }

    if (showArchiveDialog) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_archive_account),
            message = stringResource(R.string.wallet_archive_account_message),
            confirmText = stringResource(R.string.wallet_archive),
            isDestructive = false,
            onConfirm = {
                viewModel.archiveAccount()
                showArchiveDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_archived)) }
                onNavigateBack()
            },
            onDismiss = { showArchiveDialog = false }
        )
    }

    if (showDeleteDialog) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_delete_account_title),
            message = stringResource(R.string.wallet_delete_account_permanently_message),
            confirmText = stringResource(R.string.wallet_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_deleted)) }
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text(stringResource(R.string.wallet_choose_account_color)) },
            text = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(presetColors) { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colorHex == null) MaterialTheme.colorScheme.surfaceVariant
                                    else try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (colorHex == null) MaterialTheme.colorScheme.onSurface else Color.White)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showColorDialog = false
                    showColorConfirmation = true
                }) {
                    Text(stringResource(R.string.wallet_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text(stringResource(R.string.wallet_cancel))
                }
            }
        )
    }

    if (showQrFullScreen && account.qr_image_path != null) {
        AlertDialog(
            onDismissRequest = { showQrFullScreen = false },
            title = { Text(stringResource(R.string.wallet_qr_code), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(account.qr_image_path) {
                        try { BitmapFactory.decodeFile(account.qr_image_path) } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.wallet_qr_code),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(stringResource(R.string.wallet_unable_to_load_qr), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrFullScreen = false }) {
                    Text(stringResource(R.string.wallet_close))
                }
            }
        )
    }

    if (showEditDetailsDialog) {
        EditAccountDetailsSheet(
            currentAccountNumber = state.account?.account_number,
            currentAccountHolderName = state.account?.account_holder_name,
            currentBankInstitutionCode = state.account?.bank_institution_code,
            currentQrImagePath = state.account?.qr_image_path,
            isCashWallet = isCashWallet,
            onDismiss = { showEditDetailsDialog = false },
            onConfirm = { accountNumber, accountHolderName, bankInstitutionCode ->
                editAccountNumberTemp = accountNumber
                editAccountHolderNameTemp = accountHolderName
                editBankCodeTemp = bankInstitutionCode
                showEditDetailsDialog = false
                showEditAccountConfirmation = true
            },
            onPickQr = { qrImagePicker.launch("image/*") },
            onRemoveQr = {
                viewModel.updateQrImagePath(null)
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_qr_code_removed)) }
            }
        )
    }

    if (showDeleteTxConfirmation && txToDelete != null) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_delete_transaction_title),
            message = stringResource(R.string.wallet_delete_transaction_message),
            confirmText = stringResource(R.string.wallet_delete),
            isDestructive = true,
            onConfirm = {
                txToDelete?.let { viewModel.deleteTransaction(it.id) }
                showDeleteTxConfirmation = false
                txToDelete = null
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_transaction_deleted)) }
            },
            onDismiss = {
                showDeleteTxConfirmation = false
                txToDelete = null
            }
        )
    }

    if (showAdjustConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_confirm_adjustment_title),
            message = stringResource(R.string.wallet_confirm_adjustment_message, adjustAmountTemp),
            confirmText = stringResource(R.string.wallet_confirm),
            isDestructive = false,
            onConfirm = {
                viewModel.recordCorrection(adjustAmountTemp, adjustReasonTemp, adjustRemarkTemp)
                showAdjustConfirmation = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_balance_adjustment_recorded)) }
            },
            onDismiss = { showAdjustConfirmation = false }
        )
    }

    if (showColorConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_save_color),
            message = stringResource(R.string.wallet_apply_color_message),
            confirmText = stringResource(R.string.wallet_save),
            isDestructive = false,
            onConfirm = {
                viewModel.updateAccountColor(selectedColor)
                showColorConfirmation = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_color_updated)) }
            },
            onDismiss = { showColorConfirmation = false }
        )
    }

    if (showEditAccountConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.wallet_save_account_details),
            message = stringResource(R.string.wallet_save_account_details_message),
            confirmText = stringResource(R.string.wallet_save),
            isDestructive = false,
            onConfirm = {
                viewModel.updateAccountDetails(editAccountNumberTemp, editAccountHolderNameTemp, editBankCodeTemp)
                showEditAccountConfirmation = false
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_details_updated)) }
            },
            onDismiss = { showEditAccountConfirmation = false }
        )
    }
}

@Composable
private fun QrCodeThumbnail(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(filePath) {
        try { BitmapFactory.decodeFile(filePath) } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.wallet_qr_code),
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountDetailsSheet(
    currentAccountNumber: String?,
    currentAccountHolderName: String?,
    currentBankInstitutionCode: String?,
    currentQrImagePath: String?,
    isCashWallet: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (accountNumber: String?, accountHolderName: String?, bankInstitutionCode: String?) -> Unit,
    onPickQr: () -> Unit,
    onRemoveQr: () -> Unit
) {
    var accountNumber by remember { mutableStateOf(currentAccountNumber ?: "") }
    var accountHolderName by remember { mutableStateOf(currentAccountHolderName ?: "") }
    var bankInstitutionCode by remember { mutableStateOf(currentBankInstitutionCode ?: "") }

    SciuroFormSheet(
        title = stringResource(R.string.wallet_edit_account_details),
        onDismissRequest = onDismiss
    ) {

        SciuroTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = stringResource(R.string.wallet_account_number),
            placeholder = stringResource(R.string.wallet_account_number_hint)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SciuroTextField(
            value = accountHolderName,
            onValueChange = { accountHolderName = it },
            label = stringResource(R.string.wallet_account_holder_name),
            placeholder = stringResource(R.string.wallet_account_holder_name_hint)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SciuroTextField(
            value = bankInstitutionCode,
            onValueChange = { bankInstitutionCode = it },
            label = stringResource(R.string.wallet_bank_code),
            placeholder = stringResource(R.string.wallet_bank_code_hint)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isCashWallet) {
            Text(
                stringResource(R.string.wallet_qr_code),
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentQrImagePath != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        QrCodeThumbnail(
                            filePath = currentQrImagePath,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = onPickQr) {
                        Text(stringResource(R.string.wallet_change))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onRemoveQr) {
                        Text(stringResource(R.string.wallet_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickQr,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.wallet_select_qr_image))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.wallet_cancel))
            }

            SciuroPrimaryButton(
                text = stringResource(R.string.wallet_save),
                onClick = { onConfirm(accountNumber.ifBlank { null }, accountHolderName.ifBlank { null }, bankInstitutionCode.ifBlank { null }) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
