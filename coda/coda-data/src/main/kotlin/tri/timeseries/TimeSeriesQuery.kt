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

import tri.area.AreaInfo
import java.time.YearMonth

/** Manages access to a variety of time series, and provides simple query access. */
open class TimeSeriesQuery(vararg _sources: TimeSeriesProcessor) {

    val sources = _sources.toList()
    /** Load all data into memory, grouped by area. */
    val data by lazy { sources.flatMap { it.data() }.groupByArea() }
    /** Flat version of all data. */
    val flatData by lazy { data.flatMap { it.value } }
    /** List of all areas in the data. */
    val areas by lazy { data.keys }

    //region QUERIES

    /** Query all data based on area. */
    fun byArea(area: AreaInfo) = data[area] ?: emptyList()
    /** Query all data based on area. */
    fun byArea(areaFilter: (AreaInfo) -> Boolean) = data.filterKeys(areaFilter)

    /** Query by area and metric/qualifier. */
    fun by(area: AreaInfo, metric: String? = null, qualifier: String? = null) =
            data[area]?.filter { (metric == null || it.metric == metric) && (qualifier == null || it.qualifier == qualifier) } ?: emptyList()

    /** Query all data based on area and metric. */
    fun by(areaFilter: (AreaInfo) -> Boolean, metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean = { true }) =
        data.filterKeys(areaFilter)
            .mapValues { it.value.filter { metricFilter(it.metric) && qualifierFilter(it.qualifier) } }
            .filterValues { it.isNotEmpty() }

    /** Query all data based on a generic filter. */
    fun by(filter: (TimeSeries) -> Boolean) = flatData.filter(filter)

    /** Query for daily version of time series. */
    open fun daily(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for cumulative version of time series. */
    open fun cumulative(area: AreaInfo, metric: String): TimeSeries? = null

    /** Query for weekly average version of time series. */
    open fun weeklyAverage(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for weekly total version of time series. */
    open fun weeklyTotal(area: AreaInfo, metric: String): TimeSeries? = null

    /** Query for 14-day total version of time series. */
    open fun biweeklyTotal(area: AreaInfo, metric: String): TimeSeries? = null

    /** Query for monthly average value. */
    open fun monthlyAverage(area: AreaInfo, metric: String, month: YearMonth): Double? = null
    /** Query for monthly total value. */
    open fun monthlyTotal(area: AreaInfo, metric: String, month: YearMonth): Double? = null

    //endregion

}
