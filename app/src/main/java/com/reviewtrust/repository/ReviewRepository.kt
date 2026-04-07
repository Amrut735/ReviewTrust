package com.reviewtrust.repository

import android.util.Log
import com.reviewtrust.models.AnalysisRequest
import com.reviewtrust.models.AnalysisResponse
import com.reviewtrust.network.RetrofitClient

/**
 * Repository that mediates between the network layer and the UI.
 */
class ReviewRepository {

    companion object {
        private const val TAG = "ReviewRepository"
    }

    private val apiService = RetrofitClient.instance

    /**
     * Sends a product URL to the backend for fake-review analysis.
     */
    suspend fun analyzeProduct(productUrl: String, analysisMode: String = "normal"): Result<AnalysisResponse> {
        return try {
            Log.d(TAG, "analyzeProduct: sending request for $productUrl (mode=$analysisMode)")
            val response = apiService.analyzeProduct(AnalysisRequest(productUrl, analysisMode))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.i(TAG, "analyzeProduct: success — trust_score=${body.trustScore}")
                    Result.success(body)
                } else {
                    Log.w(TAG, "analyzeProduct: response body is null")
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "analyzeProduct: HTTP ${response.code()} — $errorMsg")
                Result.failure(Exception("Server error ${response.code()}: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "analyzeProduct: network failure", e)
            Result.failure(e)
        }
    }
}
