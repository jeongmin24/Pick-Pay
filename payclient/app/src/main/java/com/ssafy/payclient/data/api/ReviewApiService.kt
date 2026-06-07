package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.ReviewResponseDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ReviewApiService {
    @GET("api/reviews")
    suspend  fun getAllReviews(): List<ReviewResponseDTO>

    @Multipart
    @POST("api/reviews")
    suspend fun createReview(
        @Part("content") content: String,
        @Part("rating") rating: String,
        @Part image: MultipartBody.Part?
    ): Long
}