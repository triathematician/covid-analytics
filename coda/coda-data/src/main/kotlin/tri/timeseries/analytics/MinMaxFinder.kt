/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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

import tri.covid19.reports.percentChangeTo
import tri.timeseries.TimeSeries
import java.time.LocalDate
import kotlin.math.abs

/**
 * Finds meaningful local minima and maxima. Uses a parameter that requires values found to be at least N away from other values.
 * The algorithm first finds all local minima and maxima (that are also minima/maxima within the [x-N,x+N] sample window),
 * and then fills in values between adjacent minima/maxima (e.g. if there are two local minima in a row) with the max/min value in between.
 */
class MinMaxFinder(var sampleWindow: Int = 7) {

    fun invoke(series: TimeSeries): ExtremaSummary {
        val values = series.values.convolve()
        val largest = values.maxOrNull()
        val smallest = values.minOrNull()
        if (smallest == largest) {
            return ExtremaSummary(series).apply {
                extrema[series.start] = ExtremeInfo(series.metric, series.start, series[series.start], ExtremeType.ENDPOINT, null)
                extrema[series.end] = ExtremeInfo(series.metric, series.end, series[series.end], ExtremeType.ENDPOINT, null)
            }
        }

        val minima = findMins(values, sampleWindow)
        val maxima = findMaxs(values, sampleWindow)

        val extremes = (minima.map { it to ExtremeType.LOCAL_MIN } + maxima.map { it to ExtremeType.LOCAL_MAX })
        val endpoints = listOf(0 to ExtremeType.ENDPOINT, series.size - 1 to ExtremeType.ENDPOINT)
        val intermediates = extremes.windowed(2)
                .filter { it[0].second == it[1].second && it[1].first - it[0].first > sampleWindow }
                .map { betweenExtremes(series.values, it[0], it[1]) }
        val combined = (endpoints + extremes + intermediates).toMap().toSortedMap().toList()

        return ExtremaSummary(series).apply {
            combined.mapIndexed { i, pair ->
                val previous = if (i == 0) null else series.values.getOrNull(combined[i - 1].first)
                val current = series.values.getOrNull(pair.first) ?: 0.0
                val type = if (current == largest) ExtremeType.GLOBAL_MAX else if (current == smallest) ExtremeType.GLOBAL_MIN else pair.second
                ExtremeInfo(series.metric, series.date(pair.first), current, type, previous?.percentChangeTo(current))
            }.onEach { extrema[it.date] = it }
        }
    }

    private fun betweenExtremes(series: List<Double>, p1: Pair<Int, ExtremeType>, p2: Pair<Int, ExtremeType>): Pair<Int, ExtremeType> {
        require(p1.second == p2.second)
        return when (p1.second) {
            ExtremeType.LOCAL_MIN -> series.argmax(p1.first + 1, p2.first - 1)!! to ExtremeType.LOCAL_MAX
            ExtremeType.LOCAL_MAX -> series.argmin(p1.first + 1, p2.first - 1)!! to ExtremeType.LOCAL_MIN
            else -> throw IllegalArgumentException()
        }
    }

    /** Find indices of values that are <= values in [x-win, x+win]. */
    fun findMins(series: List<Double>, win: Int) = series.indices.filter { t -> series.window(t - win, t + win).all { it >= series[t] } }
    /** Find indices of values that are >= values in [x-win, x+win]. */
    fun findMaxs(series: List<Double>, win: Int) = series.indices.filter { t -> series.window(t - win, t + win).all { it <= series[t] } }

    private fun <X> List<X>.window(min: Int, max: Int) = subList(maxOf(min, 0), minOf(max + 1, size))
    private fun <X : Comparable<X>> List<X>.argmin(min: Int, max: Int) = (min..max).minByOrNull { it: Int -> get(it) }
    private fun <X : Comparable<X>> List<X>.argmax(min: Int, max: Int) = (min..max).maxByOrNull { it: Int -> get(it) }
    private fun List<Double>.convolve() = indices.map { i ->
        (-10..10).sumByDouble { getOrElse(i + it) { 0.0 } * convolveFun(it) }
    }

    private fun convolveFun(i: Int) = when (i) {
        0 -> 1.0
        else -> maxOf(0.0, .01 - .001 * abs(i))
    }

}

/** Summarizes information about extrema by date. */
class ExtremaSummary(val series: TimeSeries) {
    val extrema = sortedMapOf<LocalDate, ExtremeInfo>()

    override fun toString() = extrema.toString()
}

/** Types of extrema. */
enum class ExtremeType { GLOBAL_MAX, GLOBAL_MIN, LOCAL_MAX, LOCAL_MIN, ENDPOINT }

/** Information associated with a single extremum. */
data class ExtremeInfo(var metric: String, var date: LocalDate, var value: Double, var type: ExtremeType, var percentChange: Double?)
