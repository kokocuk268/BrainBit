package com.brainfocus.app.ui.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewAnimationUtils

object CircularRevealHelper {
    fun applyCircularReveal(
        rootView: View,
        startX: Int,
        startY: Int,
        duration: Long = 600,
        onEnd: (() -> Unit)? = null
    ) {
        val cx = startX.coerceIn(0, rootView.width)
        val cy = startY.coerceIn(0, rootView.height)

        val startRadius = 0f
        val endRadius = kotlin.math.max(
            kotlin.math.hypot(cx.toDouble(), cy.toDouble()),
            kotlin.math.hypot(
                (rootView.width - cx).toDouble(),
                (rootView.height - cy).toDouble()
            )
        ).toFloat()

        val anim = ViewAnimationUtils.createCircularReveal(
            rootView, cx, cy, startRadius, endRadius
        ).apply {
            this.duration = duration
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                }
            })
        }

        rootView.visibility = View.VISIBLE
        anim.start()
    }
}
