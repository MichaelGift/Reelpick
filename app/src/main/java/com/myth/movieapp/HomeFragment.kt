package com.myth.movieapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup 
class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinint var _binding: FragmentHomeBinding
    private  val binding get() = _binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _bingind =  DatabindingUtil.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding?.root
    }
}
