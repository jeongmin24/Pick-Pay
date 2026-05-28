package com.ssafy.payclient.data.model

import java.io.Serializable

data class ReviewResponseDTO(
    val reviewId: Long,
    val content: String,
    val rating: Int,
    val imageUrl: String?,
    val createdAt: String,
    val userId: Long,
    val nickname: String,
    val profileUrl: String?
) : Serializable
