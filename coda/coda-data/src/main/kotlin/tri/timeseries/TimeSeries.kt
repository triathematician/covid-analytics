package tri.timeseries

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.area.Lookup
import tri.timeseries.analytics.computeLogisticPrediction
import tri.util.DateRange
import tri.util.minus
import tri.util.rangeTo
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Time series of a single metric. Stores values as doubles, but will report them as [Int]s if a flag is set.
 */
data class TimeSeries(
        /** Source for the time series. */
        var source: String,
        /** ID of the area for this time series. */
        var areaId: String,
        /** Metric reported in this time series. */
        var metric: String,
        /** Qualifier for this time series, e.g. for breakdowns by age/demographics. */
        var qualifier: String = "",
        /** If true, values are reported/serialized as integers. */
        var intSeries: Boolean = false,
        /** Default value, when none is stored for a given date. */
        val defValue: Double = 0.0,
        /** First date with data for this series. */
        var start: LocalDate = LocalDate.now(),
        /** List of values for this series. */
        val values: List<Double> = listOf()) {

    /** Construct with explicit floating-point values. */
    constructor(source: String, areaId: String, metric: String, qualifier: String = "", defValue: Double = 0.0, start: LocalDate, vararg values: Double)
            : this(source, areaId, metric, qualifier, false, defValue, start, values.toList())

    /** Construct with a set of integer values. */
    constructor(source: String, areaId: String, metric: String, qualifier: String = "", defValue: Int = 0, start: LocalDate, values: List<Int>)
            : this(source, areaId, metric, qualifier, true, defValue.toDouble(), start, values.map { it.toDouble() })

    /** Construct with a set of integer values. */
    constructor(source: String, areaId: String, metric: String, qualifier: String = "", defValue: Int = 0, start: LocalDate, vararg values: Int)
            : this(source, areaId, metric, qualifier, true, defValue.toDouble(), start, values.map { it.toDouble() })

    val uniqueMetricKey = listOf(source, areaId, metric, qualifier).joinToString("::")

    //region GETTER HELPERS

    val area
        get() = Lookup.areaOrNull(areaId) ?: throw IllegalStateException("Area not found: $areaId")

    @get:JsonIgnore
    val size: Int
        get() = values.size
    @get:JsonIgnore
    val lastValue: Double
        get() = values.lastOrNull() ?: 0.0

    @get:JsonIgnore
    val firstPositiveDate: LocalDate
        get() = (start..end).firstOrNull { get(it) > 0.0 } ?: end
    @get:JsonIgnore
    val end: LocalDate
        get() = date(values.size - 1)
    @get:JsonIgnore
    val domain: DateRange
        get() = DateRange(firstPositiveDate, end)

    val valuesAsMap: Map<LocalDate, Double>
        get() = values.mapIndexed { i, d -> date(i) to d }.toMap()

    //endregion

    //region VALUE AND DATE GETTERS

    /** Get value on given date. */
    operator fun get(date: LocalDate): Double = values.getOrElse(indexOf(date)) { defValue }
    /** Get value on given date, or null if argument is outside range. */
    fun getOrNull(date: LocalDate): Double? = values.getOrNull(indexOf(date))

    /** Get date by index. */
    fun date(i: Int) = start.plusDays(i.toLong())
    /** Get index by date. */
    private fun indexOf(date: LocalDate) = ChronoUnit.DAYS.between(start, date).toInt()

    //endregion

    //region QUERIES

    /** Get value by days from end. Zero returns the last value. */
    fun valueByDaysFromEnd(value: Int): Double = values.getOrElse(values.size - 1 - value) { defValue }

    /** Get sublist with given indices relative to end of list. Zero is the last element. */
    fun last(range: IntRange): List<Double> {
        return values.subList(maxOf(0, values.size - range.last - 1), maxOf(0, values.size - range.first))
    }

    /** Get date of peak and value. */
    fun peak(since: LocalDate? = null): Pair<LocalDate, Double> = domain.map { it to get(it) }
            .filter { since == null || !it.first.isBefore(since) }
            .maxBy { it.second }!!

    /** Get values for the given range of dates. */
    fun values(dates: DateRange) = dates.map { get(it) }
    /** Compute sum over all dates in given range. */
    fun sum(dates: DateRange) = values(dates).sum()
    /** Compute average over all dates in given range. */
    fun average(dates: DateRange) = values(dates).average()

    /** Compute number of days since value was half of its current value. Returns null if current value is not positive. */
    fun daysSinceHalfCurrentValue(): Int? {
        val cur = lastValue
        if (cur <= 0) return null
        (1..values.size).forEach { if (valueByDaysFromEnd(it) <= .5*cur) return it }
        return null
    }

    //endregion

    //region DERIVED SERIES

    /** Copy where data is restricted to start no early than given date. */
    fun copyWithDataSince(firstDate: LocalDate)
            = copy(metric = metric, start = maxOf(start, firstDate), values = values.drop(maxOf(start, firstDate).minus(start).toInt()), intSeries = intSeries)

    /** Create a copy while adjusting the start day forward/back if the number of values is more/less than current number of values. */
    fun copyAdjustingStartDay(metric: String = this.metric, values: List<Double> = this.values, intSeries: Boolean = this.intSeries)
            = copy(metric = metric, start = date(this.values.size - values.size), values = values, intSeries = intSeries)

    /** Copy after dropping first n values. */
    fun dropFirst(n: Int): TimeSeries {
        val res = copyAdjustingStartDay(values = values.drop(n))
        return res
    }

    /** Apply arbitrary operator to series. */
    fun transform(op: (Double) -> Double) = copy(values = values.map { op(it) })

    operator fun plus(n: Number) = copy(values = values.map { it + n.toDouble() })
    operator fun minus(n: Number) = copy(values = values.map { it - n.toDouble() })
    operator fun times(n: Number) = copy(values = values.map { it * n.toDouble() })
    operator fun div(n: Number) = copy(values = values.map { it / n.toDouble() })

    operator fun plus(n: TimeSeries) = reduceSeries(this, n) { a, b -> a + b }
    operator fun minus(n: TimeSeries) = reduceSeries(this, n) { a, b -> a - b }
    operator fun times(n: TimeSeries) = reduceSeries(this, n) { a, b -> a * b }
    operator fun div(n: TimeSeries) = reduceSeries(this, n) { a, b -> a / b }

    /** Return copy with moving averages. */
    fun movingAverage(bucket: Int, nonZero: Boolean = false, includePartialList: Boolean = true) = copyAdjustingStartDay(values = values.movingAverage(bucket, nonZero, includePartialList))
    /** Return copy with moving sum. */
    fun movingSum(bucket: Int, includePartialList: Boolean = true) = copyAdjustingStartDay(values = values.movingSum(bucket, includePartialList))

    /** Return copy with deltas. */
    fun deltas(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(metric = metricFunction(metric), values = values.deltas())

    /** Compute cumulative totals. */
    fun cumulative(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(metric = metricFunction(metric), values = values.partialSums())
    /** Compute cumulative totals starting on the given day. */
    fun cumulativeSince(cumulativeStart: LocalDate, metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(metric = metricFunction(metric),
            values = values(DateRange(cumulativeStart..end)).partialSums())

    /** Return copy with growth percentages. */
    fun growthPercentages(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(metric = metricFunction(metric), values = values.growthPercentages(), intSeries = false)
            .restrictToRealNumbers()
    /** Return copy with doubling times. */
    fun doublingTimes(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(metric = metricFunction(metric), values = values.doublingTimes(), intSeries = false)
            .restrictToRealNumbers()
    /** Return derived metrics with logistic predictions, using given number of days for linear regression. */
    fun shortTermLogisticForecast(days: Int): List<TimeSeries> {
        val predictions = values.computeLogisticPrediction(days).filter { it.hasBoundedConfidence }
        return listOf(copyAdjustingStartDay(metric = "$metric (predicted total)", values = predictions.map { it.kTotal }),
                copyAdjustingStartDay(metric = "$metric (predicted total, min)", values = predictions.map { it.minKTotal }),
                copyAdjustingStartDay(metric = "$metric (predicted total, max)", values = predictions.map { it.maxKTotal }),
                copyAdjustingStartDay(metric = "$metric (predicted peak)", values = predictions.map { it.peakGrowth }),
                copyAdjustingStartDay(metric = "$metric (days to peak)", values = predictions.map { it.daysToPeak }, intSeries = false)
//                copyAdjustingStartDay(metric = "$metric (logistic slope)", values = predictions.map { it.slope }, intSeries = false),
//                copyAdjustingStartDay(metric = "$metric (logistic intercept)", values = predictions.map { it.intercept }, intSeries = false)
        ).map { it.restrictToRealNumbers() }
    }

    //endregion

    //region CLEANUP UTILS

    /** Return copy of this series where values are forced to be increasing. */
    fun coerceIncreasing(): TimeSeries {
        val res = values.toMutableList()
        for (i in 1 until res.size) {
            res[i] = maxOf(res[i - 1], res[i])
        }
        return copy(values = res)
    }

    /** Return copy of this series where values are real numbers. */
    fun restrictToRealNumbers(): TimeSeries {
        val firstRealNumber = values.indexOfFirst { it.isFinite() }
        return when {
            firstRealNumber > 0 -> dropFirst(firstRealNumber)
            else -> this
        }
    }

    /** If time series starts with more than max zeros, trim so that it has max zeros. */
    fun restrictNumberOfStartingZerosTo(max: Int): TimeSeries {
        val firstNonZeroIndex = values.indexOfFirst { it != 0.0 }
        return when {
            firstNonZeroIndex > max -> dropFirst(firstNonZeroIndex - max)
            else -> this
        }
    }

    //endregion
}

//region List<TimeSeries> XF

/** Smooth the series over a 7-day window, with either a sum or an average. */
fun TimeSeries.smooth7(total: Boolean) = if (total) movingSum(7) else movingAverage(7)
/** Smooth all series over a 7-day window, with either a sum or an average. */
fun List<TimeSeries>.smooth7(total: Boolean) = map { it.smooth7(total) }

/** Organize by area, using first series for each area. */
fun Collection<TimeSeries>.byArea() = map { it.area to it }.toMap()
/** Organize by area id, using first series for each area. */
fun Collection<TimeSeries>.byAreaId() = map { it.areaId to it }.toMap()

/** Group by area. */
fun Collection<TimeSeries>.groupByArea() = groupBy { it.area }
/** Group by area id. */
fun Collection<TimeSeries>.groupByAreaId() = groupBy { it.areaId }

/** Organize by area, using first series for each area. */
fun Collection<TimeSeries>.deltas() = map { it.deltas() }

/** First date with a positive number of values for any of the given series. */
val Collection<TimeSeries>.firstPositiveDate
    get() = map { it.firstPositiveDate }.min()

/** Last date for any of the given series. */
val Collection<TimeSeries>.lastDate
    get() = map { it.end }.max()

/** Last date for any of the given series. */
val Collection<TimeSeries>.dateRange
    get() = (firstPositiveDate to lastDate).let {
        if (it.first != null && it.second != null) DateRange(it.first!!, it.second!!) else null
    }

/** Merge a bunch of time series by id and metric. */
fun List<TimeSeries>.regroupAndMerge(coerceIncreasing: Boolean) = groupBy { it.uniqueMetricKey }
        .map { it.value.merge() }
        .map { if (coerceIncreasing) it.coerceIncreasing() else it }
        .map { it.restrictNumberOfStartingZerosTo(5) }

/** Sums a bunch of time series by id and metric. */
fun List<TimeSeries>.regroupAndSum(coerceIncreasing: Boolean) = groupBy { it.uniqueMetricKey }
        .map { it.value.sum() }
        .map { if (coerceIncreasing) it.coerceIncreasing() else it }
        .map { it.restrictNumberOfStartingZerosTo(5) }

/** Merge a bunch of separate time series into a single time series object, using the max value in two series. */
private fun List<TimeSeries>.merge() = reduce { s1, s2 ->
    require(s1.areaId == s2.areaId)
    require(s1.metric == s2.metric)
    val minDate = minOf(s1.start, s2.start)
    val maxDate = maxOf(s1.end, s2.end)
    val series = (minDate..maxDate).map { maxOf(s1[it], s2[it]) }
    s1.copy(start = minDate, values = series)
}

/** Sums a bunch of separate time series into a single time series object, with an option to update area id and metric name. */
fun List<TimeSeries>.sum(altAreaId: String? = null, altMetric: String? = null) = reduce { s1, s2 -> reduceSeries(s1, s2) { a, b -> a + b } }
        .let { it.copy(areaId = altAreaId ?: it.areaId, metric = altMetric ?: it.metric) }

/** Reduces two series using the given operation. Result has domain that is the union of the two provided series. */
fun reduceSeries(s1: TimeSeries, s2: TimeSeries, op: (Double, Double) -> Double): TimeSeries {
    val minDate = minOf(s1.start, s2.start)
    val maxDate = maxOf(s1.end, s2.end)
    val series = (minDate..maxDate).map { op(s1[it], s2[it]) }
    return s1.copy(start = minDate, values = series)
}

//endregion

