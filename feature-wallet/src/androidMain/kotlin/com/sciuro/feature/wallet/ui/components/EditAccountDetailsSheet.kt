package com.sciuro.feature.wallet.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.sciuro.feature.wallet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDetailsSheet(
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
                        QrThumbnail(
                            filePath = currentQrImagePath,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = onPickQr) {
                        Icon(SciuroIcons.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
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
                    Icon(SciuroIcons.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
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
                Icon(SciuroIcons.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.wallet_cancel))
            }

            SciuroPrimaryButton(
                text = stringResource(R.string.wallet_save),
                onClick = {
                    onConfirm(
                        accountNumber.ifBlank { null },
                        accountHolderName.ifBlank { null },
                        bankInstitutionCode.ifBlank { null }
                    )
                },
                icon = SciuroIcons.Edit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QrThumbnail(filePath: String, modifier: Modifier = Modifier) {
    val bitmap = remember(filePath) {
        try { BitmapFactory.decodeFile(filePath) } catch (_: Exception) { null }
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
