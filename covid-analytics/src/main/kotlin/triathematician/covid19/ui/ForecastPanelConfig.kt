package triathematician.covid19.ui

import javafx.beans.property.SimpleStringProperty
import org.apache.commons.math3.exception.NoBracketingException
import tornadofx.getProperty
import tornadofx.property
import triathematician.covid19.CovidTimeSeriesSources
import triathematician.covid19.DEATHS
import triathematician.covid19.sources.IhmeProjections
import triathematician.timeseries.MetricTimeSeries
import triathematician.timeseries.dateRange
import triathematician.util.DateRange
import triathematician.util.minus
import java.time.LocalDate
import kotlin.reflect.KMutableProperty1

/** Config for logistic projection. */
class ForecastPanelConfig(var onChange: () -> Unit = {}) {

    // region and metrics
    internal var region by property("US")
    internal var selectedMetric by property(METRIC_OPTIONS[0])

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
    internal val _showCu80 = property(ForecastPanelConfig::showCu80)
    internal val _showIhme = property(ForecastPanelConfig::showIhme)
    internal val _showLanl = property(ForecastPanelConfig::showLanl)
    internal val _showMobs = property(ForecastPanelConfig::showMobs)
    internal val _showUt = property(ForecastPanelConfig::showUt)

    internal val _showForecast = property(ForecastPanelConfig::showForecast)
    internal val _vActive = property(ForecastPanelConfig::vActive)

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

    //region DATA SERIES

    /** All time series data for the region and selected metric. */
    var regionMetrics = listOf<MetricTimeSeries>()
    /** The primary time series for the selected metric. */
    var mainSeries: MetricTimeSeries? = null
    /** User's projection. */
    var userForecast: MetricTimeSeries? = null
    /** Other forecasts. */
    var externalForecasts = listOf<MetricTimeSeries>()
    /** Domain for raw data. */
    var domain: DateRange? = null
    /** Domain for projection data. */
    var ihmeDomain: DateRange? = null
    /** Domain for plotted data. */
    var totalDomain: DateRange? = null

    private fun updateData() {
        regionMetrics = CovidTimeSeriesSources.dailyReports({ it == region }).filter { selectedMetric in it.metric }
        mainSeries = regionMetrics.firstOrNull { it.metric == selectedMetric }
        externalForecasts = IhmeProjections.allProjections.filter { it.id == region }

        domain = mainSeries?.dateRange
        ihmeDomain = externalForecasts.dateRange
        totalDomain = domain?.let { DateRange(it.start, it.endInclusive.plusDays(30)) }

        userForecast = if (!showForecast) null else totalDomain?.let { forecastTimeSeries(it, it.start) }
    }

    internal fun cumulativeDataSeries() = listOfNotNull(series(selectedMetric, domain, mainSeries), series(userForecast?.metric, totalDomain, userForecast)) +
            regionMetrics.filter { "predicted" in it.metric && "peak" !in it.metric }.mapNotNull { series(it.metric, domain, it) } +
            externalForecasts.filter { showIhme && selectedMetric == DEATHS && "change" !in it.metric }.mapNotNull { series(it.metric, totalDomain, ihmeDomain, it) }

    internal fun dailyDataSeries() = listOfNotNull(series(mainSeries?.metric, domain, mainSeries?.deltas()), series(userForecast?.metric, totalDomain, userForecast?.deltas())) +
            regionMetrics.filter { "predicted peak" in it.id }.mapNotNull { series(it.metric, domain, it) } +
            externalForecasts.filter { showIhme && "change" in it.metric }.mapNotNull { series(it.metric, totalDomain, ihmeDomain, it) }

    internal fun hubbertDataSeries() = listOfNotNull(mainSeries?.hubbertSeries(7)).mapNotNull { series(it.first.metric, domain?.shift(1, 0), it.first, it.second) } +
            listOfNotNull(userForecast?.hubbertSeries(1)).mapNotNull { series(it.first.metric, totalDomain?.shift(1, 0), it.first, it.second) } +
            externalForecasts.filter { showIhme && selectedMetric == DEATHS && "change" !in it.metric }.map { it.hubbertSeries(1) }
                    .mapNotNull { series(it.first.metric, totalDomain, ihmeDomain?.shift(1, 0), it.first, it.second) }

    internal fun peakDataSeries() = regionMetrics.filter { "days" in it.id }.mapNotNull { series(it.metric, domain, it) }

    //endregion

    //region FORECAST CURVE

    /** Generate time series function using given domain. */
    fun forecastTimeSeries(domain: DateRange, zeroDay: LocalDate) = MetricTimeSeries(id = region, metric = "$selectedMetric (curve)",
            intSeries = false, start = zeroDay, values = domain.map { it.minus(zeroDay) }.map { curveFitter(it) })

    /** Updates equation label. */
    private fun updateEquation() {
        _manualEquation.value = curveFitter.equation
        _manualPeak.value = try {
            val (x, y) = curveFitter.equationPeak()
            String.format("%.2f", y) + " on day " + String.format("%.1f", x)
        } catch (x: NoBracketingException) {
            ""
        }

        curveFitter.mainSeries = mainSeries
        _manualLogCumStdErr.value = "SE = ${curveFitter.calcCumulativeSE} (totals, last 3 weeks)"
        _manualDeltaStdErr.value = "SE = ${curveFitter.calcDeltaSE} (per day)"
    }

    /** Runs autofit using current config. */
    fun autofit() {
        mainSeries?.let {
            curveFitter.autofitCumulativeSE(it)
        }
    }

    //endregion

}