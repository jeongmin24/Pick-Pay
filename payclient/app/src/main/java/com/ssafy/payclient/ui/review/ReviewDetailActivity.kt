package com.ssafy.payclient.ui.review

import android.R.attr.rating
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.ssafy.payclient.R
import com.ssafy.payclient.data.model.ReviewResponseDTO
import com.ssafy.payclient.databinding.ActivityReviewDetailBinding
import com.ssafy.payclient.databinding.FragmentReviewBinding

class ReviewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val review = intent.getSerializableExtra("review_data") as ReviewResponseDTO

        review?.let {
            binding.tvNickname.text = it.nickname
            binding.detailCreatedAt.text = it.createdAt
            binding.rbRating.rating = it.rating.toFloat() // RatingBar는 Float를 받으므로 변환
            binding.detailContent.text = it.content

            // Glide로 서버 이미지 매핑
            Glide.with(this).load(it.profileUrl).into(binding.ivProfile)
            Glide.with(this).load(it.imageUrl).into(binding.ivImageView)
        }

    }
}