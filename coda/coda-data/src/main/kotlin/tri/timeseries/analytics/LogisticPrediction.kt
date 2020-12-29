/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
