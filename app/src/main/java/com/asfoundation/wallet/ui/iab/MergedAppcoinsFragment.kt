package com.asfoundation.wallet.ui.iab

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.asf.wallet.R
import com.asfoundation.wallet.entity.TransactionBuilder
import dagger.android.support.DaggerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.appcoins_radio_button.*
import kotlinx.android.synthetic.main.appcoins_radio_button.bonus_layout
import kotlinx.android.synthetic.main.appcoins_radio_button.view.*
import kotlinx.android.synthetic.main.credits_radio_button.*
import kotlinx.android.synthetic.main.dialog_buy_buttons.*
import kotlinx.android.synthetic.main.fragment_iab_error.*
import kotlinx.android.synthetic.main.local_payment_layout.*
import kotlinx.android.synthetic.main.merged_appcoins_layout.*
import kotlinx.android.synthetic.main.merged_appcoins_layout.view.bonus_layout
import kotlinx.android.synthetic.main.payment_methods_header.*
import kotlinx.android.synthetic.main.view_purchase_bonus.*
import kotlinx.android.synthetic.main.view_purchase_bonus.view.*
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class MergedAppcoinsFragment : DaggerFragment(), MergedAppcoinsView {

  companion object {
    private const val TRANSACTION_KEY = "transaction"
    private const val FIAT_AMOUNT_KEY = "fiat_amount"
    private const val FIAT_CURRENCY_KEY = "currency_amount"
    private const val BONUS_KEY = "bonus"
    private const val APP_NAME_KEY = "app_name"
    private const val SKU_ID_KEY = "skuId"
    private const val APPC_AMOUNT_KEY = "appc_amount"
    private const val APPC_ENABLED_KEY = "appc_enabled"
    private const val CREDITS_ENABLED_KEY = "credits_enabled"

    @JvmStatic
    fun newInstance(transaction: TransactionBuilder, fiatAmount: BigDecimal,
                    currency: String, bonus: String, appName: String, skuId: String,
                    appcAmount: BigDecimal, appcEnabled: Boolean,
                    creditsEnabled: Boolean): Fragment {
      val fragment = MergedAppcoinsFragment()
      val bundle = Bundle()
      bundle.putParcelable(TRANSACTION_KEY, transaction)
      bundle.putSerializable(FIAT_AMOUNT_KEY, fiatAmount)
      bundle.putString(FIAT_CURRENCY_KEY, currency)
      bundle.putString(BONUS_KEY, bonus)
      bundle.putString(APP_NAME_KEY, appName)
      bundle.putString(SKU_ID_KEY, skuId)
      bundle.putSerializable(APPC_AMOUNT_KEY, appcAmount)
      bundle.putBoolean(APPC_ENABLED_KEY, appcEnabled)
      bundle.putBoolean(CREDITS_ENABLED_KEY, creditsEnabled)
      fragment.arguments = bundle
      return fragment
    }
  }

  private lateinit var mergedAppcoinsPresenter: MergedAppcoinsPresenter
  @Inject
  lateinit var inAppPurchaseInteractor: InAppPurchaseInteractor

  private val transaction: TransactionBuilder by lazy {
    if (arguments!!.containsKey(TRANSACTION_KEY)) {
      arguments!!.getParcelable(TRANSACTION_KEY) as TransactionBuilder
    } else {
      throw IllegalArgumentException("transaction data not found")
    }
  }
  private val fiatAmount: BigDecimal by lazy {
    if (arguments!!.containsKey(FIAT_AMOUNT_KEY)) {
      arguments!!.getSerializable(FIAT_AMOUNT_KEY) as BigDecimal
    } else {
      throw IllegalArgumentException("amount data not found")
    }
  }
  private val currency: String by lazy {
    if (arguments!!.containsKey(FIAT_CURRENCY_KEY)) {
      arguments!!.getString(FIAT_CURRENCY_KEY)
    } else {
      throw IllegalArgumentException("currency data not found")
    }
  }

  private val bonus: String by lazy {
    if (arguments!!.containsKey(BONUS_KEY)) {
      arguments!!.getString(BONUS_KEY)
    } else {
      throw IllegalArgumentException("bonus data not found")
    }
  }

  private val appName: String by lazy {
    if (arguments!!.containsKey(APP_NAME_KEY)) {
      arguments!!.getString(APP_NAME_KEY)
    } else {
      throw IllegalArgumentException("app name data not found")
    }
  }

  private val skuId: String by lazy {
    if (arguments!!.containsKey(SKU_ID_KEY)) {
      arguments!!.getString(SKU_ID_KEY)
    } else {
      throw IllegalArgumentException("sku data not found")
    }
  }

  private val appcAmount: BigDecimal by lazy {
    if (arguments!!.containsKey(APPC_AMOUNT_KEY)) {
      arguments!!.getSerializable(APPC_AMOUNT_KEY) as BigDecimal
    } else {
      throw IllegalArgumentException("appc data not found")
    }
  }

  private val appcEnabled: Boolean by lazy {
    if (arguments!!.containsKey(APPC_ENABLED_KEY)) {
      arguments!!.getBoolean(APPC_ENABLED_KEY)
    } else {
      throw IllegalArgumentException("appc enable data not found")
    }
  }

  private val creditsEnabled: Boolean by lazy {
    if (arguments!!.containsKey(CREDITS_ENABLED_KEY)) {
      arguments!!.getBoolean(CREDITS_ENABLED_KEY)
    } else {
      throw IllegalArgumentException("credits enable data not found")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mergedAppcoinsPresenter =
        MergedAppcoinsPresenter(this, transaction, fiatAmount.toString(), currency,
            inAppPurchaseInteractor, CompositeDisposable(), AndroidSchedulers.mainThread(),
            Schedulers.io())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    try {
      app_icon.setImageDrawable(context!!.packageManager
          .getApplicationIcon(appName))
      app_name.text = getApplicationName(appName)
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
    }
    app_sku_description.text = skuId
    fiat_price.text =
        String.format(fiatAmount.setScale(2, RoundingMode.FLOOR).toString() + " " + currency)
    appc_price.text = String.format(appcAmount.setScale(2, RoundingMode.FLOOR).toString() + " APPC")
    buy_button.text = getString(R.string.action_buy)
    cancel_button.text = getString(R.string.back_button)

    setBonus()
    setRadioButtonListeners()
    setPaymentInformation()
    payment_methods.visibility = VISIBLE
    mergedAppcoinsPresenter.present()
  }

  private fun setBonus() {
    //Build string for both landscape (header) and portrait (radio button) bonus layout
    appcoins_radio?.bonus_value?.text =
        getString(R.string.gamification_purchase_header_part_2, bonus)
    bonus_value?.text = getString(R.string.gamification_purchase_header_part_2, bonus)

    //Set visibility for both landscape (header) and portrait (radio button) bonus layout
    bonus_layout?.visibility = VISIBLE
    appcoins_radio?.bonus_layout?.visibility = VISIBLE
    bonus_msg?.visibility = VISIBLE
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.merged_appcoins_layout, container, false)
  }

  private fun setPaymentInformation() {
    if (appcEnabled) {
      appcoins_radio_button.isEnabled = true
    } else {
      appcoins_radio.message.text = "You don't have enough AppCoins or Ethereum."
      appcoins_radio.title.setTextColor(resources.getColor(R.color.btn_disable_snd_color))
      appcoins_radio.message.setTextColor(resources.getColor(R.color.btn_disable_snd_color))
      appcoins_radio?.bonus_layout?.setBackgroundColor(
          resources.getColor(R.color.btn_disable_snd_color))
    }
    if (creditsEnabled) {
      credits_radio_button.isEnabled = true
      credits_radio_button.isChecked = true
    } else {
      appcoins_radio_button.isChecked = true
      credits_radio.message.text =
          "You don't have enough Credits in your balance, try topping up."
      credits_radio.title.setTextColor(resources.getColor(R.color.btn_disable_snd_color))
      credits_radio.message.setTextColor(resources.getColor(R.color.btn_disable_snd_color))
    }
  }

  override fun showError(errorMessage: Int) {
    payment_method_main_view.visibility = GONE
    activity_iab_error_message.text = getString(errorMessage)
    error_view.visibility = VISIBLE
  }

  private fun getApplicationName(appPackage: String): CharSequence {
    val packageManager = context!!.packageManager
    val packageInfo = packageManager.getApplicationInfo(appPackage, 0)
    return packageManager.getApplicationLabel(packageInfo)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    appcoins_radio_button.setOnClickListener(null)
    credits_radio_button.setOnClickListener(null)
  }

  private fun setRadioButtonListeners() {
    appcoins_radio_button.setOnClickListener { credits_radio_button.isChecked = false }
    credits_radio_button.setOnClickListener { appcoins_radio_button.isChecked = false }
  }

}
