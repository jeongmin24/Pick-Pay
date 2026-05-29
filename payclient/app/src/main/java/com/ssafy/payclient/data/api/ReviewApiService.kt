package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.ReviewRequestDTO
import com.ssafy.payclient.data.model.ReviewResponseDTO
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ReviewApiService {
    @GET("api/reviews")
    suspend  fun getAllReviews(): List<ReviewResponseDTO>

    @POST("api/reviews")
    suspend fun createReview(
        @Body request: ReviewRequestDTO
    ): Long
}