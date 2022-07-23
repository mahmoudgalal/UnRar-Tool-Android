/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool

import android.content.Context
import android.widget.FrameLayout
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import java.util.concurrent.atomic.AtomicBoolean

class Splash @JvmOverloads constructor(
    context: Context,
    private val onSplashEnded: Runnable? = null
) : FrameLayout(context) {
    var upAnim: Animation
    var downAnim: Animation
    var upAnimListener: AnimationListener
    var downAnimListener: AnimationListener
    var closeRunnable: Runnable? = null
    private val downAnimRunning: AtomicBoolean
    private val upAnimRunning: AtomicBoolean
    private val splashEnded = AtomicBoolean(false)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        upAnimRunning.set(true)
        postDelayed({ startAnimation(upAnim) }, 3000)
    }

    fun isSplashEnded(): Boolean {
        return splashEnded.get()
    }

    fun closeSplash(close: Runnable?) {
        if (downAnimRunning.get() || upAnimRunning.get()) return
        closeRunnable = close
        downAnimRunning.set(true)
        startAnimation(downAnim)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.splash, this)
        isClickable = true
        downAnimRunning = AtomicBoolean(false)
        upAnimRunning = AtomicBoolean(false)
        upAnim = AnimationUtils.loadAnimation(context, R.anim.move_up)
        downAnim = AnimationUtils.loadAnimation(context, R.anim.move_down)
        upAnimListener = object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                splashEnded.set(false)
            }

            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                visibility = GONE
                isClickable = false
                upAnimRunning.set(false)
                splashEnded.set(true)
                onSplashEnded?.run()
            }
        }
        upAnim.setAnimationListener(upAnimListener)
        downAnimListener = object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                downAnimRunning.set(true)
                isClickable = true
                visibility = VISIBLE
                splashEnded.set(false)
            }

            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                if (closeRunnable != null) {
                    postDelayed(closeRunnable, 1100)
                    //splashEnded.set(true);
                }
            }
        }
        downAnim.setAnimationListener(downAnimListener)
    }
}