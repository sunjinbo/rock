// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

import android.os.Bundle
import android.text.TextUtils

data class JumpInfo(val url: String, val action:Int, val extras:Bundle) : Comparable<JumpInfo> {

    constructor(url: String, action:Int) : this(url, action, Bundle())

    override fun compareTo(other: JumpInfo): Int {
        if (this == other) return 0
        if (TextUtils.equals(url, other.url) && action == other.action) return 0
        return -1
    }
}