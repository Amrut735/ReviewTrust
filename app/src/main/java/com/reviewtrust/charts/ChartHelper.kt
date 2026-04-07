package com.reviewtrust.charts

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter

/**
 * Helper object for creating MPAndroidChart chart instances
 * that can be embedded inside Jetpack Compose via AndroidView.
 */
object ChartHelper {

    // ── Colours ──────────────────────────────────────────────────────────────
    private val GREEN = Color.parseColor("#4CAF50")
    private val RED = Color.parseColor("#F44336")
    private val BLUE = Color.parseColor("#1565C0")
    private val LABEL_COLOR = Color.parseColor("#616161")

    // ── Pie Chart ────────────────────────────────────────────────────────────

    /**
     * Creates a PieChart showing Fake vs Genuine review percentage.
     */
    fun createReviewPieChart(
        context: Context,
        fakePercent: Int,
        genuinePercent: Int
    ): PieChart {
        val chart = PieChart(context)

        val entries = listOf(
            PieEntry(genuinePercent.toFloat(), "Genuine"),
            PieEntry(fakePercent.toFloat(), "Fake")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(GREEN, RED)
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueTypeface = Typeface.DEFAULT_BOLD
            valueFormatter = PercentFormatter(chart)
            sliceSpace = 2f
        }

        chart.apply {
            data = PieData(dataSet)
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 45f
            transparentCircleRadius = 50f
            setHoleColor(Color.TRANSPARENT)
            setDrawCenterText(true)
            centerText = "Reviews"
            setCenterTextSize(13f)
            setCenterTextColor(LABEL_COLOR)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.WHITE)
            legend.apply {
                isEnabled = true
                textSize = 12f
                textColor = LABEL_COLOR
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 8f
            }
            setExtraOffsets(8f, 8f, 8f, 12f)
            animateY(900)
        }

        return chart
    }

    // ── Bar Chart ────────────────────────────────────────────────────────────

    /**
     * Creates a HorizontalBarChart showing review statistics
     * (Total, Genuine, Fake review counts side-by-side).
     */
    fun createReviewBarChart(
        context: Context,
        totalReviews: Int,
        genuineReviews: Int,
        fakeReviews: Int
    ): HorizontalBarChart {
        val chart = HorizontalBarChart(context)

        val entries = listOf(
            BarEntry(0f, fakeReviews.toFloat()),
            BarEntry(1f, genuineReviews.toFloat()),
            BarEntry(2f, totalReviews.toFloat())
        )

        val dataSet = BarDataSet(entries, "Review Count").apply {
            colors = listOf(RED, GREEN, BLUE)
            valueTextSize = 12f
            valueTextColor = LABEL_COLOR
            valueTypeface = Typeface.DEFAULT_BOLD
        }

        val labels = listOf("Fake", "Genuine", "Total")

        chart.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.6f
            }
            description.isEnabled = false
            setFitBars(true)
            setDrawValueAboveBar(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 12f
                textColor = LABEL_COLOR
                valueFormatter = IndexAxisValueFormatter(labels)
            }

            axisLeft.apply {
                axisMinimum = 0f
                textColor = LABEL_COLOR
                textSize = 11f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
            }

            axisRight.isEnabled = false

            legend.apply {
                isEnabled = false
            }

            setExtraOffsets(4f, 8f, 16f, 8f)
            animateXY(700, 700)
        }

        return chart
    }
}
