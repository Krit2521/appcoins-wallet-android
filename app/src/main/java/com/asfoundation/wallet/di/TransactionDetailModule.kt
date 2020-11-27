package com.asfoundation.wallet.di

import com.asfoundation.wallet.interact.FindDefaultNetworkInteract
import com.asfoundation.wallet.interact.FindDefaultWalletInteract
import com.asfoundation.wallet.router.ExternalBrowserRouter
import com.asfoundation.wallet.router.TransactionDetailRouter
import com.asfoundation.wallet.support.SupportInteractor
import com.asfoundation.wallet.viewmodel.TransactionDetailViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import io.reactivex.disposables.CompositeDisposable

@InstallIn(ActivityComponent::class)
@Module
class TransactionDetailModule {
  @Provides
  fun provideTransactionDetailViewModelFactory(
      findDefaultNetworkInteract: FindDefaultNetworkInteract,
      findDefaultWalletInteract: FindDefaultWalletInteract,
      externalBrowserRouter: ExternalBrowserRouter, supportInteractor: SupportInteractor,
      transactionDetailRouter: TransactionDetailRouter): TransactionDetailViewModelFactory {
    return TransactionDetailViewModelFactory(findDefaultNetworkInteract, findDefaultWalletInteract,
        externalBrowserRouter, CompositeDisposable(), supportInteractor, transactionDetailRouter)
  }
}