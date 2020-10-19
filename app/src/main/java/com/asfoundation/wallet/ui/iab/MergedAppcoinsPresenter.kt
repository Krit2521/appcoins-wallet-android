package com.asfoundation.wallet.ui.iab

import android.util.Log
import android.util.Pair
import com.asf.wallet.R
import com.asfoundation.wallet.billing.analytics.BillingAnalytics
import com.asfoundation.wallet.entity.Balance
import com.asfoundation.wallet.entity.TransactionBuilder
import com.asfoundation.wallet.logging.Logger
import com.asfoundation.wallet.repository.PreferencesRepositoryType
import com.asfoundation.wallet.ui.PaymentNavigationData
import com.asfoundation.wallet.ui.iab.MergedAppcoinsFragment.Companion.APPC
import com.asfoundation.wallet.ui.iab.MergedAppcoinsFragment.Companion.CREDITS
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.util.WalletCurrency
import com.asfoundation.wallet.util.isNoNetworkException
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class MergedAppcoinsPresenter(private val view: MergedAppcoinsView,
                              private val activityView: IabView,
                              private val disposables: CompositeDisposable,
                              private val resumeDisposables: CompositeDisposable,
                              private val viewScheduler: Scheduler,
                              private val networkScheduler: Scheduler,
                              private val analytics: BillingAnalytics,
                              private val formatter: CurrencyFormatUtils,
                              private val mergedAppcoinsInteractor: MergedAppcoinsInteractor,
                              private val gamificationLevel: Int,
                              private val navigator: Navigator,
                              private val logger: Logger,
                              private val transactionBuilder: TransactionBuilder,
                              private val paymentMethodsMapper: PaymentMethodsMapper,
                              private val preferencesRepositoryType: PreferencesRepositoryType) {

  companion object {
    private val TAG = MergedAppcoinsFragment::class.java.simpleName
  }

  fun present() {
    handlePaymentSelectionChange()
    handleBuyClick()
    handleBackClick()
    handleSupportClicks()
    handleErrorDismiss()
    handleAuthenticationResult()
  }

  fun onResume() {
    fetchBalance()
  }

  fun handleStop() = disposables.clear()

  fun handlePause() = resumeDisposables.clear()

  private fun fetchBalance() {
    resumeDisposables.add(Observable.zip(getAppcBalance(), getCreditsBalance(), getEthBalance(),
        Function3 { appcBalance: FiatValue, creditsBalance: Pair<Balance, FiatValue>, ethBalance: FiatValue ->
          val appcFiatValue =
              FiatValue(appcBalance.amount.plus(ethBalance.amount), appcBalance.currency,
                  appcBalance.symbol)
          MergedAppcoinsBalance(appcFiatValue, creditsBalance.second, creditsBalance.first.value)
        })
        .subscribeOn(networkScheduler)
        .observeOn(viewScheduler)
        .doOnNext {
          val appcFiat = formatter.formatCurrency(it.appcFiatValue.amount, WalletCurrency.APPCOINS)
          val creditsFiat = formatter.formatCurrency(it.creditsFiatBalance.amount,
              WalletCurrency.CREDITS)
          view.updateBalanceValues(appcFiat, creditsFiat, it.creditsFiatBalance.currency)
        }
        .observeOn(networkScheduler)
        .flatMapSingle {
          Single.zip(hasEnoughCredits(it.creditsAppcAmount),
              mergedAppcoinsInteractor.hasAppcFunds(transactionBuilder),
              BiFunction { hasCredits: Availability, hasAppc: Availability ->
                Pair(hasCredits, hasAppc)
              })
        }
        .observeOn(viewScheduler)
        .doOnNext {
          view.setPaymentsInformation(it.first.isAvailable, it.first.disableReason,
              it.second.isAvailable, it.second.disableReason)
          view.toggleSkeletons(false)
        }
        .doOnSubscribe { view.toggleSkeletons(true) }
        .subscribe({ }, { it.printStackTrace() }))
  }

  private fun hasEnoughCredits(creditsAppcAmount: BigDecimal): Single<Availability> {
    return Single.fromCallable {
      val available = creditsAppcAmount >= transactionBuilder.amount()
      val disabledReason =
          if (!available) R.string.purchase_appcoins_credits_noavailable_body else null
      Availability(available, disabledReason)
    }
  }

  private fun handleBackClick() {
    disposables.add(Observable.merge(view.backClick(), view.backPressed())
        .observeOn(networkScheduler)
        .doOnNext { paymentMethod ->
          analytics.sendPaymentConfirmationEvent(paymentMethod.packageName,
              paymentMethod.skuDetails, paymentMethod.value, paymentMethod.purchaseDetails,
              paymentMethod.transactionType, "cancel")
        }
        .observeOn(viewScheduler)
        .doOnNext { activityView.showPaymentMethodsView() }
        .subscribe({}, { showError(it) }))
  }

  private fun handleAuthenticationResult() {
    disposables.add(activityView.onAuthenticationResult()
        .observeOn(viewScheduler)
        .doOnNext {
          if (!it.isSuccess || it.paymentNavigationData == null) {
            view.hideLoading()
          } else {
            navigateToPayment(it.paymentNavigationData)
          }
        }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun navigateToPayment(paymentNavigationData: PaymentNavigationData) {
    when (paymentMethodsMapper.map(paymentNavigationData.selectedPaymentId)) {
      PaymentMethodsView.SelectedPaymentMethod.APPC -> view.navigateToAppcPayment()
      PaymentMethodsView.SelectedPaymentMethod.APPC_CREDITS -> view.navigateToCreditsPayment()
      else -> {
        view.showError(R.string.unknown_error)
        logger.log(TAG, "Wrong payment method after authentication.")
      }
    }
  }

  private fun handleBuyClick() {
    disposables.add(view.buyClick()
        .observeOn(networkScheduler)
        .doOnNext { paymentMethod ->
          analytics.sendPaymentConfirmationEvent(paymentMethod.packageName,
              paymentMethod.skuDetails, paymentMethod.value, paymentMethod.purchaseDetails,
              paymentMethod.transactionType, "buy")
        }
        .observeOn(viewScheduler)
        .doOnNext {
          view.showLoading()
        }
        .flatMapSingle { paymentMethod ->
          mergedAppcoinsInteractor.isWalletBlocked()
              .subscribeOn(networkScheduler)
              .observeOn(viewScheduler)
              .doOnSuccess {
                if (preferencesRepositoryType.hasAuthenticationPermission()) {
                  view.showAuthenticationActivity(map(paymentMethod.purchaseDetails),
                      gamificationLevel)
                } else {
                  handleBuyClickSelection(paymentMethod.purchaseDetails)
                }
              }
        }
        .subscribe({}, {
          view.hideLoading()
          showError(it)
        }))
  }

  private fun map(purchaseDetails: String): String {
    if (purchaseDetails == APPC) return PaymentMethodsView.PaymentMethodId.APPC.id
    else if (purchaseDetails == CREDITS) return PaymentMethodsView.PaymentMethodId.APPC_CREDITS.id
    return ""
  }

  private fun handleSupportClicks() {
    disposables.add(Observable.merge(view.getSupportIconClicks(), view.getSupportLogoClicks())
        .throttleFirst(50, TimeUnit.MILLISECONDS)
        .flatMapCompletable { mergedAppcoinsInteractor.showSupport(gamificationLevel) }
        .subscribe({}, { it.printStackTrace() })
    )
  }

  private fun handleErrorDismiss() {
    disposables.add(view.errorDismisses()
        .observeOn(viewScheduler)
        .doOnNext { navigator.popViewWithError() }
        .subscribe({}, {
          it.printStackTrace()
          navigator.popViewWithError()
        }))
  }

  private fun handlePaymentSelectionChange() {
    disposables.add(view.getPaymentSelection()
        .doOnNext { handleSelection(it) }
        .subscribe({}, { showError(it) }))
  }

  private fun showError(t: Throwable) {
    logger.log(TAG, t)
    if (t.isNoNetworkException()) {
      view.showError(R.string.notification_no_network_poa)
    } else {
      view.showError(R.string.activity_iab_error_message)
    }
  }

  private fun handleBuyClickSelection(selection: String) {
    when (selection) {
      APPC -> view.navigateToAppcPayment()
      CREDITS -> view.navigateToCreditsPayment()
      else -> Log.w(TAG, "No appcoins payment method selected")
    }
  }

  private fun handleSelection(selection: String) {
    when (selection) {
      APPC -> view.showBonus()
      CREDITS -> view.hideBonus()
      else -> Log.w(TAG, "Error creating PublishSubject")
    }
  }

  private fun getCreditsBalance(): Observable<Pair<Balance, FiatValue>> =
      mergedAppcoinsInteractor.getCreditsBalance()

  private fun getAppcBalance(): Observable<FiatValue> = mergedAppcoinsInteractor.getAppcBalance()

  private fun getEthBalance(): Observable<FiatValue> = mergedAppcoinsInteractor.getEthBalance()
}
