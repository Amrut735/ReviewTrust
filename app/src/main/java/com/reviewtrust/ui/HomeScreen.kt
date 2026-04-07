package com.reviewtrust.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import com.reviewtrust.models.GenuineSummaryResult
import com.reviewtrust.models.ReviewerQualityResult
import com.reviewtrust.models.SentimentResult
import com.reviewtrust.models.SpikeDetectionResult
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.reviewtrust.charts.ChartHelper
import com.reviewtrust.models.AnalysisResponse
import com.reviewtrust.models.ReviewDetail
import com.reviewtrust.ui.theme.Green500
import com.reviewtrust.ui.theme.Orange500
import com.reviewtrust.ui.theme.Red500
import com.reviewtrust.ui.theme.ReviewTrustTheme

// ═════════════════════════════════════════════════════════════════════════════
//  HomeScreen (entry point — observes ViewModel)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(viewModel: ReviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(
        state = uiState,
        onRetry = { viewModel.retry() },
        onDeepAnalysis = { viewModel.deepAnalysis() }
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  HomeContent (state-driven, also used by previews)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun HomeContent(
    state: AnalysisUiState,
    onRetry: () -> Unit = {},
    onDeepAnalysis: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // ── App header ──────────────────────────────────────────────────
        AppHeader()

        Spacer(modifier = Modifier.height(28.dp))

        // ── State-based content ─────────────────────────────────────────
        when (state) {
            is AnalysisUiState.Idle -> DefaultInstructionsCard()
            is AnalysisUiState.UrlReceived -> SharedUrlCard(url = state.url)
            is AnalysisUiState.Loading -> LoadingCard(url = state.url)
            is AnalysisUiState.Success -> Dashboard(
                url = state.url,
                result = state.result,
                onDeepAnalysis = onDeepAnalysis
            )
            is AnalysisUiState.Error -> ErrorCard(url = state.url, message = state.message, onRetry = onRetry)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  App Header
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AppHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "ReviewTrust AI",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ReviewTrust AI",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Fake Review Detection",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Dashboard (shown on Success)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun Dashboard(url: String, result: AnalysisResponse, onDeepAnalysis: () -> Unit = {}) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(initialOffsetY = { it / 3 })
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Analysis mode & cache indicator
            AnalysisModeIndicator(result)

            // Section 1 — Trust Score
            TrustScoreCard(result)

            // Section 2 — Purchase Recommendation
            RecommendationCard(result)

            // Section 3 — Review Statistics
            ReviewStatisticsCard(result)

            // Section 3A — Genuine Summary
            if (result.genuineSummary != null) {
                GenuineSummaryCard(result.genuineSummary)
            }

            // Section 3B — Sentiment
            if (result.sentiment != null) {
                SentimentCard(result.sentiment)
            }

            // Section 3C — Spike Detection
            if (result.spikeDetection != null) {
                SpikeDetectionCard(result.spikeDetection)
            }

            // Section 3D — Reviewer Quality
            if (result.reviewerQuality != null) {
                ReviewerQualityCard(result.reviewerQuality)
            }

            // Section 4 — Pie Chart
            PieChartCard(result)

            // Section 5 — Bar Chart
            BarChartCard(result)

            // Section 6 — Fake Review Details
            if (result.fakeReviews.isNotEmpty()) {
                ReviewListCard(
                    title = "Fake Reviews",
                    icon = Icons.Filled.Error,
                    accentColor = Red500,
                    reviews = result.fakeReviews
                )
            }

            // Section 7 — Genuine Review Details
            if (result.genuineReviews.isNotEmpty()) {
                ReviewListCard(
                    title = "Genuine Reviews",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = Green500,
                    reviews = result.genuineReviews
                )
            }

            // Deep Analysis button (only show when current result is normal mode)
            if (result.analysisMode == "normal") {
                DeepAnalysisCard(onDeepAnalysis = onDeepAnalysis)
            }

            // Footer — analysed URL
            UrlFooterCard(url)
        }
    }
}

// ─── Analysis Mode Indicator ─────────────────────────────────────────────────

