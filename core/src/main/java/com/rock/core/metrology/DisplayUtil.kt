// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.core.metrology

import android.content.Context
import android.util.TypedValue

object DisplayUtil {
    /**
     * 将dip转换成px.
     */
    fun dip2px(ctx: Context, dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.resources.displayMetrics)
            .toInt()
    }

    /**
     * 将px转换成dip.
     */
    fun px2dip(ctx: Context, pxValue: Float): Int {
        val scale: Float = ctx.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
}
