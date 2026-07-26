package com.sciuro.feature.wallet.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import com.sciuro.feature.wallet.R
import com.sciuro.feature.wallet.ui.AppInfo
import com.sciuro.feature.wallet.model.WalletAccount
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import kotlin.math.absoluteValue

@Composable
fun AccountCard(
    account: WalletAccount,
    installedApps: List<AppInfo>,
    pagerOffset: Float,
    onAccountClick: (String) -> Unit,
    onQrClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerCol = if (account.color != null) {
        try { Color(android.graphics.Color.parseColor(account.color)) } catch(e: Exception) { MaterialTheme.colorScheme.surfaceVariant }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentCol = if (account.color != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .graphicsLayer {
                val scale = lerp(0.85f, 1f, 1f - pagerOffset.absoluteValue.coerceIn(0f, 1f))
                scaleX = scale
                scaleY = scale
                alpha = lerp(0.5f, 1f, 1f - pagerOffset.absoluteValue.coerceIn(0f, 1f))
            }
            .clickable { onAccountClick(account.id) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerCol, contentColor = contentCol)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val associatedApp = installedApps.find { it.packageName == account.associatedPackage }
                    if (associatedApp != null) {
                        Image(
                            bitmap = associatedApp.icon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                account.isCashWallet -> Icons.Filled.Wallet
                                account.isEWallet -> Icons.Filled.AccountBalanceWallet
                                else -> Icons.Filled.AccountBalance
                            },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentCol
                        )
                        Text(
                            when {
                                account.isCashWallet -> stringResource(R.string.wallet_cash_wallet)
                                account.isEWallet -> stringResource(R.string.wallet_e_wallet)
                                else -> stringResource(R.string.wallet_bank_account)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = contentCol.copy(alpha = 0.7f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "RM ${"%.2f".format(account.balance)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = IBMPlexMono,
                        color = contentCol
                    )
                    if (account.qrImagePath != null) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = stringResource(R.string.wallet_view_qr_cd),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onQrClick(account.qrImagePath) }
                                .padding(2.dp),
                            tint = contentCol.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountPagerDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return

    Row(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = currentPage == index
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}
