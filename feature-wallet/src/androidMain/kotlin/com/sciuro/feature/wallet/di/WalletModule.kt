package com.sciuro.feature.wallet.di

import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val walletModule = module {
    viewModel { WalletViewModel() }
}
