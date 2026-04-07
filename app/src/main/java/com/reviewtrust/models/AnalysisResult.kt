package com.reviewtrust.models

import com.google.gson.annotations.SerializedName

/**
 * Request payload sent to POST /analyze.
 */
data class AnalysisRequest(
    @SerializedName("product_url")
    val productUrl: String,

    @SerializedName("analysis_mode")
    val analysisMode: String = "normal"
)

/**
 * A single review with its LIME-generated explanation.
 */
data class ReviewDetail(
    val text: String = "",
    val explanation: List<String> = emptyList()
)

/**
 * Response received from POST /analyze.
 */
data class AnalysisResponse(
    @SerializedName("trust_score")
    val trustScore: Int = 0,

    @SerializedName("fake_percentage")
    val fakePercentage: Int = 0,

    @SerializedName("genuine_percentage")
    val genuinePercentage: Int = 0,

    @SerializedName("total_reviews")
    val totalReviews: Int = 0,

    @SerializedName("fake_count")
    val fakeCount: Int = 0,

    @SerializedName("genuine_count")
    val genuineCount: Int = 0,

    @SerializedName("recommendation")
    val recommendation: String = "",

    @SerializedName("analysis_mode")
    val analysisMode: String = "normal",

    @SerializedName("cached")
    val cached: Boolean = false,

    @SerializedName("fake_reviews")
    val fakeReviews: List<ReviewDetail> = emptyList(),

    @SerializedName("genuine_reviews")
    val genuineReviews: List<ReviewDetail> = emptyList(),

    @SerializedName("sentiment")
    val sentiment: SentimentResult? = null,

    @SerializedName("reviewer_quality")
    val reviewerQuality: ReviewerQualityResult? = null,

    @SerializedName("spike_detection")
    val spikeDetection: SpikeDetectionResult? = null,

    @SerializedName("genuine_summary")
    val genuineSummary: GenuineSummaryResult? = null
)

data class SentimentResult(
    @SerializedName("positive_pct") val positivePct: Float = 0f,
    @SerializedName("negative_pct") val negativePct: Float = 0f,
    @SerializedName("neutral_pct") val neutralPct: Float = 0f,
    @SerializedName("average_compound") val averageCompound: Float = 0f
)

data class ReviewerQualityResult(
    @SerializedName("suspicious_reviewer_pct") val suspiciousReviewerPct: Float = 0f,
    @SerializedName("unverified_pct") val unverifiedPct: Float = 0f,
    @SerializedName("thin_account_count") val thinAccountCount: Int = 0,
    @SerializedName("mass_reviewer_count") val massReviewerCount: Int = 0,
    val platform: String = "",
    @SerializedName("data_limitation") val dataLimitation: String? = null
)

data class SpikePeriod(
    val period: String,
    val count: Int
)

data class SpikeDetectionResult(
    val granularity: String,
    val timeline: List<SpikePeriod>,
    @SerializedName("spike_periods") val spikePeriods: List<String>,
    @SerializedName("spike_detected") val spikeDetected: Boolean,
    val platform: String,
    @SerializedName("data_limitation") val dataLimitation: String? = null
)

data class GenuineSummaryResult(
    val summary: String?,
    @SerializedName("genuine_review_count_used") val genuineReviewCountUsed: Int?,
    @SerializedName("max_sentences") val maxSentences: Int?,
    val reason: String? = null
)
