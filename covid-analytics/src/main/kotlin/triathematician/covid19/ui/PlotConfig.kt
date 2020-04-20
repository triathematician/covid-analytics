package triathematician.covid19.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getProperty
import tornadofx.property
import triathematician.covid19.*
import triathematician.covid19.countryData
import triathematician.covid19.usCountyData
import triathematician.covid19.usStateData
import triathematician.population.lookupPopulation
import triathematician.timeseries.MetricTimeSeries
import java.lang.IllegalStateException
import kotlin.reflect.KMutableProperty1

const val COUNTRIES = "Countries and Global Regions"
const val STATES = "US States and Territories"
const val COUNTIES = "US Counties"
val METRIC_OPTIONS = listOf(CASES, DEATHS, RECOVERED, ACTIVE)

/** Config for both plots. */
class PlotConfig(var onChange: () -> Unit = {}) {
    val regionTypes = listOf(COUNTRIES, STATES, COUNTIES)
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
    var bucket by property(7)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> onChange() } }

    val regionLimitProperty = property(PlotConfig::regionLimit)
    val minPopulationProperty = property(PlotConfig::minPopulation)
    val selectedMetricProperty = property(PlotConfig::selectedMetric)
    val perCapitaProperty = property(PlotConfig::perCapita)
    val perDayProperty = property(PlotConfig::perDay)
    val bucketProperty = property(PlotConfig::bucket)

    //endregion

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
        get() = split(",", " ").filter { it.isNotEmpty() }.map { it.toLowerCase() }

    /** Get historical data for current config. Matching "includes" are first. */
    internal fun historicalData(): Set<MetricTimeSeries> {
        val sMetrics = data().filter { it.metric == mainPlotMetric }
                .filter { lookupPopulation(it.id).let { it != null && it >= minPopulation } }
                .filter { exclude(it.id) }
                .sortedByDescending { it.lastValue }
        return (sMetrics.filter { include(it.id) } + sMetrics).take(regionLimit).toSet()
    }

    internal fun data() = when (selectedRegionType.get()) {
        COUNTRIES -> countryData(includeGlobal = false)
        STATES -> usStateData(includeUS = false)
        COUNTIES -> usCountyData()
        else -> throw IllegalStateException()
    }
}