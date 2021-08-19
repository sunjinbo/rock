// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import com.rock.core.ui.DisplayUtil

class CircleImageView : AbsImageView {

    /**
     * 边线宽度.
     */
    var strokeWidth: Float = 0F
        get() = DisplayUtil.px2dip(context, field).toFloat()
        set(value) {
            field = DisplayUtil.dip2px(context, value).toFloat()
            postInvalidate()
        }

    /**
     * 边线颜色.
     */
    var strokeColor: Int = Color.LTGRAY
        set(value) {
            field = value
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