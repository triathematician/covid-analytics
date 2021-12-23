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

import tri.util.DateRange
import tri.util.minus
import tri.util.plus
import java.time.LocalDate

/**
 * Provides various ways to aggregate date/value data into a [TimeSeries].
 */
enum class TimeSeriesAggregate(private val aggregator: (List<Pair<LocalDate, Number?>>, LocalDate?) -> Map<LocalDate, Number>) {
    /** Gets first entry for each date. */
    FIRST({ pairs, _ ->
        pairs.filter { it.second != null }.associate { it as Pair<LocalDate, Number> }.toSortedMap()
    }),

    /** Sums entries. */
    SUM({ pairs, finalDate ->
        if (pairs.isEmpty()) mapOf<LocalDate, Number>() else {
            val dateValues = pairs.groupBy { it.first }.mapValues { it.value.map { it.second } }.toSortedMap()
            associateDates(dateValues.keys, finalDate) {
                val values = dateValues[it] ?: listOf()
                val integers = values.all { it is Int }
                it to if (integers) values.sumBy { it?.toInt() ?: 0 } else values.sumByDouble { it?.toDouble() ?: 0.0 }
            }
        }
    }),

    /** Takes a 7-day average, skipping any missing values. If there is more than one entry per date, sums across those dates. */
    AVERAGE_7_DAY({ pairs, finalDate ->
        if (pairs.isEmpty()) mapOf<LocalDate, Number>() else {
            val dateValues = pairs.groupBy { it.first }.mapValues { it.value.map { it.second } }.toSortedMap()
            associateDates(dateValues.keys, finalDate) {
                it to dateValues.subMap(it - 6, it + 1).values
                    .mapNotNull { it.sumOrNull() }
                    .average()
            }.filter { it.value.isFinite() }
        }
    }),

    /** Fills latest value, ensuring gaps between missing entries are all filled. */
    FILL_WITH_LATEST_VALUE({ pairs, finalDate ->
        if (pairs.isEmpty()) mapOf<LocalDate, Number>() else {
            val sortedDates = pairs.associate { it.first to it.second }.toSortedMap()
            associateDates(sortedDates.keys, finalDate) {
                it to (sortedDates[it] ?: sortedDates.headMap(it).values.last()!!)
            }
        }
    }),

    /** Fills latest value, but does not allow filling forward more than 7 days. */
    FILL_WITH_LATEST_VALUE_UP_TO_7({ pairs, finalDate ->
        if (pairs.isEmpty()) mapOf<LocalDate, Number>() else {
            val sortedDates = pairs.associate { it.first to it.second?.toDouble() }.toSortedMap()
            val first = sortedDates.keys.first()
            val last = finalDate ?: sortedDates.keys.last()
            val dates = DateRange(first..last).toList()
            val values = dates.map { sortedDates[it] }
            var lastValueIndex = 0
            var lastValue = 0.0
            values.mapIndexed { i, value ->
                dates[i] to when {
                    value != null -> {
                        lastValueIndex = i
                        lastValue = value
                        value
                    }
                    i - lastValueIndex <= 7 -> lastValue
                    else -> 0.0
                }
            }.toMap().toSortedMap()
        }
    });

    operator fun invoke(entries: List<Pair<LocalDate, Number>>, date: LocalDate?) = aggregator(entries, date)

    companion object {
        private fun <Y> associateDates(sortedKeys: Set<LocalDate>, finalDate: LocalDate?, op: (LocalDate) -> Pair<LocalDate, Y>) =
            DateRange(sortedKeys.first()..(finalDate ?: sortedKeys.last())).associate(op).toSortedMap()

        private fun List<Number?>.sumOrNull(): Double? = mapNotNull { it?.toDouble() }.filter { it.isFinite() }.let {
            if (it.isEmpty()) null else it.sum()
        }
    }
}