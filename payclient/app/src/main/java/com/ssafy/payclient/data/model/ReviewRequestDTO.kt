package com.ssafy.payclient.data.model

import java.io.Serializable

data class ReviewRequestDTO(
    val content: String,
    val rating: Int,
    val imageUrl: String?,
) : Serializable