package com.reviewtrust.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reviewtrust.models.AnalysisResponse
import com.reviewtrust.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representation for the Home / Analysis screen.
 */
sealed class AnalysisUiState {
    /** App opened normally — no URL shared yet. */
    data object Idle : AnalysisUiState()

    /** A URL was received but analysis has not started yet. */
    data class UrlReceived(val url: String) : AnalysisUiState()

    /** API call in progress. */
    data class Loading(val url: String) : AnalysisUiState()

    /** Analysis completed successfully. */
    data class Success(val url: String, val result: AnalysisResponse) : AnalysisUiState()

    /** An error occurred during the API call. */
    data class Error(val url: String, val message: String) : AnalysisUiState()
}

class ReviewViewModel : ViewModel() {

    companion object {
        private const val TAG = "ReviewViewModel"
    }

    private val repository = ReviewRepository()

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    /**
     * Called when a product URL is received from Share Intent.
     * Triggers the backend analysis automatically.
     */
    fun onUrlReceived(url: String) {
        Log.d(TAG, "onUrlReceived: $url")
        _uiState.value = AnalysisUiState.Loading(url)
        analyzeProduct(url, "normal")
    }

    /**
     * Run deep analysis on the current URL.
     */
    fun deepAnalysis() {
        val current = _uiState.value
        val url = when (current) {
            is AnalysisUiState.Success -> current.url
            is AnalysisUiState.Error -> current.url
            else -> return
        }
        Log.d(TAG, "deepAnalysis: re-analyzing $url in deep mode")
        _uiState.value = AnalysisUiState.Loading(url)
        analyzeProduct(url, "deep")
    }

    /**
     * Retry analysis for the last URL.
     */
    fun retry() {
        val current = _uiState.value
        if (current is AnalysisUiState.Error) {
            Log.d(TAG, "retry: re-analyzing ${current.url}")
            _uiState.value = AnalysisUiState.Loading(current.url)
            analyzeProduct(current.url, "normal")
        }
    }

    private fun analyzeProduct(url: String, mode: String = "normal") {
        viewModelScope.launch {
            val result = repository.analyzeProduct(url, mode)
            result.fold(
                onSuccess = { response ->
                    Log.i(TAG, "analyzeProduct: success — trustScore=${response.trustScore}")
                    _uiState.value = AnalysisUiState.Success(url, response)
                },
                onFailure = { throwable ->
                    val msg = throwable.message ?: "Unknown error occurred"
                    Log.e(TAG, "analyzeProduct: failure — $msg", throwable)
                    _uiState.value = AnalysisUiState.Error(url, msg)
                }
            )
        }
    }
}
