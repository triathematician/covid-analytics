package tri.timeseries

import tri.covid19.data.CovidHistory
import java.time.LocalDate
import kotlin.math.abs
import kotlin.time.ExperimentalTime

/**
 * Finds meaningful local minima and maxima. Uses a parameter that requires values found to be at least N away from other values.
 * The algorithm first finds all local minima and maxima (that are also minima/maxima within the [x-N,x+N] sample window),
 * and then fills in values between adjacent minima/maxima (e.g. if there are two local minima in a row) with the max/min value in between.
 */
class MinMaxFinder(var sampleWindow: Int = 7) {

    fun invoke(series: MetricTimeSeries): ExtremaSummary {
        val values = series.values.convolve()
        val minima = findMins(values, sampleWindow)
        val maxima = findMaxs(values, sampleWindow)
        val extremes = (minima.map { it to ExtremaType.LOCAL_MIN } + maxima.map { it to ExtremaType.LOCAL_MAX })
        val endpoints = listOf(0 to ExtremaType.ENDPOINT, series.size - 1 to ExtremaType.ENDPOINT)
        val intermediates = extremes.windowed(2)
                .filter { it[0].second == it[1].second && it[1].first - it[0].first > sampleWindow }
                .map { betweenExtremes(series.values, it[0], it[1]) }

        return ExtremaSummary(series).apply {
            (endpoints + extremes + intermediates).map {
                ExtremaInfo(series.date(it.first), series.values.getOrNull(it.first) ?: 0.0, it.second)
            }.onEach { extrema.put(it.date, it) }
        }
    }

    private fun betweenExtremes(series: List<Double>, p1: Pair<Int, ExtremaType>, p2: Pair<Int, ExtremaType>): Pair<Int, ExtremaType> {
        require(p1.second == p2.second)
        return when (p1.second) {
            ExtremaType.LOCAL_MIN -> series.argmax(p1.first + 1, p2.first - 1)!! to ExtremaType.LOCAL_MAX
            ExtremaType.LOCAL_MAX -> series.argmin(p1.first + 1, p2.first - 1)!! to ExtremaType.LOCAL_MIN
            else -> throw IllegalArgumentException()
        }
    }

    /** Find indices of values that are <= values in [x-win, x+win]. */
    fun findMins(series: List<Double>, win: Int) = series.indices.filter { t -> series.window(t-win, t+win).all { it >= series[t] } }
    /** Find indices of values that are >= values in [x-win, x+win]. */
    fun findMaxs(series: List<Double>, win: Int) = series.indices.filter { t -> series.window(t-win, t+win).all { it <= series[t] } }

    private fun <X> List<X>.window(min: Int, max: Int) = subList(maxOf(min, 0), minOf(max + 1, size))
    private fun <X: Comparable<X>> List<X>.argmin(min: Int, max: Int) = (min..max).minBy { get(it) }
    private fun <X: Comparable<X>> List<X>.argmax(min: Int, max: Int) = (min..max).maxBy { get(it) }
    private fun List<Double>.convolve() = indices.map { i ->
        (-10..10).sumByDouble { getOrElse(i + it) { 0.0 } * convolveFun(it) }
    }
    private fun convolveFun(i: Int) = when (i) {
        0 -> 1.0
        else -> maxOf(0.0, .01 - .001 * abs(i))
    }

}

/** Summarizes information about extrema by date. */
class ExtremaSummary(val series: MetricTimeSeries) {
    val extrema = sortedMapOf<LocalDate, ExtremaInfo>()

    override fun toString() = extrema.toString()
}

/** Types of extrema. */
enum class ExtremaType { LOCAL_MAX, LOCAL_MIN, ENDPOINT }

/** Information associated with a single extremum. */
data class ExtremaInfo(var date: LocalDate, var value: Double, var type: ExtremaType)

@ExperimentalTime
fun main() {
    CovidHistory.allData.filter { it.region.id.endsWith(", US") && it.region.type == RegionType.PROVINCE_STATE }
            .onEach {
                val series = it.deltas().restrictNumberOfStartingZerosTo(1).movingAverage(7)
                println("${it.region.id} - ${it.metric} - ${series.values.map { it.toInt() }}")
                println("  " + MinMaxFinder(10).invoke(series))
            }
}