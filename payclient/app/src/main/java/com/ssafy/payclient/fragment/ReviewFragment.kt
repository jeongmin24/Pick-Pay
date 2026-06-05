package com.ssafy.payclient.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.payclient.BuildConfig.BASE_URL
import com.ssafy.payclient.MainViewModel
import com.ssafy.payclient.R
import com.ssafy.payclient.ui.review.ReviewDetailActivity
import com.ssafy.payclient.data.api.ReviewApiService
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentReviewBinding
import com.ssafy.payclient.ui.review.ReviewAdapter
import com.ssafy.payclient.ui.review.ReviewAddActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReviewFragment : Fragment(R.layout.fragment_review) {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var apiService: ReviewApiService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReviewBinding.bind(view)

        initRetrofit()
        setupRecyclerView()

        binding.fabAddReview.setOnClickListener {
            val intent = Intent(requireContext(), ReviewAddActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchReviewsWithCoroutine()
    }

    private fun initRetrofit() {
        val tokenManager = TokenManager(requireContext())
        apiService = RetrofitClient.getReviewApiService(tokenManager)

    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.recyclerViewReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        reviewAdapter.setOnItemClickListener { review ->
            val intent = Intent(requireContext(), ReviewDetailActivity::class.java).apply {
                putExtra("review_data", review)
            }
            startActivity(intent)
        }
    }

    private fun fetchReviewsWithCoroutine() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val reviews = withContext(Dispatchers.IO) {
                    apiService.getAllReviews()
                }

                if (isAdded) {
//                    context?.let { safeContext ->
//                        Toast.makeText(safeContext, "가져온 리뷰 개수: ${reviews.size}개", Toast.LENGTH_SHORT).show()
//                    }

                    reviewAdapter.setReviews(reviews)
                }
            } catch (e: Exception) {
                if (isAdded) {
                    context?.let { safeContext ->
                        Toast.makeText(safeContext, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}