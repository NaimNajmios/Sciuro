package com.sciuro.feature.wallet.di

import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import com.sciuro.feature.wallet.viewmodel.AccountDetailViewModel
import com.sciuro.feature.wallet.viewmodel.OnboardingViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val walletModule = module {
    viewModel { WalletViewModel(get(), get(), get(), get()) }
    viewModel { AccountDetailViewModel(get(), get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}
