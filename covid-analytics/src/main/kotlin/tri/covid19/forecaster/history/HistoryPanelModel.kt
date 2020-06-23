package tri.covid19.forecaster.history

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getProperty
import tornadofx.property
import tri.covid19.ACTIVE
import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.RECOVERED
import tri.covid19.forecaster.utils.ChartDataSeries
import tri.covid19.forecaster.utils.series
import tri.timeseries.MetricTimeSeries
import tri.timeseries.dateRange
import tri.timeseries.deltas
import tri.timeseries.movingAverage
import tri.util.DateRange
import tri.util.javaTrim
import triathematician.covid19.CovidTimeSeriesSources.countryData
import triathematician.covid19.CovidTimeSeriesSources.usCbsaData
import triathematician.covid19.CovidTimeSeriesSources.usCountyData
import triathematician.covid19.CovidTimeSeriesSources.usStateData
import triathematician.covid19.perCapita
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
    var minPopulation by property(10000)

    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> onChange() } }
    val includeRegionActive = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }
    val excludeRegionActive = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }
    val parentRegion = SimpleStringProperty("").apply { addListener { _ -> onChange() } }
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
    val _minPopulation = property(HistoryPanelModel::minPopulation)
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
    internal fun historicalData(metric: String? = null): List<MetricTimeSeries> {
        if (metric == null) {
            val sMetrics = data()
                    .asSequence()
                    .filter { parentRegion.value.isEmpty() || it.region.parent == parentRegion.value }
                    .filter { it.metric == if (perCapita) selectedMetric.perCapita else selectedMetric }
                    .filter { it.region.population.let { it == null || it >= minPopulation } }
                    .filter { exclude(it.region.id) }
                    .sortedByDescending { it.sortMetric }
                    .toList()
            return (sMetrics.filter { include(it.region.id) } + sMetrics).take(regionLimit)
        } else {
            val regions = historicalData(null).map { it.region.id }
            return data().filter { it.metric == if (perCapita) metric.perCapita else metric }
                .filter { it.region.id in regions }
                .sortedBy { regions.indexOf(it.region.id) }
        }
    }

    private val MetricTimeSeries.sortMetric
        get() = when(sort) {
            TimeSeriesSort.ALL -> lastValue
            TimeSeriesSort.LAST14 -> lastValue - values.getOrElse(values.size - 14) { 0.0 }
            TimeSeriesSort.LAST7 -> lastValue - values.getOrElse(values.size - 7) { 0.0 }
            TimeSeriesSort.POPULATION -> region.population?.toDouble() ?: 0.0
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
    internal val List<MetricTimeSeries>.smoothed: List<MetricTimeSeries>
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
        return domain to metrics.map { series(it.region.id, domain, it) }
    }

    /** Plot growth vs counts. */
    internal fun hubbertDataSeries() = smoothedData().map { it.hubbertSeries(1) }
                .map { series(it.first.region.id, it.first.domain.shift(1, 0), it.first, it.second) }

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
fun MetricTimeSeries.hubbertSeries(window: Int): Pair<MetricTimeSeries, MetricTimeSeries> {
    val totals = movingAverage(window).restrictNumberOfStartingZerosTo(0)
    val growths = totals.growthPercentages()
    return totals to growths
}

/** Creates doubling-change series from monotonic metric. */
fun MetricTimeSeries.changeDoublingDataSeries(window: Int): Pair<MetricTimeSeries, MetricTimeSeries> {
    return movingAverage(window).doublingTimes() to movingAverage(window).deltas()
}

/** Creates total-doubling series from monotonic metric. */
fun MetricTimeSeries.doublingTotalDataSeries(window: Int): Pair<MetricTimeSeries, MetricTimeSeries> {
    return movingAverage(window) to movingAverage(window).doublingTimes()
}