/*-
 * #%L
 * coda-app
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
package tri.covid19.coda.history

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getProperty
import tornadofx.property
import tri.area.Lookup
import tri.covid19.ACTIVE
import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.RECOVERED
import tri.covid19.coda.utils.ChartDataSeries
import tri.covid19.coda.utils.series
import tri.timeseries.TimeSeries
import tri.timeseries.dateRange
import tri.timeseries.deltas
import tri.timeseries.movingAverage
import tri.util.DateRange
import tri.util.javaTrim
import tri.covid19.data.CovidTimeSeriesSources.countryData
import tri.covid19.data.CovidTimeSeriesSources.usCbsaData
import tri.covid19.data.CovidTimeSeriesSources.usCountyData
import tri.covid19.data.CovidTimeSeriesSources.usStateData
import tri.covid19.data.perCapita
import java.time.LocalDate
import kotlin.reflect.KMutableProperty1
import kotlin.time.ExperimentalTime

const val COUNTRIES = "Countries and Global Regions"
const val STATES = "US States and Territories"
const val COUNTIES = "US Counties"
const val CBSA = "US CBSA"
val METRIC_OPTIONS = listOf(CASES, DEATHS, RECOVERED, ACTIVE)

/** UI model for history panel. */
@ExperimentalTime
class HistoryPanelModel(var onChange: () -> Unit = {}) {

