package com.ssafy.payclient.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadWelcomeMessage()
    }

    private fun loadWelcomeMessage() {
        val tokenManager = TokenManager(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getUserApiService(tokenManager)
                val userResponse = apiService.getUserInfo()

                binding.tvWelcomeTitle.text = "안녕하세요, ${userResponse.nickname}님! ☕"
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvWelcomeTitle.text = "안녕하세요!"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}