package com.ssafy.payclient.ui.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.ReviewResponseDTO
import com.ssafy.payclient.databinding.ItemReviewBinding

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var reviewList = listOf<ReviewResponseDTO>()
    private var onItemClickListener: ((ReviewResponseDTO) -> Unit)? = null

    fun setReviews(list: List<ReviewResponseDTO>) {
        this.reviewList = list
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (ReviewResponseDTO) -> Unit) {
        this.onItemClickListener = listener
    }

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: ReviewResponseDTO) {
            binding.tvNickname.text = review.nickname
            binding.tvRating.text = "⭐ ${review.rating}"
            binding.tvContent.text = review.content

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

    override fun onBindViewHolder(holder: ReviewAdapter.ReviewViewHolder, position: Int) {
        holder.bind(reviewList[position])
    }

    override fun getItemCount(): Int = reviewList.size


}