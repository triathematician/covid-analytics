package tri.timeseries

import java.time.LocalDate
import tri.util.DateRange

/**
 * Provides various ways to aggregate date/value data into a [TimeSeries].
 */
enum class TimeSeriesAggregate(private val aggregator: (List<Pair<LocalDate, Number?>>, LocalDate?) -> Map<LocalDate, Number>) {
    /** Sums entries. */
    SUM({ pairs, date ->
        val dateValues = pairs.groupBy { it.first }.mapValues { it.value.map { it.second } }.toSortedMap()
        if (dateValues.isEmpty()) mapOf<LocalDate, Number>()
        else DateRange(dateValues.keys.first()..(date ?: dateValues.keys.last())).map {
            val values = dateValues[it] ?: listOf()
            val integers = values.all { it is Int }
            val sum: Number = if (integers) values.sumBy { it?.toInt() ?: 0 } else values.sumByDouble { it?.toDouble() ?: 0.0 }
            it to sum
        }.toMap()
    }),

    /** Fills latest value, ensuring nonzero entries. */
    FILL_WITH_LATEST_VALUE({ pairs, date ->
        val dateValues = pairs.map { it.first to it.second }.toMap().toSortedMap()
        if (dateValues.isEmpty()) mapOf<LocalDate, Number>()
        else DateRange(dateValues.keys.first()..(date ?: dateValues.keys.last())).map {
            it to if (it in dateValues.keys) dateValues[it]!!
            else dateValues.headMap(it).values.last()!!
        }.toMap()
    });

/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 Elisha Peterson
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

    operator fun invoke(entries: List<Pair<LocalDate, Number>>, date: LocalDate?) = aggregator(entries, date)
}
