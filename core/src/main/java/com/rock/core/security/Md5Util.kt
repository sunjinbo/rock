// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.core.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class Md5Util {
    companion object {
        fun getMd5(text: String): String {
            try {
                // Create MD5 Hash
                val digest = MessageDigest.getInstance("MD5")
                digest.update(text.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = java.lang.StringBuilder()
                for (aMessageDigest in messageDigest) {
                    var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                    while (h.length < 2) h = "0$h"
                    hexString.append(h)
                }
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return ""
        }
    }
}
