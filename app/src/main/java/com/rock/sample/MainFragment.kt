// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.sample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation

class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_main, container, false)
        view.findViewById<Button>(R.id.banner).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_bannerFragment)
        }
        return view
    }
}