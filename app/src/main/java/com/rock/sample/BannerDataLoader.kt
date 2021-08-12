// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.sample

import android.os.SystemClock
import com.rock.ui.CardInfo
import com.rock.ui.DataLoader
import com.rock.ui.JumpInfo

class BannerDataLoader : DataLoader() {
    override fun loadOfflineData(): List<CardInfo> {
        return ArrayList()
    }

    override fun loadOnlineData(callback: Callback) {
        Thread {
            SystemClock.sleep(333L)

            var cardList = mutableListOf<CardInfo>()
                .apply {
                    add(CardInfo("https://tenfei03.cfp.cn/creative/vcg/veer/800water/veer-131730927.jpg", JumpInfo("https://www.baidu.com", 0)))
                    add(CardInfo("https://alifei04.cfp.cn/creative/vcg/veer/800water/veer-134589743.jpg", JumpInfo("https://www.google.com", 0)))
                    add(CardInfo("https://tenfei04.cfp.cn/creative/vcg/veer/800water/veer-133253238.jpg", JumpInfo("https://www.qq.com", 0)))
                }
            callback?.onDataLoaded(cardList)
        }.start()
    }
}
