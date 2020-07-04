package tri.timeseries.analytics

import org.apache.commons.math3.stat.regression.SimpleRegression
import tri.timeseries.growthPercentages
import tri.timeseries.slidingWindow
import kotlin.math.ln

/** Fit logistic curve using Hubbert linearization, and use the result to predict final total and peaks. */
fun List<Double>.computeLogisticPrediction(days: Int): List<LogisticPrediction> {
    return slidingWindow(days)
            .map { it.last() to linearRegression(it.drop(1), it.growthPercentages()) }
            .map { LogisticPrediction(it.first, it.second) }
}

fun linearRegression(x: List<Double>, y: List<Double>) = SimpleRegression().apply {
    x.indices.intersect(y.indices).forEach { addData(x[it], y[it]) }
}

class LogisticPrediction(startVal: Double, regression: SimpleRegression) {
    val intercept = regression.intercept
    val slope = if (regression.slope > 0) Double.NaN else regression.slope
    val kTotal = if (regression.slope > 0) Double.NaN else -intercept/slope
    val peakGrowth = if (regression.slope > 0) Double.NaN else .25*kTotal*intercept
    val daysToPeak = if (regression.slope > 0) Double.NaN else ln((kTotal-startVal)/startVal)/intercept

    val slopeConfidenceInterval = if (slope > 0) Double.NaN else regression.slopeConfidenceInterval
    val minSlope = slope - slopeConfidenceInterval
    val maxSlope = slope + slopeConfidenceInterval
    val minKTotal = if (regression.slope > 0) Double.NaN else maxOf(0.0, minOf(-intercept/minSlope, -intercept/maxSlope))
    val maxKTotal = if (regression.slope > 0) Double.NaN else if (maxOf(minSlope, maxSlope) > 0) Double.POSITIVE_INFINITY else maxOf(-intercept/minSlope, -intercept/maxSlope)

    val hasBoundedConfidence = slope < 0 && minSlope < 0 && maxSlope < 0
}