    val regionTypes = listOf(COUNTRIES, STATES, COUNTIES, CBSA)
    var regionLimit by property(10)
    var skipFirst by property(0)
    var minPopulation by property(10000)
    var maxPopulation by property(Int.MAX_VALUE)

    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> onChange() } }
    val includeRegionActive = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }
    val excludeRegionActive = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }
    val parentRegion = SimpleStringProperty("USA").apply { addListener { _ -> onChange() } }
    val includeRegion = SimpleStringProperty("").apply { addListener { _ -> if (includeRegionActive.get()) onChange() } }
    val excludeRegion = SimpleStringProperty("").apply { addListener { _ -> if (excludeRegionActive.get()) onChange() } }

    var selectedMetric by property(METRIC_OPTIONS[0])
    var perDay by property(false)
    var perCapita by property(false)
    var logScale by property(false)
    var smooth by property(7)
    var extraSmooth by property(false)
    var sort by property(TimeSeriesSort.ALL)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> onChange() } }

    val _regionLimit = property(HistoryPanelModel::regionLimit)
    val _skipFirst = property(HistoryPanelModel::skipFirst)
    val _minPopulation = property(HistoryPanelModel::minPopulation)
    val _maxPopulation = property(HistoryPanelModel::maxPopulation)
    val _selectedMetric = property(HistoryPanelModel::selectedMetric)

    val _perCapita = property(HistoryPanelModel::perCapita)
    val _perDay = property(HistoryPanelModel::perDay)
    val _smooth = property(HistoryPanelModel::smooth)
    val _extraSmooth = property(HistoryPanelModel::extraSmooth)
    val _sort = property(HistoryPanelModel::sort)

    val _logScale = property(HistoryPanelModel::logScale)

    //endregion

    //region INCLUDE/EXCLUDE

    /** Test when a region should be included in the plot. */
    fun include(region: String) = when (includeRegionActive.get()) {
        true -> includeRegion.get().filterOptions.any { it in region.toLowerCase() }
        else -> false
    }

    /** Test when a region should be excluded from the plot. */
    fun exclude(region: String) = when (excludeRegionActive.get()) {
        true -> excludeRegion.get().filterOptions.none { it in region.toLowerCase() }
        else -> true
    }

    private val String.filterOptions: List<String>
        get() = split(",").filter { it.isNotEmpty() }.map { it.javaTrim().toLowerCase() }

    //endregion

    //region DATA

    /** Get historical data for current config. Matching "includes" are first. */
    internal fun historicalData(metric: String? = null): List<TimeSeries> {
        if (metric == null) {
            val sMetrics = data()
                    .asSequence()
                    .filter { parentRegion.value == null || it.area.parent == Lookup.area(parentRegion.value) }
                    .filter { it.metric == if (perCapita) selectedMetric.perCapita else selectedMetric }
                    .filter { it.area.population.let { it == null || it in minPopulation..maxPopulation } }
                    .filter { exclude(it.areaId) }
                    .sortedByDescending { it.sortMetric }
                    .toList()
            return (sMetrics.filter { include(it.areaId) } + sMetrics).take(regionLimit + skipFirst).drop(skipFirst)
        } else {
            val regions = historicalData(null).map { it.areaId }
            return data().filter { it.metric == if (perCapita) metric.perCapita else metric }
                .filter { it.areaId in regions }
                .sortedBy { regions.indexOf(it.areaId) }
        }
    }

    private val TimeSeries.sortMetric
        get() = when(sort) {
            TimeSeriesSort.ALL -> lastValue
            TimeSeriesSort.LAST14 -> lastValue - values.getOrElse(values.size - 14) { 0.0 }
            TimeSeriesSort.LAST7 -> lastValue - values.getOrElse(values.size - 7) { 0.0 }
            TimeSeriesSort.POPULATION -> area.population?.toDouble() ?: 0.0
            TimeSeriesSort.PEAK7 -> values.deltas().movingAverage(7).max() ?: 0.0
            TimeSeriesSort.PEAK14 -> values.deltas().movingAverage(14).max() ?: 0.0
        }

    internal fun data() = when (selectedRegionType.get()) {
        COUNTRIES -> countryData(includeGlobal = true)
        STATES -> usStateData(includeUS = true)
        COUNTIES -> usCountyData()
        CBSA -> usCbsaData()
        else -> throw IllegalStateException()
    }

    //endregion

    //region

    /** Smooth using current settings. */
    internal val List<TimeSeries>.smoothed: List<TimeSeries>
        get() {
            var res = this
            if (smooth != 1) {
                res = res.map { it.movingAverage(smooth, false) }
                if (extraSmooth) {
                    res = res.map { it.movingAverage(3, false).movingAverage(3, false) }
                }
            }
            return res
        }

    /** Get smoothed version of data. */
    internal fun smoothedData(metric: String? = null) = historicalData(metric).smoothed

    /** Plot counts by date. */
    internal fun historicalDataSeries(): Pair<DateRange, List<ChartDataSeries>> {
        var metrics = smoothedData()
        if (perDay) {
            metrics = metrics.map { it.deltas() }
        }
        val domain = metrics.dateRange ?: DateRange(LocalDate.now(), LocalDate.now())
        return domain to metrics.map { series(it.areaId, domain, it) }
    }

    /** Plot growth vs counts. */
    internal fun hubbertDataSeries() = smoothedData().map { it.hubbertSeries(1) }
                .map { series(it.first.areaId, it.first.domain.shift(1, 0), it.first, it.second) }

    //endregion
}

/** Options for sorting time series retrieved for history panel. */
enum class TimeSeriesSort {
    ALL,
    LAST7,
    LAST14,
    PEAK7,
    PEAK14,
    POPULATION
}

/** Creates Hubbert series from monotonic metric. */
fun TimeSeries.hubbertSeries(window: Int): Pair<TimeSeries, TimeSeries> {
    val totals = movingAverage(window).restrictNumberOfStartingZerosTo(0)
    val growths = totals.growthPercentages()
    return totals to growths
}

/** Creates doubling-change series from monotonic metric. */
fun TimeSeries.changeDoublingDataSeries(window: Int): Pair<TimeSeries, TimeSeries> {
    return movingAverage(window).doublingTimes() to movingAverage(window).deltas()
}

/** Creates total-doubling series from monotonic metric. */
fun TimeSeries.doublingTotalDataSeries(window: Int): Pair<TimeSeries, TimeSeries> {
    return movingAverage(window) to movingAverage(window).doublingTimes()
}
