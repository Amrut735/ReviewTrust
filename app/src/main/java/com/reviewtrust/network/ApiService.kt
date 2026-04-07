package com.reviewtrust.network

import com.reviewtrust.models.AnalysisRequest
import com.reviewtrust.models.AnalysisResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API service interface for the ReviewTrust backend.
 */
interface ApiService {

    @POST("analyze")
    suspend fun analyzeProduct(@Body request: AnalysisRequest): Response<AnalysisResponse>
}
