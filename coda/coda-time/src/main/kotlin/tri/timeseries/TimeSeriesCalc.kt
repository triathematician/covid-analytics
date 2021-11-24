/*-
 * #%L
 * coda-data-0.1.9-SNAPSHOT
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

//region GENERIC REDUCE OPERATIONS

/** Reduces time series by given operation, using the given reduce operation. */
fun List<DailyTimeSeries>.mergeSeries(op: (List<Double>) -> Double): DailyTimeSeries {
    val dates = dateRange()
    val intSeries = all { it.intSeries }
    val values = dates.map { date -> op(map { it[date] }) }
    return get(0).copy(start = dates.start, values = values, intSeries = intSeries)
}

/** Merge two [DailyTimeSeries] using the given operation. */
fun mergeSeries(s1: DailyTimeSeries, s2: DailyTimeSeries, op: (Double, Double) -> Double) = listOf(s1, s2).mergeSeries { it.reduce(op) }

/** First date with a positive number of values for any of the given series. */
fun Collection<DailyTimeSeries>.firstPositiveDateOrNull() = map { it.firstPositiveDate }.minOrNull()

/** Last date for any of the given series. */
fun Collection<DailyTimeSeries>.lastDateOrNull() = map { it.end }.maxOrNull()

/** Last date for any of the given series. */
fun Collection<DailyTimeSeries>.dateRange() = DateRange(firstPositiveDateOrNull()!!, lastDateOrNull()!!)

/** Last date for any of the given series. */
fun Collection<DailyTimeSeries>.dateRangeOrNull() = (firstPositiveDateOrNull() to lastDateOrNull()).let {
    if (it.first != null && it.second != null) DateRange(it.first!!, it.second!!) else null
}

//endregion

//region SPECIFIC MERGE OPERATIONS

/** Merge [TimeSeries] by unique key, using the first nonzero value in the series. */
fun List<DailyTimeSeries>.firstNonZero(altAreaId: String? = null, altMetric: String? = null) = mergeSeries { it.firstOrNull { it != 0.0 } ?: 0.0 }
        .copy(altAreaId, altMetric)

/** Merge [TimeSeries] by unique key, using the minimum value across series. */
fun List<DailyTimeSeries>.min(altAreaId: String? = null, altMetric: String? = null) = mergeSeries { it.minOrNull() ?: 0.0 }
        .copy(altAreaId, altMetric)

/** Merge [TimeSeries] by unique key, using the maximum value across series. */
fun List<DailyTimeSeries>.max(altAreaId: String? = null, altMetric: String? = null) = mergeSeries { it.maxOrNull() ?: 0.0 }
        .copy(altAreaId, altMetric)

/** Merge [TimeSeries] by unique key, using the sum across series. */
fun List<DailyTimeSeries>.sum(altAreaId: String? = null, altMetric: String? = null) = mergeSeries { it.sum() }
        .copy(altAreaId, altMetric)

private fun DailyTimeSeries.copy(altAreaId: String? = null, altMetric: String? = null) = copy(areaId = altAreaId ?: areaId, metric = altMetric ?: metric)

//endregion

//region REGROUPING OPERATIONS

/**
 * Merge [TimeSeries] by unique key, taking the max value across instances.
 * @param coerceIncreasing if true, forces the series to be always increasing (e.g. to force a cumulative series)
 * @param replaceZerosWithPrevious if true, replaces any zeros in the middle of the series with the prior value
 *   (e.g. to force a cumulative series with occasional downward corrections). This flag is ignored if [coerceIncreasing] is true.
 */
fun List<DailyTimeSeries>.regroupAndMax(coerceIncreasing: Boolean, replaceZerosWithPrevious: Boolean) = groupBy { it.uniqueMetricKey }
        .map { it.value.max() }
        .map { if (coerceIncreasing) it.coerceIncreasing() else if (replaceZerosWithPrevious) it.replaceZerosWithPrevious() else it }
        .map { it.restrictNumberOfStartingZerosTo(5) }

/** Merge [TimeSeries] by unique key, summing across instances. */
fun List<DailyTimeSeries>.regroupAndSum(coerceIncreasing: Boolean) = groupBy { it.uniqueMetricKey }
        .map { it.value.sum() }
        .map { if (coerceIncreasing) it.coerceIncreasing() else it }
        .map { it.restrictNumberOfStartingZerosTo(5) }

/** Merge [TimeSeries] by unique key, filling in any missing days with the latest available value rather than the default value. */
fun List<DailyTimeSeries>.regroupAndLatest() = groupBy { it.uniqueMetricKey }
        .map {
            val first = it.value[0]
            val valueMap = it.value.flatMap { it.valuesAsMap.entries }.map { it.key to it.value }.toMap()
            DailyTimeSeries(first.source, first.areaId, first.metric, first.qualifier, 0.0, valueMap, fillLatest = true)
        }

//endregion

//region EXTENSION FUNCTIONS FOR CALCULATING/PROPERTIES

/** Organize by area id, using first series for each area. */
fun Collection<DailyTimeSeries>.byAreaId() = associate { it.areaId to it }
/** Group by area id. */
fun Collection<DailyTimeSeries>.groupByAreaId() = groupBy { it.areaId }

//endregion
