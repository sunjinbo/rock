// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.logger

import java.lang.Exception

class Log {
    companion object {
        private const val tag = "rock"

        var debug: Boolean = false

        fun d(message : String) {
            if (debug) {
                android.util.Log.d(tag, message)
            }
        }

        fun w(message: String) {
            if (debug) {
                android.util.Log.w(tag, message)
            }
        }

        fun e(message: String) {
            if (debug) {
                android.util.Log.d(tag, message)
            }
        }

        fun e(exception: Exception) {
            if (debug) {
                android.util.Log.d(tag, exception.message!!)
            }
        }
    }
}