@Composable
private fun AnalysisModeIndicator(result: AnalysisResponse) {
    val isDeep = result.analysisMode == "deep"
    val bgColor = if (isDeep)
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val textColor = if (isDeep)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isDeep) Icons.Filled.Search else Icons.Filled.Shield,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDeep) "Deep Analysis" else "Normal Analysis",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        if (result.cached) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "• Cached",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Green500
            )
        }
    }
}

// ─── Deep Analysis Card ──────────────────────────────────────────────────────

@Composable
private fun DeepAnalysisCard(onDeepAnalysis: () -> Unit) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Deep Analysis",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Want more detailed results?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Deep analysis scrapes all reviews and generates more explanations for a thorough assessment.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDeepAnalysis,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Run Deep Analysis",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ─── Section 1: Trust Score ──────────────────────────────────────────────────

// ─── Section 2: Recommendation ───────────────────────────────────────────────

@Composable
private fun RecommendationCard(result: AnalysisResponse) {
    val recommendationColor = when {
        result.trustScore >= 80 -> Green500
        result.trustScore >= 60 -> Orange500
        else -> Red500
    }

    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(icon = Icons.Filled.Recommend, title = "Recommendation")

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = result.recommendation,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = recommendationColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Based on review authenticity analysis",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun TrustScoreCard(result: AnalysisResponse) {
    val scoreColor = when {
        result.trustScore >= 70 -> Green500
        result.trustScore >= 40 -> Orange500
        else -> Red500
    }

    // Animate the score from 0 → actual
    var animateTarget by remember { mutableStateOf(false) }
    val animatedScore by animateFloatAsState(
        targetValue = if (animateTarget) result.trustScore.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "trustScoreAnim"
    )
    LaunchedEffect(result.trustScore) { animateTarget = true }

    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(icon = Icons.Filled.Shield, title = "Trust Score")

            Spacer(modifier = Modifier.height(16.dp))

            // Big score number
            Text(
                text = "${animatedScore.toInt()}",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )

            Text(
                text = when {
                    result.trustScore >= 70 -> "Mostly Trustworthy"
                    result.trustScore >= 40 -> "Use Caution"
                    else -> "Likely Unreliable"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Thin progress bar showing score / 100
            LinearProgressIndicator(
                progress = result.trustScore / 100f,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = scoreColor,
                trackColor = scoreColor.copy(alpha = 0.15f)
            )
        }
    }
}

// ─── Section 2: Review Statistics ────────────────────────────────────────────

@Composable
private fun ReviewStatisticsCard(result: AnalysisResponse) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(icon = Icons.Filled.Reviews, title = "Review Statistics")

            Spacer(modifier = Modifier.height(20.dp))

            // Percentages row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Fake", value = "${result.fakePercentage}%", color = Red500)
                StatItem(label = "Genuine", value = "${result.genuinePercentage}%", color = Green500)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Counts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Analysed",
                    value = "${result.totalReviews}",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Fake Reviews",
                    value = "${result.fakeCount}",
                    color = Red500
                )
                StatItem(
                    label = "Genuine",
                    value = "${result.genuineCount}",
                    color = Green500
                )
            }
        }
    }
}

// ─── Section 3: Pie Chart ────────────────────────────────────────────────────

@Composable
private fun PieChartCard(result: AnalysisResponse) {
    val context = LocalContext.current

    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(icon = Icons.Filled.PieChart, title = "Fake vs Genuine")

            Spacer(modifier = Modifier.height(12.dp))

            AndroidView(
                factory = {
                    ChartHelper.createReviewPieChart(
                        context = context,
                        fakePercent = result.fakePercentage,
                        genuinePercent = result.genuinePercentage
                    )
                },
                update = { chart ->
                    // Update data if the result changes
                    val entries = listOf(
                        com.github.mikephil.charting.data.PieEntry(
                            result.genuinePercentage.toFloat(), "Genuine"
                        ),
                        com.github.mikephil.charting.data.PieEntry(
                            result.fakePercentage.toFloat(), "Fake"
                        )
                    )
                    val ds = chart.data.dataSet as com.github.mikephil.charting.data.PieDataSet
                    ds.values = entries
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        }
    }
}

// ─── Section 4: Bar Chart ────────────────────────────────────────────────────

