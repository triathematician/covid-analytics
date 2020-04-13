package triathematician.timeseries

import triathematician.util.rangeTo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Time series of a single metric.
 * Stores values as doubles, but will report them as [Int]s if a flag is set.
 */
data class MetricTimeSeries(var id: String = "", var metric: String = "", var intSeries: Boolean, val defValue: Double = 0.0, var start: LocalDate = LocalDate.now(), val values: List<Double> = listOf()) {

    constructor(id: String, metric: String, defValue: Double = 0.0, start: LocalDate, value: Double) : this(id, metric, false, defValue, start, listOf(value))
    constructor(id: String, metric: String, defValue: Int = 0, start: LocalDate, values: List<Int>) : this(id, metric, false, defValue.toDouble(), start, values.map { it.toDouble() })
    constructor(id: String, metric: String, defValue: Int = 0, start: LocalDate, value: Int) : this(id, metric, true, defValue.toDouble(), start, listOf(value.toDouble()))

    val size: Int
        get() = values.size
    val lastValue: Double
        get() = values.lastOrNull() ?: 0.0
    val end: LocalDate
        get() = start.plusDays((values.size - 1).toLong())
    val valuesAsMap: Map<LocalDate, Double>
        get() = values.mapIndexed { i, d -> start.plusDays(i.toLong()) to d }.toMap()

    operator fun get(date: LocalDate): Double = values.getOrElse(indexOf(date)) { defValue }

    private fun indexOf(date: LocalDate) = ChronoUnit.DAYS.between(start, date).toInt()

    //region DERIVED SERIES

    fun copyAdjustingStartDay(metric: String = this.metric, values: List<Double> = this.values, intSeries: Boolean = this.intSeries)
            = copy(metric = metric, start = start.plusDays((this.values.size - values.size).toLong()), values = values, intSeries = intSeries)

    operator fun plus(n: Number): MetricTimeSeries = copy(values = values.map { it + n.toDouble() })
    operator fun minus(n: Number): MetricTimeSeries = copy(values = values.map { it - n.toDouble() })
    operator fun times(n: Number): MetricTimeSeries = copy(values = values.map { it * n.toDouble() })
    operator fun div(n: Number): MetricTimeSeries = copy(values = values.map { it / n.toDouble() })

    /** Return copy with moving averages. */
    fun movingAverage(bucket: Int) = copyAdjustingStartDay(values = values.movingAverage(bucket))

    /** Return copy with growth percentages. */
    fun growthPercentages(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(metric = metricFunction(metric), values = values.growthPercentages(), intSeries = false)
            .restrictToRealNumbers()

    /** Return derived metrics with logistic predictions, using given number of days for linear regression. */
    fun logisticPredictions(days: Int): List<MetricTimeSeries> = with(values.computeLogisticPrediction(days)) {
        listOf(copyAdjustingStartDay(metric = "$metric (predicted total)", values = map { it.kTotal }),
                copyAdjustingStartDay(metric = "$metric (predicted total, min)", values = map { it.minKTotal }, intSeries = false),
                copyAdjustingStartDay(metric = "$metric (predicted total, max)", values = map { it.maxKTotal }, intSeries = false),
                copyAdjustingStartDay(metric = "$metric (predicted peak)", values = map { it.peakGrowth }),
                copyAdjustingStartDay(metric = "$metric (days to peak)", values = map { it.daysToPeak }, intSeries = false),
                copyAdjustingStartDay(metric = "$metric (logistic slope)", values = map { it.slope }, intSeries = false),
                copyAdjustingStartDay(metric = "$metric (logistic intercept)", values = map { it.intercept }, intSeries = false))
                .map { it.restrictToRealNumbers() }
    }

    /** Copy after dropping first n values. */
    fun dropFirst(n: Int): MetricTimeSeries {
        val res = copyAdjustingStartDay(values = values.drop(n))
        return res
    }

    //endregion

    //region CLEANUP UTILS

    /** Return copy of this series where values are forced to be increasing. */
    fun coerceIncreasing(): MetricTimeSeries {
        val res = values.toMutableList()
        for (i in 1 until res.size) {
            res[i] = maxOf(res[i - 1], res[i])
        }
        return copy(values = res)
    }

    /** Return copy of this series where values are real numbers. */
    fun restrictToRealNumbers(): MetricTimeSeries {
        val firstRealNumber = values.indexOfFirst { it.isFinite() }
        return when {
            firstRealNumber > 0 -> dropFirst(firstRealNumber)
            else -> this
        }
    }

    /** If time series starts with more than max zeros, trim so that it has max zeros. */
    fun restrictNumberOfStartingZerosTo(max: Int): MetricTimeSeries {
        val firstNonZeroIndex = values.indexOfFirst { it != 0.0 }
        return when {
            firstNonZeroIndex > max -> dropFirst(firstNonZeroIndex - max)
            else -> this
        }
    }

    //endregion
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