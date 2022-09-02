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
package tri.timeseries.query

import tri.area.AreaInfo
import tri.area.AreaLookup
import tri.timeseries.MetricInfo
import tri.timeseries.TimeSeries
import tri.timeseries.io.TimeSeriesProcessor
import java.time.YearMonth
import kotlin.time.ExperimentalTime

/** Manages access to a variety of time series, and provides simple query access. */
open class TimeSeriesQuery(val lookup: AreaLookup, vararg _sources: TimeSeriesProcessor) {

    /** List of sources. */
    val sources = _sources.toMutableList()
    /** Data associated with a given source. */
    private val sourceData = mutableMapOf<TimeSeriesProcessor, Map<AreaInfo, List<TimeSeries>>>()

    /** Load all data into memory, grouped by area. */
    private val data by lazy { sources.flatMap { it.data() }.groupByArea(lookup) }
    /** Flat version of all data. */
    private val flatData by lazy { data.flatMap { it.value } }
    /** List of all areas in the data. */
    private val areas by lazy { data.keys }

    //region QUERIES

    /** Query all data based on area. */
    fun byArea(area: AreaInfo): List<TimeSeries> =
            data[area] ?: emptyList()

    /** Get all areas in the data set (loading all data). */
    fun allDataAreas() =
            allSources(lookup).flatMap { sourceData[it]!!.keys }.toSet()

    /** Get all data matching given area filter (loading all data). */
    fun allDataByArea(areaFilter: (AreaInfo) -> Boolean) =
            allSources(lookup).map { sourceData[it]!!.filterKeys(areaFilter) }
                    .flatMap { it.values.flatten() }.groupByArea(lookup)

    /** Query by area and metric/qualifier. */
    fun by(area: AreaInfo, metric: String, qualifier: String = "") =
            sourcesFor(area, metric, qualifier)
                    .flatMap { it.query(area, metric, qualifier) }

    /** Query all data based on area and metric. */
    fun by(areaFilter: (AreaInfo) -> Boolean, metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean = { true }) =
        sourcesFor(metricFilter, qualifierFilter)
                .flatMap { it.query(areaFilter, metricFilter, qualifierFilter) }

    /** Query all data based on a generic filter. */
    fun by(filter: (TimeSeries) -> Boolean) =
            flatData.filter(filter)

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

    /** Query for weekly percent change version of time series. */
    open fun weeklyPercentChange(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for weekly average difference version of time series. */
    open fun weeklyAverageDifference(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for weekly total difference version of time series. */
    open fun weeklyTotalDifference(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for weekly cumulative percent change version of time series. */
    open fun weeklyCumulativePercentChange(area: AreaInfo, metric: String): TimeSeries? = null

    /** Query for 14-day average version of time series. */
    open fun biweeklyAverage(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for 14-day total version of time series. */
    open fun biweeklyTotal(area: AreaInfo, metric: String): TimeSeries? = null

    /** Query for monthly average value. */
    open fun monthlyAverage(area: AreaInfo, metric: String, month: YearMonth): Double? = null
    /** Query for monthly total value. */
    open fun monthlyTotal(area: AreaInfo, metric: String, month: YearMonth): Double? = null

    //endregion

    //region UTILS

    /** Loads all data sources. */
    private fun allSources(lookup: AreaLookup) =
            sources.onEach { it.loadData(lookup) }

    private fun sourcesFor(area: AreaInfo, metric: String, qualifier: String = "") =
            sources.filter { it.provides(area, metric, qualifier) }

    /** Filter indicating whether the given data is provided by this processor. Override to limit areas. */
    private fun TimeSeriesProcessor.provides(area: AreaInfo, metric: String, qualifier: String) =
            MetricInfo(metric, qualifier) in metricsProvided()

    private fun sourcesFor(metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean) =
            sources.filter {
                it.metricsProvided().any(metricInfoFilter(metricFilter, qualifierFilter))
            }

    private fun metricInfoFilter(metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean): (MetricInfo) -> Boolean =
        { metricFilter(it.metric) && qualifierFilter(it.qualifier) }

    private fun TimeSeriesProcessor.query(area: AreaInfo, metric: String, qualifier: String? = ""): List<TimeSeries> {
        if (this !in sourceData.keys) loadData(lookup)
        return sourceData[this]!!.getOrElse(area) { listOf() }
            .filter { (it.metric == metric) && (qualifier == null || it.qualifier == qualifier) }
    }

    private fun TimeSeriesProcessor.query(areaFilter: (AreaInfo) -> Boolean, metricFilter: (String) -> Boolean, qualifierFilter: (String) -> Boolean): List<TimeSeries> {
        if (this !in sourceData.keys) loadData(lookup)
        val data = sourceData[this]!!
        val filter = metricInfoFilter(metricFilter, qualifierFilter)
        return data.filterKeys(areaFilter).flatMap { it.value.filter { filter(it.metricInfo) } }
    }

    private fun TimeSeriesProcessor.loadData(lookup: AreaLookup) {
        if (!sourceData.containsKey(this))
            sourceData[this] = data().groupByArea(lookup)
    }

    //endregion

    /** Organize by area, using first series for each area. */
    private fun Collection<TimeSeries>.byArea(lookup: AreaLookup) =
            associate { it.area(lookup) to it }

    /** Group by area, filtering out invalid areas. */
    private fun Collection<TimeSeries>.groupByArea(lookup: AreaLookup) =
            filter { lookup.areaOrNull(it.areaId) != null }.groupBy { it.area(lookup) }

    /** Get area by id, if found. */
    private fun TimeSeries.area(lookup: AreaLookup) =
            lookup.areaOrNull(areaId)
                    ?: throw IllegalStateException("Area not found: $areaId")
}
