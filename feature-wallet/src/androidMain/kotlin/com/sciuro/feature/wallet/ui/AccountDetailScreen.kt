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
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroTextField
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
                snackbarHostState.showSnackbar("QR code saved")
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
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                content = {
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
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = "View QR Code",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        FilledTonalButton(
                            onClick = { showAdjustmentDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adjust Balance")
                        }
                    }
                }
            )

            Box(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 36.dp, end = 16.dp)) {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Details") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            expanded = false
                            showEditDetailsDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Change Color") },
                        onClick = {
                            expanded = false
                            selectedColor = state.account?.color
                            showColorDialog = true
                        }
                    )
                    if (state.account?.is_system == 0L) {
                        DropdownMenuItem(
                            text = { Text("Archive Account") },
                            onClick = {
                                expanded = false
                                showArchiveDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
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

        SheetList(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter) }
                        )
                    }
                }

                if (state.timeline.isEmpty()) {
                    com.najmi.sciuro.core.ui.components.EmptyStateView(
                        message = if (state.selectedFilter == "Adjustments") "No adjustments recorded for this account."
                                   else if (state.selectedFilter == "All" && state.transactions.isEmpty() && state.adjustments.isEmpty()) "No transactions or adjustments found for this account."
                                   else "No items match the current filter."
                    )
                } else {
                    for (item in state.timeline) {
                        when (item) {
                            is TimelineItem.TransactionItem -> {
                                val tx = item.tx
                                val isTransfer = tx.category_id == "cat_transfer"
                                val statusText = if (tx.is_reviewed == 1L) "Reviewed" else "Unreviewed"
                                TransactionCard(
                                    merchantName = tx.merchant ?: "Unknown Merchant",
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
                                    amount = adj.amount,
                                    formattedTime = "",
                                    onClick = {
                                        viewModel.deleteCorrection(adj.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Adjustment removed")
                                        }
                                    }
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
            merchantName = tx.merchant ?: "Unknown Merchant",
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
            },
            onDeleteClick = {
                showDetailSheet = false
            }
        )
    }

    if (showAdjustmentDialog && state.account != null) {
        AdjustmentBottomSheet(
            currentBalance = state.account!!.balance,
            onDismiss = { showAdjustmentDialog = false },
            onConfirm = { amount, reason ->
                viewModel.recordCorrection(amount, reason)
                showAdjustmentDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Balance adjustment recorded")
                }
            }
        )
    }

    if (showArchiveDialog) {
        SciuroConfirmationDialog(
            title = "Archive Account",
            message = "Are you sure you want to archive this account? It will be hidden from the main wallet views but historical transactions will be kept.",
            confirmText = "Archive",
            isDestructive = false,
            onConfirm = {
                viewModel.archiveAccount()
                showArchiveDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Account archived") }
                onNavigateBack()
            },
            onDismiss = { showArchiveDialog = false }
        )
    }

    if (showDeleteDialog) {
        SciuroConfirmationDialog(
            title = "Delete Account",
            message = "Are you sure you want to permanently delete this account? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Account deleted") }
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Choose Account Color") },
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
                    viewModel.updateAccountColor(selectedColor)
                    showColorDialog = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Account color updated") }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQrFullScreen && account.qr_image_path != null) {
        AlertDialog(
            onDismissRequest = { showQrFullScreen = false },
            title = { Text("QR Code", textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
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
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Unable to load QR image", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrFullScreen = false }) {
                    Text("Close")
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
                viewModel.updateAccountDetails(accountNumber, accountHolderName, bankInstitutionCode)
                showEditDetailsDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Account details updated")
                }
            },
            onPickQr = { qrImagePicker.launch("image/*") },
            onRemoveQr = {
                viewModel.updateQrImagePath(null)
                coroutineScope.launch { snackbarHostState.showSnackbar("QR code removed") }
            }
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
            contentDescription = "QR Code",
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

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Edit Account Details",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        SciuroTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = "Account Number",
            placeholder = "e.g. 1234567890 or last 4 digits"
        )

        Spacer(modifier = Modifier.height(12.dp))

        SciuroTextField(
            value = accountHolderName,
            onValueChange = { accountHolderName = it },
            label = "Account Holder Name",
            placeholder = "e.g. AHMAD BIN ABDULLAH"
        )

        Spacer(modifier = Modifier.height(12.dp))

        SciuroTextField(
            value = bankInstitutionCode,
            onValueChange = { bankInstitutionCode = it },
            label = "Bank Code",
            placeholder = "e.g. CIMB, MBB, BSN"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isCashWallet) {
            Text(
                "QR Code",
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
                        Text("Change")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onRemoveQr) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickQr,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select QR Image")
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
                Text("Cancel")
            }

            Button(
                onClick = { onConfirm(accountNumber.ifBlank { null }, accountHolderName.ifBlank { null }, bankInstitutionCode.ifBlank { null }) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
