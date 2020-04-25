package triathematician.covid19.ui

import javafx.beans.property.SimpleStringProperty
import org.apache.commons.math3.exception.NoBracketingException
import tornadofx.getProperty
import tornadofx.property
import triathematician.covid19.CovidTimeSeriesSources
import triathematician.covid19.data.forecasts.CovidForecasts
import triathematician.covid19.data.forecasts.Forecast
import triathematician.covid19.data.forecasts.IHME
import triathematician.covid19.data.forecasts.LANL
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.DateRange
import triathematician.util.userFormat
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KMutableProperty1

/** Config for logistic projection. */
class ForecastPanelConfig(var onChange: () -> Unit = {}) {

    // region and metrics
    internal var region by property("US")
    internal var selectedMetric by property(METRIC_OPTIONS[0])
    internal var smooth by property(true)

    // user forecast
    internal var showForecast by property(true)
    internal val curveFitter = ForecastCurveFitter()
    private var vActive by property(false)

    internal val _manualEquation = SimpleStringProperty(curveFitter.equation)
    internal val _manualPeak = SimpleStringProperty("")
    internal val _manualLogCumStdErr = SimpleStringProperty("")
    internal val _manualDeltaStdErr = SimpleStringProperty("")

    // other forecasts
    internal var showCu80 by property(false)
    internal var showIhme by property(true)
    internal var showLanl by property(false)
    internal var showMobs by property(false)
    internal var showUt by property(false)

