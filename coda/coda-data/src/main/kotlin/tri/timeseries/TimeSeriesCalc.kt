package tri.timeseries

import tri.area.Lookup
import tri.util.DateRange

//region OPERATORS ON SINGLE/MULTIPLE [TimeSeries]

/** Smooth all series over a 7-day window, with either a sum or an average. */
fun Collection<TimeSeries>.smooth7(total: Boolean) = map { it.smooth7(total) }

/** Organize by area, using first series for each area. */
fun Collection<TimeSeries>.deltas() = map { it.deltas() }

/** Merge [TimeSeries] by unique key, taking the max value across instances. */
fun List<TimeSeries>.regroupAndMax(coerceIncreasing: Boolean) = groupBy { it.uniqueMetricKey }
        .map { it.value.max() }
        .map { if (coerceIncreasing) it.coerceIncreasing() else it }
        .map { it.restrictNumberOfStartingZerosTo(5) }

/** Merge [TimeSeries] by unique key, summing across instances. */
fun List<TimeSeries>.regroupAndSum(coerceIncreasing: Boolean) = groupBy { it.uniqueMetricKey }
        .map { it.value.sum() }
        .map { if (coerceIncreasing) it.coerceIncreasing() else it }
        .map { it.restrictNumberOfStartingZerosTo(5) }

/** Merge [TimeSeries] by unique key, filling in any missing days with the latest available value rather than the default value. */
fun List<TimeSeries>.regroupAndLatest() = groupBy { it.uniqueMetricKey }
        .map {
            val first = it.value[0]
            val valueMap = it.value.flatMap { it.valuesAsMap.entries }.map { it.key to it.value }.toMap()
            TimeSeries(first.source, first.areaId, first.metric, first.qualifier, 0.0, valueMap, fillLatest = true)
        }

/** Merge [TimeSeries] by unique key, using the first nonzero value in two series. */
fun List<TimeSeries>.firstNonZero(altAreaId: String? = null, altMetric: String? = null) = reduce { s1, s2 ->
    val dates = listOf(s1, s2).dateRange!!
    val series = dates.map { listOf(s1[it], s2[it]).firstOrNull { it != 0.0 } ?: 0.0 }
    s1.copy(start = dates.start, values = series)
}.copy(altAreaId, altMetric)


/** Merge [TimeSeries] by unique key, using the minimum value across series. */
fun List<TimeSeries>.min(altAreaId: String? = null, altMetric: String? = null) = reduce { s1, s2 ->
    val dates = listOf(s1, s2).dateRange!!
    val series = dates.map { minOf(s1[it], s2[it]) }
    s1.copy(start = dates.start, values = series)
}.copy(altAreaId, altMetric)

/** Merge [TimeSeries] by unique key, using the maximum value across series. */
fun List<TimeSeries>.max(altAreaId: String? = null, altMetric: String? = null) = reduce { s1, s2 ->
    val dates = listOf(s1, s2).dateRange!!
    val series = dates.map { maxOf(s1[it], s2[it]) }
    s1.copy(start = dates.start, values = series)
}.copy(altAreaId, altMetric)

/** Merge [TimeSeries] by unique key, using the sum across series. */
fun List<TimeSeries>.sum(altAreaId: String? = null, altMetric: String? = null) = reduce { s1, s2 ->
    reduceSeries(s1, s2) { a, b -> a + b }
}.copy(altAreaId, altMetric)

/** Merge [TimeSeries] by unique key, using the given reduce operation. */
fun reduceSeries(s1: TimeSeries, s2: TimeSeries, op: (Double, Double) -> Double): TimeSeries {
    val dates = listOf(s1, s2).dateRange!!
    val series = dates.map { op(s1[it], s2[it]) }
    return s1.copy(start = dates.start, values = series, intSeries = s1.intSeries && s2.intSeries)
}

private fun TimeSeries.copy(altAreaId: String? = null, altMetric: String? = null) = copy(areaId = altAreaId ?: areaId, metric = altMetric ?: metric)

//endregion

//region EXTENSION FUNCTIONS FOR CALCULATING/PROPERTIES

/** Organize by area, using first series for each area. */
fun Collection<TimeSeries>.byArea() = map { it.area to it }.toMap()

/** Organize by area id, using first series for each area. */
fun Collection<TimeSeries>.byAreaId() = map { it.areaId to it }.toMap()

/** Group by area. */
fun Collection<TimeSeries>.groupByArea() = filter { Lookup.areaOrNull(it.areaId) != null }.groupBy { it.area }

/** Group by area id. */
fun Collection<TimeSeries>.groupByAreaId() = groupBy { it.areaId }

/** First date with a positive number of values for any of the given series. */
val Collection<TimeSeries>.firstPositiveDate
    get() = map { it.firstPositiveDate }.minOrNull()

/** Last date for any of the given series. */
val Collection<TimeSeries>.lastDate
    get() = map { it.end }.maxOrNull()

/** Last date for any of the given series. */
val Collection<TimeSeries>.dateRange
    get() = (firstPositiveDate to lastDate).let {
        if (it.first != null && it.second != null) DateRange(it.first!!, it.second!!) else null
    }

//endregion