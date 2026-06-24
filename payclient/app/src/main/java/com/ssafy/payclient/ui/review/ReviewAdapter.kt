package com.ssafy.payclient.ui.review

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.payclient.data.model.ReviewResponseDTO
import com.ssafy.payclient.databinding.ItemReviewBinding

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var reviewList = listOf<ReviewResponseDTO>()
    private var onItemClickListener: ((ReviewResponseDTO) -> Unit)? = null

    fun setReviews(list: List<ReviewResponseDTO>) {
        reviewList = list
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (ReviewResponseDTO) -> Unit) {
        onItemClickListener = listener
    }

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: ReviewResponseDTO) {
            binding.tvReviewInitial.text = review.nickname.firstInitial()
            binding.tvNickname.text = review.nickname
            binding.tvReviewDate.text = review.createdAt.toReviewDate()
            binding.tvRating.text = "★ ${review.rating.coerceIn(0, 5)}"
            binding.tvContent.text = "\"${review.content}\""

            if (review.imageUrl.isNullOrBlank()) {
                binding.ivReviewImage.visibility = View.GONE
            } else {
                binding.ivReviewImage.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(review.imageUrl)
                    .centerCrop()
                    .into(binding.ivReviewImage)
            }

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(review)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviewList[position])
    }

    override fun getItemCount(): Int = reviewList.size

    private fun String.firstInitial(): String {
        return trim().firstOrNull()?.toString()?.uppercase() ?: "?"
    }

    private fun String.toReviewDate(): String {
        if (isBlank()) return "방금 전"
        return take(10).replace("-", ".")
    }
}
