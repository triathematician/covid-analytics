/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
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
import tri.timeseries.io.TimeSeriesProcessor
import java.time.YearMonth
import kotlin.time.ExperimentalTime

/** Manages access to a variety of time series, and provides simple query access. */
@ExperimentalTime
open class TimeSeriesQuery(vararg _sources: TimeSeriesProcessor) {

    /** List of sources. */
    val sources = _sources.toMutableList()
    /** Data associated with a given source. */
    private val sourceData = mutableMapOf<TimeSeriesProcessor, Map<AreaInfo, List<TimeSeries>>>()

//    /** Load all data into memory, grouped by area. */
//    private val data by lazy { sources.flatMap { it.data() }.groupByArea() }
//    /** Flat version of all data. */
//    private val flatData by lazy { data.flatMap { it.value } }
//    /** List of all areas in the data. */
//    private val areas by lazy { data.keys }

    //region QUERIES

//    /** Query all data based on area. */
//    fun byArea(area: AreaInfo) = data[area] ?: emptyList()

    /** Get all areas in the data set (loading all data). */
    fun allDataAreas() = allSources().flatMap { sourceData[it]!!.keys }.toSet()

    /** Get all data matching given area filter (loading all data). */
    fun allDataByArea(areaFilter: (AreaInfo) -> Boolean) = allSources().map { sourceData[it]!!.filterKeys(areaFilter) }
        .flatMap { it.values.flatten() }.groupByArea()

    /** Query by area and metric/qualifier. */
    fun by(area: AreaInfo, metric: String, qualifier: String = "") = sourcesFor(area, metric, qualifier)
        .flatMap { it.query(area, metric, qualifier) }

    /** Query all data based on area and metric. */
    fun by(areaFilter: (AreaInfo) -> Boolean, metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean = { true }) =
        sourcesFor(metricFilter, qualifierFilter).flatMap { it.query(areaFilter, metricFilter, qualifierFilter) }

//    /** Query all data based on a generic filter. */
//    fun by(filter: (TimeSeries) -> Boolean) = flatData.filter(filter)

    /** Query for raw data. */
    open fun data(area: AreaInfo, metric: String): TimeSeries? = null

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

    //region UTILS

    /** Loads all data sources. */
    private fun allSources() = sources.onEach { it.loadData() }

    private fun sourcesFor(area: AreaInfo, metric: String, qualifier: String = "") = sources.filter { it.provides(area, metric, qualifier) }

    private fun sourcesFor(metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean) = sources.filter {
        it.metricsProvided().any(metricInfoFilter(metricFilter, qualifierFilter))
    }

    private fun metricInfoFilter(metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean): (MetricInfo) -> Boolean =
        { metricFilter(it.metric) && qualifierFilter(it.qualifier) }

    private fun TimeSeriesProcessor.query(area: AreaInfo, metric: String, qualifier: String? = ""): List<TimeSeries> {
        if (this !in sourceData.keys) loadData()
        return sourceData[this]!!.getOrElse(area) { listOf() }
            .filter { (it.metric == metric) && (qualifier == null || it.qualifier == qualifier) }
    }

    private fun TimeSeriesProcessor.query(areaFilter: (AreaInfo) -> Boolean, metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean): List<TimeSeries> {
        if (this !in sourceData.keys) loadData()
        val data = sourceData[this]!!
        val filter = metricInfoFilter(metricFilter, qualifierFilter)
        return data.filterKeys(areaFilter).flatMap { it.value.filter { filter(it.metricInfo) } }
    }

    private fun TimeSeriesProcessor.loadData() {
        if (!sourceData.containsKey(this))
            sourceData[this] = data().groupByArea()
    }

    //endregion
}
