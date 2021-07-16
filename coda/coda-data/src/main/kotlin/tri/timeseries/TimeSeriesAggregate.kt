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

import java.time.LocalDate
import tri.util.DateRange
import tri.util.minus

/**
 * Provides various ways to aggregate date/value data into a [TimeSeries].
 */
enum class TimeSeriesAggregate(private val aggregator: (List<Pair<LocalDate, Number?>>, LocalDate?) -> Map<LocalDate, Number>) {
    /** Sums entries. */
    SUM({ pairs, finalDate ->
        val dateValues = pairs.groupBy { it.first }.mapValues { it.value.map { it.second } }.toSortedMap()
        if (dateValues.isEmpty()) mapOf<LocalDate, Number>()
        else DateRange(dateValues.keys.first()..(finalDate ?: dateValues.keys.last())).map {
            val values = dateValues[it] ?: listOf()
            val integers = values.all { it is Int }
            val sum: Number = if (integers) values.sumBy { it?.toInt() ?: 0 } else values.sumByDouble { it?.toDouble() ?: 0.0 }
            it to sum
        }.toMap()
    }),

    /** Fills latest value, ensuring gaps between missing entries are all filled. */
    FILL_WITH_LATEST_VALUE({ pairs, date ->
        val dateValues = pairs.map { it.first to it.second }.toMap().toSortedMap()
        if (dateValues.isEmpty()) mapOf<LocalDate, Number>()
        else DateRange(dateValues.keys.first()..(date ?: dateValues.keys.last())).map {
            it to if (it in dateValues.keys) dateValues[it]!!
            else dateValues.headMap(it).values.last()!!
        }.toMap()
    }),

    /** Fills latest value, but does not allow filling forward more than 7 days. */
    FILL_WITH_LATEST_VALUE_UP_TO_7({ pairs, date ->
        val sortedDates = pairs.map { it.first to it.second?.toDouble() }.toMap().toSortedMap()
        val first = sortedDates.keys.first()
        val last = sortedDates.keys.last()
        val dates = DateRange(first..last).toList()
        val values = dates.map { sortedDates[it] }
        var lastValueIndex = 0
        var lastValue = 0.0
        val adjustedValues = values.mapIndexed { i, value ->
            dates[i] to if (value != null) {
                lastValueIndex = i
                lastValue = value
                value
            } else if (i - lastValueIndex <= 7) {
                lastValue
            } else {
                0.0
            }
        }.toMap()
        adjustedValues
    });

    operator fun invoke(entries: List<Pair<LocalDate, Number>>, date: LocalDate?) = aggregator(entries, date)
}