// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

data class CardInfo(val urlImage: String, val jumpTo: JumpInfo, val weight: Int) : Comparable<CardInfo> {

    constructor(urlImage: String, jumpTo: JumpInfo) : this(urlImage, jumpTo, 0)

    override fun compareTo(other: CardInfo): Int {
        return when {
            weight > other.weight -> 1
            weight < other.weight -> -1
            else -> 0
        }
    }
}
