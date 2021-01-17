/*-
 * #%L
 * coda-app
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
package tri.covid19.coda.utils

import tri.timeseries.TimeSeries
import tri.util.DateRange
import java.time.LocalDate

/** A named set of (x,y) data points. */
data class ChartDataSeries(var id: String, var points: DataPoints) {
    fun maxX() = points.map { it.first.toDouble() }.maxOrNull()
    fun maxY() = points.map { it.second.toDouble() }.maxOrNull()
}

/** Construct series from time series, using indices of metric's domain for x values. */
fun series(id: String, s: TimeSeries) = ChartDataSeries(id, s.domain.mapIndexed { i, d -> i to s.getOrNull(d) }.filterNullValues())

/** Construct series from time series, using indices of alternate domain for x values. */
fun series(id: String, domain: DateRange, s: TimeSeries) = ChartDataSeries(id, domain.mapIndexed { i, d -> i to s.getOrNull(d) }.filterNullValues())

/** Construct series from two time series, using the common domain between the two. */
fun series(id: String, x: TimeSeries, y: TimeSeries) = series(id, x.domain.intersect(y.domain) ?: emptySet(), x, y)

/** Construct series from two time series, using the common domain between the two. */
fun series(id: String, domain: Iterable<LocalDate>, x: TimeSeries, y: TimeSeries) = ChartDataSeries(id, domain.mapIndexed { _, d -> x.getFinite(d) to y.getFinite(d) }.filterNullValues())

/** Get finite value from data series. */
fun TimeSeries.getFinite(d: LocalDate) = getOrNull(d)?.let { if (it.isFinite()) it else null }

/** Filters out nulls from pairs. */
private fun <X, Y> List<Pair<X?, Y?>>.filterNullValues() = filter { it.first != null && it.second != null } as List<Pair<X, Y>>
