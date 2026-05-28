package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.ReviewResponseDTO
import retrofit2.http.GET

interface ReviewApiService {
    @GET("api/reviews")
    suspend  fun getAllReviews(): List<ReviewResponseDTO>
}