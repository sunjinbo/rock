// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.rock.core.metrology.DisplayUtil

class RoundImageView : AbsImageView {

    var roundCorner: Float = 0F
        get() = DisplayUtil.px2dip(context, field).toFloat()
        set(value) {
            field = DisplayUtil.dip2px(context, value).toFloat()
            postInvalidate()
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }
}