@Composable
private fun BarChartCard(result: AnalysisResponse) {
    val context = LocalContext.current

    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(icon = Icons.Filled.BarChart, title = "Review Breakdown")

            Spacer(modifier = Modifier.height(12.dp))

            AndroidView(
                factory = {
                    ChartHelper.createReviewBarChart(
                        context = context,
                        totalReviews = result.totalReviews,
                        genuineReviews = result.genuineCount,
                        fakeReviews = result.fakeCount
                    )
                },
                update = { chart ->
                    val entries = listOf(
                        com.github.mikephil.charting.data.BarEntry(0f, result.fakeCount.toFloat()),
                        com.github.mikephil.charting.data.BarEntry(1f, result.genuineCount.toFloat()),
                        com.github.mikephil.charting.data.BarEntry(2f, result.totalReviews.toFloat())
                    )
                    val ds = chart.data.dataSets[0] as com.github.mikephil.charting.data.BarDataSet
                    ds.values = entries
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

// ─── Section 6 & 7: Expandable Review Lists ─────────────────────────────────

@Composable
private fun ReviewListCard(
    title: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    reviews: List<ReviewDetail>
) {
    var expanded by remember { mutableStateOf(false) }

    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Header — tap to expand / collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$title (${reviews.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded content
            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                reviews.forEachIndexed { index, review ->
                    ReviewItem(review = review, accentColor = accentColor)
                    if (index < reviews.lastIndex) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: ReviewDetail,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Review text
        Text(
            text = "\u201C${review.text}\u201D",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )

        // Explanations
        if (review.explanation.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            review.explanation.forEach { reason ->
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "\u2022 ",
                        fontSize = 12.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reason,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ─── URL Footer ──────────────────────────────────────────────────────────────

@Composable
private fun UrlFooterCard(url: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = url,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Reusable helpers
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun DashboardCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        content = { content() }
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Loading Card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingCard(url: String) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Analyzing reviews…",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = url,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Error Card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ErrorCard(url: String, message: String, onRetry: () -> Unit) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = Red500,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Analysis Failed",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Red500
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = url,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Shared URL Card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SharedUrlCard(url: String) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Green500,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Product URL received",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Green500
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = url,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Default Instructions Card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun DefaultInstructionsCard() {
    DashboardCard {
        Text(
            text = "Share a product link from Amazon or Flipkart to analyze reviews.",
            modifier = Modifier.padding(24.dp),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Previews
// ═════════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, name = "Idle")
@Composable
fun PreviewIdle() {
    ReviewTrustTheme { HomeContent(state = AnalysisUiState.Idle) }
}

@Preview(showBackground = true, showSystemUi = true, name = "Loading")
@Composable
fun PreviewLoading() {
    ReviewTrustTheme {
        HomeContent(state = AnalysisUiState.Loading("https://amazon.in/product/xyz"))
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Dashboard")
@Composable
fun PreviewDashboard() {
    ReviewTrustTheme {
        HomeContent(
            state = AnalysisUiState.Success(
                url = "https://amazon.in/product/xyz",
                result = AnalysisResponse(
                    trustScore = 73,
                    fakePercentage = 27,
                    genuinePercentage = 73,
                    totalReviews = 147,
                    fakeCount = 40,
                    genuineCount = 107,
                    recommendation = "Mixed review authenticity detected. Proceed with caution.",
                    fakeReviews = listOf(
                        ReviewDetail("Amazing product!!! Must buy!!!", listOf("Excessive promotional language", "Very short review length")),
                        ReviewDetail("Best product ever, absolutely perfect in every way!", listOf("Pattern resembles computer-generated text"))
                    ),
                    genuineReviews = listOf(
                        ReviewDetail("Battery life is good but charging is slow. Camera quality is decent for the price.", listOf("Balanced sentiment with pros and cons", "Detailed and lengthy review")),
                        ReviewDetail("Display is bright, fingerprint sensor works well. Build quality feels average.", listOf("Critical feedback with specifics"))
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Error")
@Composable
fun PreviewError() {
    ReviewTrustTheme {
        HomeContent(
            state = AnalysisUiState.Error(
                url = "https://amazon.in/product/xyz",
                message = "Unable to connect to server. Check your internet connection."
            )
        )
    }
}

// ─── Section 3A: Genuine Summary ──────────────────────────────────────────

@Composable
private fun GenuineSummaryCard(summaryResult: GenuineSummaryResult) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            SectionHeader(icon = Icons.Filled.FormatQuote, title = "What real buyers say")
            Spacer(modifier = Modifier.height(16.dp))

            if (summaryResult.summary != null) {
                Text(
                    text = "\"${summaryResult.summary}\"",
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            } else {
                Text(
                    text = summaryResult.reason ?: "Insufficient genuine reviews to summarize",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Section 3B: Sentiment ────────────────────────────────────────────────

@Composable
private fun SentimentCard(sentiment: SentimentResult) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            SectionHeader(icon = Icons.Filled.Reviews, title = "Genuine Review Sentiment")
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sentiment.positivePct > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(sentiment.positivePct.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Green500),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sentiment.positivePct > 10f) Text("${sentiment.positivePct.toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (sentiment.neutralPct > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(sentiment.neutralPct.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sentiment.neutralPct > 10f) Text("${sentiment.neutralPct.toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (sentiment.negativePct > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(sentiment.negativePct.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Red500),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sentiment.negativePct > 10f) Text("${sentiment.negativePct.toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Positive", color = Green500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Neutral", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Negative", color = Red500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Section 3C: Spike Detection ──────────────────────────────────────────

@Composable
private fun SpikeDetectionCard(spikeDetection: SpikeDetectionResult) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(icon = Icons.Filled.DateRange, title = "Review Timeline")
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = if (spikeDetection.granularity == "day") "Daily" else "Monthly", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (spikeDetection.spikeDetected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Orange500.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = Orange500, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review spike detected: " + spikeDetection.spikePeriods.joinToString(", "),
                        fontSize = 13.sp,
                        color = Orange500,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val timeline = spikeDetection.timeline
            if (timeline.isNotEmpty()) {
                val maxCount = timeline.maxOf { it.count }.coerceAtLeast(1)
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)) {
                    val barSpacing = 8.dp.toPx()
                    val availableWidth = size.width
                    val totalBars = timeline.size
                    // Compute bar width, ensuring min 2px and max 24px
                    val barWidth = ((availableWidth - (totalBars - 1) * barSpacing) / totalBars).coerceIn(2f, 24.dp.toPx())
                    val canvasHeight = size.height

                    timeline.forEachIndexed { i, period ->
                        val isSpike = spikeDetection.spikePeriods.contains(period.period)
                        val barHeight = (period.count.toFloat() / maxCount) * canvasHeight
                        val startX = i * (barWidth + barSpacing)
                        
                        // Center bars if there's excessive width
                        val totalDrawWidth = (totalBars * barWidth) + ((totalBars - 1) * barSpacing)
                        val offsetX = (availableWidth - totalDrawWidth) / 2f
                        
                        drawRect(
                            color = if (isSpike) Red500 else Green500,
                            topLeft = Offset(offsetX + startX, canvasHeight - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            }

            if (spikeDetection.dataLimitation != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = spikeDetection.dataLimitation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── Section 3D: Reviewer Quality ──────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewerQualityCard(qualityResult: ReviewerQualityResult) {
    DashboardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            SectionHeader(icon = Icons.Filled.Group, title = "Reviewer Credibility")
            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QualitasChip("${qualityResult.unverifiedPct}% unverified", if (qualityResult.unverifiedPct > 20f) Red500 else Green500)
                QualitasChip("${qualityResult.suspiciousReviewerPct}% suspicious accounts", if (qualityResult.suspiciousReviewerPct > 15f) Red500 else Green500)
                
                if (qualityResult.platform == "amazon") {
                    if (qualityResult.thinAccountCount > 0) QualitasChip("${qualityResult.thinAccountCount} thin accounts", Red500)
                    if (qualityResult.massReviewerCount > 0) QualitasChip("${qualityResult.massReviewerCount} mass reviewers", Red500)
                }
            }

            if (qualityResult.dataLimitation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = qualityResult.dataLimitation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QualitasChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
