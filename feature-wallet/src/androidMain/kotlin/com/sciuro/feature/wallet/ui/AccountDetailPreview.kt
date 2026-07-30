package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.TransactionSkeletonRow
import com.najmi.sciuro.core.ui.theme.SciuroTheme
import com.sciuro.feature.wallet.ui.components.AccountDetailHero
import com.sciuro.feature.wallet.ui.components.AccountInfoCard

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Account Detail Loading")
@Composable
private fun AccountDetailLoadingPreview() {
    SciuroTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    AccountDetailHero(
                        accountName = "Maybank Account",
                        accountBalance = 5240.50,
                        accountColor = "#4CAF50",
                        accountType = "SAVINGS",
                        accountNumber = "1234567890",
                        qrImagePath = null,
                        isCashWallet = false,
                        isSystem = false,
                        onNavigateBack = {},
                        onQrClick = {},
                        onAdjustClick = {},
                        onEditDetails = {},
                        onChangeColor = {},
                        onArchive = {},
                        onDelete = {}
                    )
                }
                item {
                    SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            AccountInfoCard(
                                accountNumber = "1234567890",
                                accountHolderName = "AHMAD BIN ABDULLAH",
                                bankInstitutionCode = "MBB",
                                transactionCount = 0
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Transaction History",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            repeat(5) { TransactionSkeletonRow() }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Account Detail Loaded")
@Composable
private fun AccountDetailLoadedPreview() {
    SciuroTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    AccountDetailHero(
                        accountName = "Maybank Account",
                        accountBalance = 5240.50,
                        accountColor = "#4CAF50",
                        accountType = "SAVINGS",
                        accountNumber = "1234567890",
                        qrImagePath = null,
                        isCashWallet = false,
                        isSystem = false,
                        onNavigateBack = {},
                        onQrClick = {},
                        onAdjustClick = {},
                        onEditDetails = {},
                        onChangeColor = {},
                        onArchive = {},
                        onDelete = {}
                    )
                }
                item {
                    SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            AccountInfoCard(
                                accountNumber = "1234567890",
                                accountHolderName = "AHMAD BIN ABDULLAH",
                                bankInstitutionCode = "MBB",
                                transactionCount = 47
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Transaction History",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                "No transactions for this account.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
