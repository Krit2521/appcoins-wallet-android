package com.asfoundation.wallet.rating.negative

import android.animation.Animator
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.DaggerFragment
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_rating_entry.no_button
import kotlinx.android.synthetic.main.fragment_rating_negative.*
import javax.inject.Inject

class RatingNegativeFragment : DaggerFragment(), RatingNegativeView {

  @Inject
  lateinit var presenter: RatingNegativePresenter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_rating_negative, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setAndRunAnimation()
    setTextWatcher()
    presenter.present()
  }

  private fun setAndRunAnimation() {
    animation.setMinFrame(97)
    animation.setMaxFrame(123)
    animation.playAnimation()
    animation.addAnimatorListener(object : Animator.AnimatorListener {
      override fun onAnimationRepeat(anim: Animator) = Unit
      override fun onAnimationEnd(anim: Animator) {
        if (animation.minFrame == 97f) {
          animation.setMaxFrame(283)
          animation.setMinFrame(219)
          animation.playAnimation()
        }
      }

      override fun onAnimationCancel(anim: Animator) = Unit
      override fun onAnimationStart(anim: Animator) = Unit
    })
  }

  private fun setTextWatcher() {
    feedback_input_text.addTextWatcher(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        if (!TextUtils.isEmpty(s)) feedback_input_text.reset()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    })
  }

  override fun submitClickEvent(): Observable<String> {
    return RxView.clicks(submit_button)
        .map { feedback_input_text.getText() }
  }

  override fun noClickEvent(): Observable<Any> {
    return RxView.clicks(no_button)
  }

  override fun setLoading() {
    progress_bar.visibility = View.VISIBLE
    feedback_input_text.visibility = View.INVISIBLE
    submit_button.isEnabled = false
  }

  override fun showEmptySuggestionsError() {
    feedback_input_text.setError(getString(R.string.rate_us_improve_field__empty_error))
  }

  override fun onDestroyView() {
    presenter.stop()
    super.onDestroyView()
  }

  companion object {

    @JvmStatic
    fun newInstance(): RatingNegativeFragment {
      return RatingNegativeFragment()
    }
  }
}
