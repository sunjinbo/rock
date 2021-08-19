// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.rock.logger.Log

abstract class AbsImageView : AppCompatImageView {

    private var loadingBitmap: Bitmap? = null

    /**
     * 控件背景底色.
     */
    var areaColor: Int = Color.WHITE
        set(value) {
            field = value
            postInvalidate()
        }

    /**
     * 网络加载中显示的占位图.
     */
    var loadingImage: Drawable? = null
        set(value) {
            field = value
            loadingBitmap = field?.let { (field as BitmapDrawable).bitmap }
            postInvalidate()
        }

    /**
     * 是否是debug模式.
     */
    var debug: Boolean
        get() = Log.debug
        set(value) {
            Log.debug = value
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
}