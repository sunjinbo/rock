// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

import android.content.Context

abstract class DataLoader {
    interface Callback {
        fun onDataLoaded(list: MutableList<CardInfo>)
        fun onLoadFailed(error:Int, message: String)
    }

    var context:Context? = null
    var test:Boolean = false
    var appKey:String = ""
    var appSecret:String = ""

    abstract fun loadOfflineData(): List<CardInfo>
    abstract fun loadOnlineData(callback: Callback)
}
