// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.core.ui

import android.content.Context
import android.util.TypedValue

class DisplayUtil {
    companion object {
        fun dp2px(ctx: Context, dp: Float): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.resources.displayMetrics)
                .toInt()
        }
    }
}