package tri.covid19.forecaster

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getProperty
import tornadofx.property
import tri.covid19.*
import tri.covid19.forecaster.utils.ChartDataSeries
import tri.covid19.forecaster.utils.series
import tri.timeseries.MetricTimeSeries
import tri.timeseries.dateRange
import tri.util.DateRange
import tri.util.javaTrim
import triathematician.covid19.CovidTimeSeriesSources.countryData
import triathematician.covid19.CovidTimeSeriesSources.usCbsaData
import triathematician.covid19.CovidTimeSeriesSources.usCountyData
import triathematician.covid19.CovidTimeSeriesSources.usStateData
import triathematician.covid19.perCapita
import java.lang.IllegalStateException
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
    var minPopulation by property(100000)

    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> onChange() } }
    val includeRegionActive = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }
    val excludeRegionActive = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }
    val includeRegion = SimpleStringProperty("").apply { addListener { _ -> if (includeRegionActive.get()) onChange() } }
    val excludeRegion = SimpleStringProperty("").apply { addListener { _ -> if (excludeRegionActive.get()) onChange() } }

    var selectedMetric by property(METRIC_OPTIONS[0])
    val mainPlotMetric: String
        get() = if (perCapita) selectedMetric.perCapita else selectedMetric
    var perDay by property(false)
    var perCapita by property(false)
    var logScale by property(false)
    var bucket by property(7)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> onChange() } }

    val _regionLimit = property(HistoryPanelModel::regionLimit)
    val _minPopulation = property(HistoryPanelModel::minPopulation)
    val _selectedMetric = property(HistoryPanelModel::selectedMetric)
    val _perCapita = property(HistoryPanelModel::perCapita)
    val _perDay = property(HistoryPanelModel::perDay)
    val _bucket = property(HistoryPanelModel::bucket)

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
    internal fun historicalData(): Set<MetricTimeSeries> {
        val sMetrics = data().filter { it.metric == mainPlotMetric }
                .filter { it.region.population.let { it == null || it >= minPopulation } }
                .filter { exclude(it.region.id) }
                .sortedByDescending { it.lastValue }
        return (sMetrics.filter { include(it.region.id) } + sMetrics).take(regionLimit).toSet()
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

    /** Plot counts by date. */
    internal fun historicalDataSeries(): Pair<DateRange, List<ChartDataSeries>> {
        var metrics = historicalData()
        if (bucket != 1) {
            metrics = metrics.map { it.movingAverage(bucket, false) }.toSet()
        }
        if (perDay) {
            metrics = metrics.map { it.deltas() }.toSet()
        }
        val domain = metrics.dateRange ?: DateRange(LocalDate.now(), LocalDate.now())
        return domain to metrics.map { series(it.region.id, domain, it) }
    }

    /** Plot growth vs counts. */
    internal fun hubbertDataSeries() = historicalData().map { it.hubbertSeries(7) }
            .map { series(it.first.region.id, it.first.domain.shift(1, 0), it.first, it.second) }

    //endregion
}

/** Creates Hubbert series from monotonic metric. */
fun MetricTimeSeries.hubbertSeries(window: Int): Pair<MetricTimeSeries, MetricTimeSeries> {
    val totals = movingAverage(window).restrictNumberOfStartingZerosTo(0)
    val growths = totals.growthPercentages()
    return totals to growths
}

/** Creates Hubbert series from monotonic metric. */
fun MetricTimeSeries.changeDoublingDataSeries(window: Int): Pair<MetricTimeSeries, MetricTimeSeries> {
    return movingAverage(window).doublingTimes().movingAverage(window) to movingAverage(window).deltas()
}