    // historical forecasts
    private var movingAverage by property(4)
    private var projectionDays by property(10)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> updateData(); onChange() } }
    private fun <T> forecastProperty(prop: KMutableProperty1<*, T>) = curveFitter.getProperty(prop).apply {
        addListener { _ ->
            updateData()
            updateEquation()
            vActive = curveFitter.curve == GEN_LOGISTIC
            onChange()
        }
    }

    internal val _region = property(ForecastPanelConfig::region)
    internal val _selectedRegion = property(ForecastPanelConfig::selectedMetric)
    internal val _smooth = property(ForecastPanelConfig::smooth)

    internal val _showCu80 = property(ForecastPanelConfig::showCu80)
    internal val _showIhme = property(ForecastPanelConfig::showIhme)
    internal val _showLanl = property(ForecastPanelConfig::showLanl)
    internal val _showMobs = property(ForecastPanelConfig::showMobs)
    internal val _showUt = property(ForecastPanelConfig::showUt)

    internal val _showForecast = property(ForecastPanelConfig::showForecast)
    internal val _vActive = property(ForecastPanelConfig::vActive)

    internal val _fitLabel = forecastProperty(ForecastCurveFitter::fitLabel)
    internal val _curve = forecastProperty(ForecastCurveFitter::curve)
    internal val _l = forecastProperty(ForecastCurveFitter::l)
    internal val _k = forecastProperty(ForecastCurveFitter::k)
    internal val _x0 = forecastProperty(ForecastCurveFitter::x0)
    internal val _v = forecastProperty(ForecastCurveFitter::v)

    internal val _autofitDay0 = forecastProperty(ForecastCurveFitter::day0).apply { addListener { _ -> autofit() }}
    internal val _autofitDays = forecastProperty(ForecastCurveFitter::days).apply { addListener { _ -> autofit() }}

    internal val _movingAverage = property(ForecastPanelConfig::movingAverage)
    internal val _projectionDays = property(ForecastPanelConfig::projectionDays)

    //endregion

    //region DATA FOR PROJECTION PLOT

    /** List of regions available for panel. */
    val regions: SortedSet<String> by lazy {
        val jhuRegions = CovidTimeSeriesSources.dailyReports().map { it.id }.toSet()
        val forecastRegions = CovidForecasts.allForecasts.map { it.region }.toSet()
        (jhuRegions + forecastRegions).toSortedSet()
    }

    /** Domain for raw data. */
    var domain: DateRange? = null

    /** The primary time series for the selected metric. */
    var mainSeries: MetricTimeSeries? = null
    /** User's projection. */
    var userForecast: MetricTimeSeries? = null

    /** Past forecasts. */
    var pastForecasts = PastForecasts()
    /** Other forecasts. */
    var externalForecasts = ExternalForecasts()

    private fun updateData() {
        val regionMetrics = CovidTimeSeriesSources.dailyReports(region, selectedMetric)
        mainSeries = regionMetrics.firstOrNull { it.metric == selectedMetric }?.restrictNumberOfStartingZerosTo(0)

        domain = mainSeries?.domain?.shift(0, 30)

        val shift = if (smooth) -3.5 else 0.0
        userForecast = when {
            !showForecast -> null
            domain == null -> null
            else -> MetricTimeSeries(region, "", "$selectedMetric (curve)", false, 0.0, domain!!.start,
                    domain!!.mapIndexed { i, _ -> curveFitter(i + shift) })
        }

        pastForecasts.metrics = regionMetrics.filter { "predicted" in it.metric || "peak" in it.metric }
        externalForecasts.forecasts = CovidForecasts.allForecasts.filter { it.region == region && it.metric == selectedMetric }
    }

    /** Get day associated with given number. */
    fun dayOf(x: Number) = domain?.start?.plusDays(x.toLong())

    //endregion

    //region SERIES BUILDERS

    internal fun cumulativeDataSeries() = dataseries {
        series(mainSeries?.maybeSmoothed())
        series(userForecast)
        series(pastForecasts.cumulative)
        series(externalForecasts.cumulative)
    }

    internal fun dailyDataSeries() = dataseries {
        series(mainSeries?.deltas()?.maybeSmoothed())
        series(userForecast?.deltas())
        series(pastForecasts.deltas)
        series(externalForecasts.cumulative.map { it.deltas() })
    }

    internal fun hubbertDataSeries() = dataseries {
        series(mainSeries?.hubbertSeries(7))
        series(userForecast?.hubbertSeries(1))
        series(externalForecasts.cumulative.map { it.hubbertSeries(1) })
    }

    internal fun peakDataSeries() = dataseries {
        series(pastForecasts.peakDays)
    }

    private fun dataseries(op: MutableList<ChartDataSeries>.() -> Unit) = mutableListOf<ChartDataSeries>().apply { op() }
    private fun MutableList<ChartDataSeries>.series(s: MetricTimeSeries?) { series(listOfNotNull(s)) }
    private fun MutableList<ChartDataSeries>.series(ss: List<MetricTimeSeries>) {
        domain?.let { domain -> ss.forEach { this += series(it.metric, domain, it) } }
    }
    private fun MutableList<ChartDataSeries>.series(xy: Pair<MetricTimeSeries, MetricTimeSeries>?, idFirst: Boolean = true) { series(listOfNotNull(xy), idFirst) }
    private fun MutableList<ChartDataSeries>.series(xyxy: List<Pair<MetricTimeSeries, MetricTimeSeries>>, idFirst: Boolean = true) {
        domain?.let { domain -> xyxy.forEach { this += series(if (idFirst) it.first.metric else it.second.metric, domain, it.first, it.second) } }
    }

    private fun MetricTimeSeries.maybeSmoothed() = if (smooth) movingAverage(7) else this

    //endregion

    //region FORECAST CURVE

    /** Updates equation label whenever it changes. */
    private fun updateEquation() {
        _manualEquation.value = curveFitter.equation
        _manualPeak.value = try {
            val (x, y) = curveFitter.equationPeak()
            "${y.userFormat()} on day ${dayOf(x) ?: "?"}"
        } catch (x: NoBracketingException) {
            ""
        }

        val se1 = curveFitter.cumulativeStandardError(mainSeries, 0.0)
        val se2 = curveFitter.deltaStandardError(mainSeries, 0.0)

        _manualLogCumStdErr.value = "SE = ${se1?.userFormat() ?: "?"} (totals)"
        _manualDeltaStdErr.value = "SE = ${se2?.userFormat() ?: "?"} (per day)"
    }

    /** Runs autofit using current config. */
    fun autofit() {
        curveFitter.updateFitLabel(mainSeries?.end ?: LocalDate.now())
        mainSeries?.let {
            curveFitter.autofitCumulativeSE(it)
        }
    }

    //endregion

    //region DATA MANAGEMENT

    /** Provides access to past forecasts. */
    class PastForecasts(var metrics: List<MetricTimeSeries> = listOf())
    /** Provides access to external forecasts. */
    class ExternalForecasts(var forecasts: List<Forecast> = listOf())

    val PastForecasts.cumulative
        get() = metrics.filter { "predicted" in it.metric && "peak" !in it.metric }
    val PastForecasts.deltas
        get() = metrics.filter { "predicted peak" in it.id }
    val PastForecasts.peakDays
        get() = metrics.filter { "days" in it.id }

    val ExternalForecasts.filtered
        get() = forecasts.filter { (showIhme && it.model == IHME || showLanl && it.model == LANL) }.flatMap { it.data }
    val ExternalForecasts.cumulative
        get() = filtered.filter { "change" !in it.metric }
    val ExternalForecasts.deltas
        get() = filtered.filter { "change" in it.metric }

    //endregion

}

