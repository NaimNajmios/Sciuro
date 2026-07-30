package com.sciuro.feature.wallet.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.*
import com.najmi.sciuro.core.ui.components.formatAuditLogDetail
import com.najmi.sciuro.core.ui.util.mapCategoryIcon
import com.sciuro.feature.wallet.R
import com.sciuro.feature.wallet.ui.components.*
import com.sciuro.feature.wallet.viewmodel.AccountDetailViewModel
import com.sciuro.feature.wallet.viewmodel.AccountDetailUiState
import com.sciuro.feature.wallet.viewmodel.AccountDetailDialog
import com.sciuro.feature.wallet.viewmodel.TimelineItem
import com.sciuro.feature.wallet.viewmodel.TransactionDetailData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentDialog by remember { mutableStateOf<AccountDetailDialog>(AccountDetailDialog.None) }
    var detailData by remember { mutableStateOf<TransactionDetailData?>(null) }

    val expenseCats by viewModel.expenseCategories.collectAsState()
    val incomeCats by viewModel.incomeCategories.collectAsState()
    val categoryMap = remember(expenseCats, incomeCats) {
        (expenseCats + incomeCats).associateBy { it.id }
    }

    val qrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val destFile = withContext(Dispatchers.IO) {
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

    LaunchedEffect(currentDialog) {
        if (currentDialog is AccountDetailDialog.TransactionDetail) {
            val tx = (currentDialog as AccountDetailDialog.TransactionDetail).tx
            detailData = withContext(Dispatchers.IO) {
                viewModel.loadTransactionDetail(tx)
            }
        } else {
            detailData = null
        }
    }

    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "uiState"
    ) { state ->
        when (state) {
            is AccountDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AccountDetailUiState.Loaded -> {
                val account = state.account
                val isCashWallet = account.type.lowercase().contains("cash") || account.type.lowercase().contains("personal")

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            AccountDetailHero(
                                accountName = account.name,
                                accountBalance = account.balance,
                                accountColor = account.color,
                                accountType = account.type,
                                accountNumber = account.account_number,
                                qrImagePath = account.qr_image_path,
                                isCashWallet = isCashWallet,
                                isSystem = account.is_system == 1L,
                                onNavigateBack = onNavigateBack,
                                onQrClick = { currentDialog = AccountDetailDialog.QrFullScreen },
                                onAdjustClick = { currentDialog = AccountDetailDialog.AdjustBalance },
                                onEditDetails = { currentDialog = AccountDetailDialog.EditAccountDetails },
                                onChangeColor = { currentDialog = AccountDetailDialog.ColorPicker },
                                onArchive = { currentDialog = AccountDetailDialog.ArchiveConfirm },
                                onDelete = { currentDialog = AccountDetailDialog.DeleteConfirm }
                            )
                        }

                        item {
                            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 32.dp)
                                        .navigationBarsPadding()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    AccountInfoCard(
                                        accountNumber = account.account_number,
                                        accountHolderName = account.account_holder_name,
                                        bankInstitutionCode = account.bank_institution_code,
                                        transactionCount = state.transactions.size
                                    )

                                    if (state.spendingVelocity != null) {
                                        AccountVelocityCard(velocity = state.spendingVelocity)
                                    }

                                    AccountDetailTimeline(
                                        timeline = state.timeline,
                                        selectedFilter = state.selectedFilter,
                                        onFilterSelected = { viewModel.setFilter(it) },
                                        categoryMap = categoryMap,
                                        onTransactionClick = { tx ->
                                            currentDialog = AccountDetailDialog.TransactionDetail(tx)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (val dialog = currentDialog) {
        AccountDetailDialog.None -> {}

        is AccountDetailDialog.TransactionDetail -> {
            val tx = dialog.tx
            val isTransfer = detailData?.transferLink != null || tx.category_id == "cat_transfer"
            val rawEvent = detailData?.rawEvent
            val auditLogs = detailData?.auditLogs ?: emptyList()
            val formattedTimestamp = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
                .format(Date(tx.timestamp))
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
                showSheet = true,
                onDismiss = { currentDialog = AccountDetailDialog.None },
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
                    currentDialog = AccountDetailDialog.EditTransaction(tx)
                },
                onDeleteClick = {
                    currentDialog = AccountDetailDialog.DeleteTransactionConfirm(tx)
                }
            )
        }

        is AccountDetailDialog.EditTransaction -> {
            val tx = dialog.tx
            var editTxAmount by remember { mutableStateOf(tx.amount.toString()) }
            var editTxMerchant by remember { mutableStateOf(tx.merchant ?: "") }
            var editTxDirection by remember { mutableStateOf(tx.direction) }
            var editTxCategoryId by remember { mutableStateOf(tx.category_id) }
            var editTxAccountId by remember { mutableStateOf(tx.account_id) }

            val loaded = uiState as? AccountDetailUiState.Loaded

            EditTransactionSheet(
                accounts = loaded?.let { listOf(it.account) } ?: emptyList(),
                editTxAmount = editTxAmount,
                onAmountChange = { editTxAmount = it },
                editTxMerchant = editTxMerchant,
                onMerchantChange = { editTxMerchant = it },
                editTxDirection = editTxDirection,
                onDirectionChange = { editTxDirection = it },
                editTxCategoryId = editTxCategoryId,
                onCategoryIdChange = { editTxCategoryId = it },
                editTxAccountId = editTxAccountId,
                onAccountIdChange = { editTxAccountId = it },
                expenseCategories = expenseCats,
                incomeCategories = incomeCats,
                onDelete = {
                    currentDialog = AccountDetailDialog.DeleteTransactionConfirm(tx)
                },
                onSave = {
                    val amt = editTxAmount.toDoubleOrNull() ?: 0.0
                    viewModel.editTransaction(
                        transactionId = tx.id,
                        amount = amt,
                        direction = editTxDirection,
                        merchant = editTxMerchant,
                        categoryId = editTxCategoryId,
                        accountId = editTxAccountId
                    )
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_transaction_updated))
                    }
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.AdjustBalance -> {
            val loaded = uiState as? AccountDetailUiState.Loaded
            if (loaded != null) {
                AdjustmentBottomSheet(
                    currentBalance = loaded.account.balance,
                    onDismiss = { currentDialog = AccountDetailDialog.None },
                    onConfirm = { amount, reason, remark ->
                        currentDialog = AccountDetailDialog.AdjustConfirm(amount, reason, remark)
                    }
                )
            }
        }

        is AccountDetailDialog.AdjustConfirm -> {
            SciuroConfirmationDialog(
                title = stringResource(R.string.wallet_confirm_adjustment_title),
                message = stringResource(R.string.wallet_confirm_adjustment_message, dialog.amount),
                confirmText = stringResource(R.string.wallet_confirm),
                isDestructive = false,
                onConfirm = {
                    viewModel.recordCorrection(dialog.amount, dialog.reason, dialog.remark)
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_balance_adjustment_recorded))
                    }
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.EditAccountDetails -> {
            val loaded = uiState as? AccountDetailUiState.Loaded
            if (loaded != null) {
                EditAccountDetailsSheet(
                    currentAccountNumber = loaded.account.account_number,
                    currentAccountHolderName = loaded.account.account_holder_name,
                    currentBankInstitutionCode = loaded.account.bank_institution_code,
                    currentQrImagePath = loaded.account.qr_image_path,
                    isCashWallet = loaded.account.type.lowercase().contains("cash") || loaded.account.type.lowercase().contains("personal"),
                    onDismiss = { currentDialog = AccountDetailDialog.None },
                    onConfirm = { number, holder, code ->
                        currentDialog = AccountDetailDialog.EditAccountConfirm(number, holder, code)
                    },
                    onPickQr = { qrImagePicker.launch("image/*") },
                    onRemoveQr = {
                        viewModel.updateQrImagePath(null)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.wallet_qr_code_removed))
                        }
                    }
                )
            }
        }

        is AccountDetailDialog.EditAccountConfirm -> {
            SciuroConfirmationDialog(
                title = stringResource(R.string.wallet_save_account_details),
                message = stringResource(R.string.wallet_save_account_details_message),
                confirmText = stringResource(R.string.wallet_save),
                isDestructive = false,
                onConfirm = {
                    viewModel.updateAccountDetails(dialog.accountNumber, dialog.holderName, dialog.bankCode)
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_details_updated))
                    }
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.ColorPicker -> {
            val loaded = uiState as? AccountDetailUiState.Loaded
            ColorPickerDialog(
                currentColor = loaded?.account?.color,
                onDismiss = { currentDialog = AccountDetailDialog.None },
                onSave = { color ->
                    currentDialog = AccountDetailDialog.ColorConfirm(color)
                }
            )
        }

        is AccountDetailDialog.ColorConfirm -> {
            SciuroConfirmationDialog(
                title = stringResource(R.string.wallet_save_color),
                message = stringResource(R.string.wallet_apply_color_message),
                confirmText = stringResource(R.string.wallet_save),
                isDestructive = false,
                onConfirm = {
                    viewModel.updateAccountColor(dialog.color)
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_color_updated))
                    }
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.ArchiveConfirm -> {
            SciuroConfirmationDialog(
                title = stringResource(R.string.wallet_archive_account),
                message = stringResource(R.string.wallet_archive_account_message),
                confirmText = stringResource(R.string.wallet_archive),
                isDestructive = false,
                onConfirm = {
                    viewModel.archiveAccount()
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_archived))
                    }
                    onNavigateBack()
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.DeleteConfirm -> {
            SciuroConfirmationDialog(
                title = stringResource(R.string.wallet_delete_account_title),
                message = stringResource(R.string.wallet_delete_account_permanently_message),
                confirmText = stringResource(R.string.wallet_delete),
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteAccount()
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_account_deleted))
                    }
                    onNavigateBack()
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.DeleteTransactionConfirm -> {
            SciuroConfirmationDialog(
                title = stringResource(R.string.wallet_delete_transaction_title),
                message = stringResource(R.string.wallet_delete_transaction_message),
                confirmText = stringResource(R.string.wallet_delete),
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteTransaction(dialog.tx.id)
                    currentDialog = AccountDetailDialog.None
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.wallet_transaction_deleted))
                    }
                },
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }

        is AccountDetailDialog.QrFullScreen -> {
            val loaded = uiState as? AccountDetailUiState.Loaded
            QrFullScreenDialog(
                qrImagePath = loaded?.account?.qr_image_path,
                onDismiss = { currentDialog = AccountDetailDialog.None }
            )
        }
    }
}
