package com.asfoundation.wallet.rating.positive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.DaggerFragment
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_rating_positive.*
import javax.inject.Inject

class RatingPositiveFragment : DaggerFragment(), RatingPositiveView {

  @Inject
  lateinit var presenter: RatingPositivePresenter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_rating_positive, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    presenter.present()
  }

  override fun initializeView(isNotFirstTime: Boolean) {
    if (isNotFirstTime) {
      title.setText(R.string.rate_us_back_title)
      description.setText(R.string.rate_us_back_body)
      animation.setMinFrame(196)
      animation.setMaxFrame(196)
    } else {
      animation.setMinFrame(97)
      animation.setMaxFrame(196)
      animation.playAnimation()
    }
  }

  override fun rateAppClickEvent(): Observable<Any> {
    return RxView.clicks(rate_app_button)
  }

  override fun remindMeLaterClickEvent(): Observable<Any> {
    return RxView.clicks(remind_me_later_button)
  }

  override fun noClickEvent(): Observable<Any> {
    return RxView.clicks(no_button)
  }

  override fun onDestroyView() {
    presenter.stop()
    super.onDestroyView()
  }

  companion object {

    @JvmStatic
    fun newInstance(): RatingPositiveFragment {
      return RatingPositiveFragment()
    }
  }
}
