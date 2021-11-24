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
package tri.timeseries

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.area.Lookup
import tri.timeseries.analytics.computeLogisticPrediction
import tri.util.DateRange
import tri.util.dateRange
import tri.util.minus
import tri.util.rangeTo
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Time series of a single metric. Stores values as doubles, but will report them as [Int]s if a flag is set. */
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
    val values: List<Double> = listOf()
) {

    /** Construct with a set of floating-point values. */
    constructor(source: String, areaId: String, metric: String, qualifier: String = "", defValue: Double = 0.0, values: Map<LocalDate, Double>, fillLatest: Boolean = false)
            : this(source, areaId, metric, qualifier, defValue, values.keys.minOrNull()!!, *values.valueList(defValue, fillLatest).toDoubleArray())

    /** Construct with explicit floating-point values. */
    constructor(source: String, areaId: String, metric: String, qualifier: String = "", defValue: Double = 0.0, start: LocalDate, vararg values: Double)
            : this(source, areaId, metric, qualifier, false, defValue, start, values.toList())

    /** Construct with a set of integer values. */
    constructor(source: String, areaId: String, metric: String, qualifier: String = "", defValue: Int = 0, values: Map<LocalDate, Int>)
            : this(source, areaId, metric, qualifier, defValue, values.keys.minOrNull()!!, values.valueList(defValue))

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
    val metricInfo
        get() = MetricInfo(metric, qualifier)

    @get:JsonIgnore
    val firstPositiveDate: LocalDate
        get() = (start..end).firstOrNull { get(it) > 0.0 } ?: end
    @get:JsonIgnore
    val end: LocalDate
        get() = date(values.size - 1)
    @get:JsonIgnore
    val domain: DateRange
        get() = DateRange(start, end)

    @get:JsonIgnore
    val size: Int
        get() = values.size
    val valuesAsMap: Map<LocalDate, Double>
        get() = values.mapIndexed { i, d -> date(i) to d }.toMap()
    @get:JsonIgnore
    val lastValue: Double
        get() = values.lastOrNull() ?: 0.0

    //endregion

    //region VALUE AND DATE GETTERS

    /** Get value on given date. */
    operator fun get(date: LocalDate): Double = values.getOrElse(indexOf(date)) { defValue }

    /** Get value on given date, or null if argument is outside range. */
    fun getOrNull(date: LocalDate): Double? = values.getOrNull(indexOf(date))

    /** Get date by index. */
    fun date(i: Int) = start.plusDays(i.toLong())!!

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
        .maxByOrNull { it.second }!!

    /** Get values for the given range of dates. */
    fun values(dates: DateRange) = dates.map { get(it) }

    /** Compute sum over all dates in given range. */
    fun sum(dates: DateRange) = values(dates).sum()

    /** Compute average over all dates in given range. */
    fun average(dates: DateRange) = values(dates).average()

    /** Compute sum over all dates in given range. */
    fun sum(month: YearMonth) = values(month.dateRange).sum()

    /** Compute average over all dates in given range. */
    fun average(month: YearMonth) = values(month.dateRange).average()

    /** Compute number of days since value was half of its current value. Returns null if current value is not positive. */
    fun daysSinceHalfCurrentValue(): Int? {
        val cur = lastValue
        if (cur <= 0) return null
        (1..values.size).forEach { if (valueByDaysFromEnd(it) <= .5 * cur) return it }
        return null
    }

    //endregion

    //region DERIVED SERIES

    /** Copy where data is restricted to start no early than given date. */
    fun copyWithDataSince(firstDate: LocalDate) =
        copy(metric = metric, start = maxOf(start, firstDate), values = values.drop(maxOf(start, firstDate).minus(start).toInt()), intSeries = intSeries)

    /** Create a copy while adjusting the start day forward/back if the number of values is more/less than current number of values. Assumes the end of the series is fixed date. */
    fun copyAdjustingStartDay(metric: String = this.metric, values: List<Double> = this.values, intSeries: Boolean = this.intSeries) =
        copy(metric = metric, start = date(this.values.size - values.size), values = values, intSeries = intSeries)

    /**
     * Create copy after filling data through a given date.
     */
    fun copyExtendedThrough(date: LocalDate, fill: TimeSeriesFillStrategy): TimeSeries {
        if (!date.isAfter(end)) {
            return this
        }
        val daysToAdd = date - end
        val fillValue = when (fill) {
            TimeSeriesFillStrategy.FILL_WITH_ZEROS -> 0.0
            TimeSeriesFillStrategy.FILL_LAST -> values.last()
        }
        return copy(values = values + (1..daysToAdd).map { fillValue })
    }

    /** Copy after dropping first n values. */
    fun dropFirst(n: Int) = copyAdjustingStartDay(values = values.drop(n))

    /** Copy after dropping first n values. */
    fun dropLast(n: Int): TimeSeries {
        val res = copyAdjustingStartDay(values = values.dropLast(n))
        return res
    }

    /** Apply arbitrary operator to series. */
    fun transform(op: (Double) -> Double) = copy(values = values.map { op(it) })

    operator fun plus(n: Number) = copy(values = values.map { it + n.toDouble() })
    operator fun minus(n: Number) = copy(values = values.map { it - n.toDouble() })
    operator fun times(n: Number) = copy(values = values.map { it * n.toDouble() })
    operator fun div(n: Number) = copy(values = values.map { it / n.toDouble() })

    operator fun plus(n: TimeSeries) = mergeSeries(this, n) { a, b -> a + b }
    operator fun minus(n: TimeSeries) = mergeSeries(this, n) { a, b -> a - b }
    operator fun times(n: TimeSeries) = mergeSeries(this, n) { a, b -> a * b }
    operator fun div(n: TimeSeries) = mergeSeries(this, n) { a, b -> a / b }

    /**
     * Subtracts the other [TimeSeries], but constrain so that only positive values are in the result. Trims the time
     * series start so that all values are positive.
     */
    fun minusMustBePositive(n: TimeSeries) = minus(n).let {
        val lastZero = it.valuesAsMap.entries.findLast { it.value <= 0 }
        if (lastZero == null) it else it.copyWithDataSince(lastZero.key.plusDays(1))
    }

    /** Smooth the series over a 7-day window, with either a sum or an average. */
    fun smooth7(total: Boolean) = if (total) movingSum(7) else movingAverage(7)

    /** Return copy with moving averages. If bucket is <=1, returns this series. */
    fun movingAverage(bucket: Int, nonZero: Boolean = false, includePartialList: Boolean = true) =
        when {
            bucket <= 1 -> this
            else -> copyAdjustingStartDay(values = values.movingAverage(bucket, nonZero, includePartialList))
        }

    /** Return copy with moving sum. If bucket is <=1, returns this series. */
    fun movingSum(bucket: Int, includePartialList: Boolean = true) = when {
        bucket <= 1 -> this
        else -> copyAdjustingStartDay(values = values.movingSum(bucket, includePartialList))
    }

    /** Return copy with deltas. */
    fun deltas(offset: Int = 1, metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric),
        values = values.deltas(offset)
    )

    /** Return copy with deltas between averages of [bucket] successive values. */
    fun averageDeltas(bucket: Int, metricFunction: (String) -> String = { it }) =
        copyAdjustingStartDay(
            metric = metricFunction(metric), intSeries = false,
            values = values.movingAverage(bucket, includePartialList = true).deltas(bucket)
        )

    /** Compute cumulative totals. */
    fun cumulative(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric),
        values = values.partialSums()
    )

    /** Compute cumulative totals starting on the given day. */
    fun cumulativeSince(cumulativeStart: LocalDate, metricFunction: (String) -> String = { it }) =
        copyAdjustingStartDay(
            metric = metricFunction(metric),
            values = values(DateRange(cumulativeStart..end)).partialSums()
        )

    /** Return copy with absolute changes by offsetting days. */
    fun absoluteChanges(offset: Int = 7, metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric), intSeries = false,
        values = values.changes(offset)
    ).restrictToRealNumbers()

    /** Return copy with percent changes. */
    fun percentChanges(bucket: Int = 1, offset: Int = bucket, metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric), intSeries = false,
        values = values.movingAverage(bucket).percentChanges(offset = offset)
    ).restrictToRealNumbers()

    /** Return copy with growth rates. */
    fun growthRates(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric), intSeries = false,
        values = values.growthRates()
    ).restrictToRealNumbers()

    /** Return copy with growth percentages. */
    fun symmetricGrowth(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric), intSeries = false,
        values = values.symmetricGrowth()
    ).restrictToRealNumbers()

    /** Return copy with doubling times. */
    fun doublingTimes(metricFunction: (String) -> String = { it }) = copyAdjustingStartDay(
        metric = metricFunction(metric), intSeries = false,
        values = values.doublingTimes()
    ).restrictToRealNumbers()

    /** Return derived metrics with logistic predictions, using given number of days for linear regression. */
    fun shortTermLogisticForecast(days: Int): List<TimeSeries> {
        val predictions = values.computeLogisticPrediction(days).filter { it.hasBoundedConfidence }
        return listOf(
            copyAdjustingStartDay(metric = "$metric (predicted total)", values = predictions.map { it.kTotal }),
            copyAdjustingStartDay(metric = "$metric (predicted total, min)", values = predictions.map { it.minKTotal }),
            copyAdjustingStartDay(metric = "$metric (predicted total, max)", values = predictions.map { it.maxKTotal }),
            copyAdjustingStartDay(metric = "$metric (predicted peak)", values = predictions.map { it.peakGrowth }),
            copyAdjustingStartDay(
                metric = "$metric (days to peak)",
                values = predictions.map { it.daysToPeak },
                intSeries = false
            )
//                copyAdjustingStartDay(metric = "$metric (logistic slope)", values = predictions.map { it.slope }, intSeries = false),
//                copyAdjustingStartDay(metric = "$metric (logistic intercept)", values = predictions.map { it.intercept }, intSeries = false)
        ).map { it.restrictToRealNumbers() }
    }

    //endregion

    //region CLEANUP UTILS

    /**
     * Return copy of this series where values are forced to be increasing.
     */
    fun coerceIncreasing(): TimeSeries {
        val res = values.toMutableList()
        for (i in 1 until res.size) {
            res[i] = maxOf(res[i - 1], res[i])
        }
        return copy(values = res)
    }

    /**
     * Return copy of this series where zero values are replaced by the previous value.
     */
    fun replaceZerosWithPrevious(): TimeSeries {
        val res = values.toMutableList()
        for (i in 1 until res.size) {
            if (res[i] == 0.0) res[i] = res[i - 1]
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

/**
 * Utility that converts a key-value map by date to a list of values.
 * @param valueIfMissing used in result list if a date is missing
 * @param fillLatest if true, will fill in any missing values with the latest value prior to the missing date
 */
private fun <X> Map<LocalDate, X>.valueList(valueIfMissing: X, fillLatest: Boolean = false) = if (fillLatest) {
    var recent: X? = null
    DateRange(keys).map {
        val x = getOrDefault(it, recent ?: valueIfMissing)
        recent = x
        x
    }
} else DateRange(keys).map { getOrDefault(it, valueIfMissing) }

enum class TimeSeriesFillStrategy {
    FILL_WITH_ZEROS,
    FILL_LAST;
}