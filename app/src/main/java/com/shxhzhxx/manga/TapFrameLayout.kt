package com.shxhzhxx.manga

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class TapFrameLayout : FrameLayout {
    private val mDetector: GestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                mListener?.onViewTap(this@TapFrameLayout, e.rawX, e.rawY)
                return false
            }
        })
    }
    private var mListener: OnViewTapListener? = null

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        mDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    fun setOnViewTapListener(listener: OnViewTapListener?) {
        mListener = listener
    }
}

interface OnViewTapListener {
    fun onViewTap(v: View, x: Float, y: Float)
}