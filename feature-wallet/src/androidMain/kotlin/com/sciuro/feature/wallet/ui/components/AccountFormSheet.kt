package com.sciuro.feature.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.theme.AccountColorGreen
import com.najmi.sciuro.core.ui.theme.AccountColorBlue
import com.najmi.sciuro.core.ui.theme.AccountColorRed
import com.najmi.sciuro.core.ui.theme.AccountColorPurple
import com.najmi.sciuro.core.ui.theme.AccountColorOrange
import com.najmi.sciuro.core.ui.theme.AccountColorGrey
import com.najmi.sciuro.core.ui.theme.AccountColorBlack
import com.najmi.sciuro.core.ui.theme.AccountColorBrown
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.sciuro.feature.wallet.R
import com.sciuro.feature.wallet.ui.AppInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountFormSheet(
    editingAccountId: String?,
    installedApps: List<AppInfo>,
    initialName: String,
    initialType: String,
    initialPackage: String,
    initialBalance: String,
    initialColor: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onPackageChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SciuroFormSheet(
        title = if (editingAccountId == null) stringResource(R.string.wallet_add_account) else stringResource(R.string.wallet_edit_account),
        onDismissRequest = onDismiss, 
        modifier = modifier
    ) {

        SciuroTextField(
            value = initialName,
            onValueChange = onNameChange,
            label = stringResource(R.string.wallet_account_name_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            val selectedApp = installedApps.find { it.packageName == initialPackage }
            val displayValue = selectedApp?.name ?: initialPackage

            SciuroTextField(
                value = displayValue,
                onValueChange = onPackageChange,
                label = stringResource(R.string.wallet_associated_app_optional),
                placeholder = stringResource(R.string.wallet_search_apps),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )

            val filteredApps = installedApps.filter {
                it.name.contains(initialPackage, ignoreCase = true) || it.packageName.contains(initialPackage, ignoreCase = true)
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
                            onPackageChange(app.packageName)
                            expanded = false
                        }
                    )
                }
            }
        }

        SciuroTextField(
            value = initialBalance,
            onValueChange = onBalanceChange,
            label = stringResource(R.string.wallet_initial_balance_rm),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (initialName.isNotBlank()) onSave() })
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillToggle(
                options = listOf("Bank Account", "E-Wallet"),
                selectedOption = initialType,
                onOptionSelected = onTypeChange,
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.wallet_account_color), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            presetColors.forEach { (hex, color) ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorChange(hex) }
                        .padding(4.dp)
                ) {
                    if (initialColor == hex) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(8.dp))

        SciuroPrimaryButton(
            text = stringResource(R.string.wallet_save),
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = initialName.isNotBlank()
        )

        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wallet_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
