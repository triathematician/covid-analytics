package triathematician.timeseries

import triathematician.util.rangeTo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Time series of a single metric.
 * Stores values as doubles, but will report them as [Int]s if a flag is set.
 */
data class MetricTimeSeries(var id: String = "", var metric: String = "", var intSeries: Boolean, val defValue: Double = 0.0, var start: LocalDate = LocalDate.now(), val values: List<Double> = listOf()) {

    constructor(id: String, metric: String, defValue: Double = 0.0, start: LocalDate, value: Double): this(id, metric, false, defValue, start, listOf(value))
    constructor(id: String, metric: String, defValue: Int = 0, start: LocalDate, values: List<Int>): this(id, metric, false, defValue.toDouble(), start, values.map { it.toDouble() })
    constructor(id: String, metric: String, defValue: Int = 0, start: LocalDate, value: Int): this(id, metric, true, defValue.toDouble(), start, listOf(value.toDouble()))

    val size: Int
        get() = values.size
    val lastValue: Double
        get() = values.lastOrNull() ?: 0.0
    val end: LocalDate
        get() = start.plusDays((values.size - 1).toLong())
    val valuesAsMap: Map<LocalDate, Double>
        get() = values.mapIndexed { i, d -> start.plusDays(i.toLong()) to d }.toMap()

    operator fun get(date: LocalDate): Double = values.getOrElse(indexOf(date)) { defValue }

    operator fun plus(n: Number): MetricTimeSeries = copy(values = values.map { it + n.toDouble() })
    operator fun minus(n: Number): MetricTimeSeries = copy(values = values.map { it - n.toDouble() })
    operator fun times(n: Number): MetricTimeSeries = copy(values = values.map { it * n.toDouble() })
    operator fun div(n: Number): MetricTimeSeries = copy(values = values.map { it / n.toDouble() })

    private fun indexOf(date: LocalDate) = ChronoUnit.DAYS.between(start, date).toInt()

    /** Return copy of this series where values are foerced to be increasing. */
    fun coerceIncreasing(): MetricTimeSeries {
        val res = values.toMutableList()
        for (i in 1 until res.size) {
            res[i] = maxOf(res[i-1], res[i])
        }
        return copy(values = res)
    }

    /** If time series starts with more than max zeros, trim so that it has max zeros. */
    fun restrictNumberOfStartingZerosTo(max: Int): MetricTimeSeries {
        val firstNonZeroIndex = values.indexOfFirst { it > 0 }
        return when {
            firstNonZeroIndex > max -> copy(start = start.plusDays((firstNonZeroIndex - max - 1).toLong()), values = values.drop(firstNonZeroIndex - max - 1))
            else -> this
        }
    }
}

//region factories

fun intTimeSeries(id: String, metric: String, start: LocalDate, values: List<Int>) = MetricTimeSeries(id, metric, 0, start, values)
fun intTimeSeries(id: String, metric: String, date: LocalDate, value: Int) = MetricTimeSeries(id, metric, 0, date, value)

//endregion

/** Merge a bunch of separate time series into a single time series object. */
private fun List<MetricTimeSeries>.merge() = reduce { s1, s2 ->
    require(s1.id == s2.id)
    require(s1.metric == s2.metric)
    val minDate = minOf(s1.start, s2.start)
    val maxDate = maxOf(s1.end, s2.end)
    val series = (minDate..maxDate).map { maxOf(s1[it], s2[it]) }
    s1.copy(start = minDate, values = series)
}

/** Merge a bunch of time series by id and metric. */
fun List<MetricTimeSeries>.regroupAndMerge() = groupBy { listOf(it.id, it.metric) }
        .map { it.value.merge() }
        .map { it.coerceIncreasing() }
        .map { it.restrictNumberOfStartingZerosTo(5) }