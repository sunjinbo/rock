// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.core.net

import com.rock.logger.Log

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object FileDownloadUtil {
//    suspend fun downloadFile(url: String, file: File) {
//        GlobalScope.launch {
//
//
//        }
//
//
//        Log.d("Begin to download $url to ${file.absolutePath}")
//        threadPool!!.execute(Runnable {
//            try {
//                var url = URL(url)
//                var httpConnection = url.openConnection()
//                httpConnection.connectTimeout = 5000
//                httpConnection.connect()
//                var inputStream = httpConnection.getInputStream()
//                val outputStream = FileOutputStream(file)
//                val buffer = ByteArray(1024 * 4)
//                val byteStream = ByteArrayOutputStream()
//                while (true) {
//                    var length = inputStream.read(buffer)
//                    if (length > 0) {
//                        byteStream.write(buffer, 0, length)
//                        outputStream.write(buffer, 0, length)
//                    } else {
//                        break
//                    }
//                }
//
//                outputStream.flush()
//                outputStream.close()
//                byteStream.close()
//                inputStream.close()
//            } catch (e: java.lang.Exception) {
//                Log.e(e)
//            }
//        })
//    }
}