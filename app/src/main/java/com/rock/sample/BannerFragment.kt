// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.sample

import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rock.core.metrology.DisplayUtil
import com.rock.ui.Banner

class BannerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_banner, container, false)

        val banner = view.findViewById<Banner>(R.id.banner)
        val metric = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metric)
        val width = metric.widthPixels - (DisplayUtil.dip2px(requireContext(), 16F) * 2)
        val height = (width * 0.67F).toInt()
        val lp = banner!!.layoutParams
        lp.width = width
        lp.height = height
        banner.layoutParams = lp

        banner.refreshData()

        return view
    }
}