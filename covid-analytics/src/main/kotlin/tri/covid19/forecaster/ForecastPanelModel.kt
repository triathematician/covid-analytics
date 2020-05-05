package tri.covid19.forecaster

import javafx.beans.property.SimpleStringProperty
import org.apache.commons.math3.exception.NoBracketingException
import tornadofx.asObservable
import tornadofx.getProperty
import tornadofx.property
import tri.covid19.data.*
import tri.timeseries.Forecast
import tri.covid19.forecaster.utils.ChartDataSeries
import tri.math.GEN_LOGISTIC
import tri.regions.RegionLookup
import tri.regions.UnitedStates
import tri.timeseries.MetricTimeSeries
import tri.util.DateRange
import tri.util.minus
import tri.util.userFormat
import triathematician.covid19.CovidTimeSeriesSources
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.time.ExperimentalTime

/** Config for logistic projection. */
@ExperimentalTime
class ForecastPanelModel(var onChange: () -> Unit = {}) {

    //region UI BOUND PROPERTIES

    // metric selection
    internal var region by property("US")
    internal var selectedMetric by property(METRIC_OPTIONS[0])
    internal var smooth by property(true)

    // user forecast
    internal val curveFitter = ForecastCurveFitter()
    internal val userForecasts = mutableListOf<UserForecast>().asObservable()

    internal var showForecast by property(true)
    private var vActive by property(false)

    internal val _manualEquation = SimpleStringProperty(curveFitter.equation)
    internal val _manualPeak = SimpleStringProperty("")
    internal val _manualLogCumStdErr = SimpleStringProperty("")
    internal val _manualDeltaStdErr = SimpleStringProperty("")

    // other forecasts
    internal var showIhme by property(true)
    internal var showLanl by property(false)
    internal var showYyg by property(false)

    // historical forecasts
    private var movingAverage by property(4)
    private var projectionDays by property(10)

    //endregion

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

    internal val _region = property(ForecastPanelModel::region)
    internal val _selectedMetric = property(ForecastPanelModel::selectedMetric)
    internal val _smooth = property(ForecastPanelModel::smooth)

    internal val _showIhme = property(ForecastPanelModel::showIhme)
    internal val _showLanl = property(ForecastPanelModel::showLanl)
    internal val _showYyg = property(ForecastPanelModel::showYyg)

    internal val _showForecast = property(ForecastPanelModel::showForecast)
    internal val _vActive = property(ForecastPanelModel::vActive)

    internal val _fitLabel = forecastProperty(ForecastCurveFitter::fitLabel)
    internal val _curve = forecastProperty(ForecastCurveFitter::curve)
    internal val _l = forecastProperty(ForecastCurveFitter::l)
    internal val _k = forecastProperty(ForecastCurveFitter::k)
    internal val _x0 = forecastProperty(ForecastCurveFitter::x0)
    internal val _v = forecastProperty(ForecastCurveFitter::v)

    internal val _autofitLastDay = forecastProperty(ForecastCurveFitter::lastFitDay).apply { addListener { _ -> autofit() }}
    internal val _autofitDays = forecastProperty(ForecastCurveFitter::fitDays).apply { addListener { _ -> autofit() }}

    internal val _movingAverage = property(ForecastPanelModel::movingAverage)
    internal val _projectionDays = property(ForecastPanelModel::projectionDays)

    //endregion

    //region DATA FOR PROJECTION PLOT

    /** List of regions available for panel. */
    val regions: SortedSet<String> by lazy {
        val jhuRegions = CovidHistory.allData.map { it.region.id }.toSet()
        val forecastRegions = CovidForecasts.allForecasts.map { it.region.id }.toSet()
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
        val regionMetrics = CovidTimeSeriesSources.dailyReports(RegionLookup(region), selectedMetric)
        mainSeries = regionMetrics.firstOrNull { it.metric == selectedMetric }?.restrictNumberOfStartingZerosTo(0)

        domain = mainSeries?.domain?.shift(0, 30)

        val shift = if (smooth) -3.5 else 0.0
        userForecast = when {
            !showForecast -> null
            domain == null -> null
            else -> MetricTimeSeries(RegionLookup(region), "$selectedMetric (curve)", false, 0.0, domain!!.start,
                    domain!!.mapIndexed { i, _ -> curveFitter(i + shift) })
        }

        pastForecasts.metrics = regionMetrics.filter { "predicted" in it.metric || "peak" in it.metric }
        externalForecasts.forecasts = CovidForecasts.allForecasts.filter { it.region.id == region && it.metric == selectedMetric }
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

    internal fun residualDataSeries() = dataseries {
        val daily = mainSeries?.deltas()?.maybeSmoothed()
        series(userForecast?.deltas()?.residuals(daily))
        series(externalForecasts.deltas.mapNotNull { it.residuals(daily) })
    }

    private fun MetricTimeSeries.residuals(empirical: MetricTimeSeries?): MetricTimeSeries? {
        empirical ?: return null
        val commonDomain = domain.intersect(empirical.domain)
        commonDomain ?: return null
        return copy(start = commonDomain.start, values = commonDomain.map { empirical[it] - get(it) })
    }

    private fun dataseries(op: MutableList<ChartDataSeries>.() -> Unit) = mutableListOf<ChartDataSeries>().apply { op() }
    private fun MutableList<ChartDataSeries>.series(s: MetricTimeSeries?) { series(listOfNotNull(s)) }
    private fun MutableList<ChartDataSeries>.series(ss: List<MetricTimeSeries>) {
        domain?.let { domain -> ss.forEach { this += tri.covid19.forecaster.utils.series(it.metric, domain, it) } }
    }
    private fun MutableList<ChartDataSeries>.series(xy: Pair<MetricTimeSeries, MetricTimeSeries>?, idFirst: Boolean = true) { series(listOfNotNull(xy), idFirst) }
    private fun MutableList<ChartDataSeries>.series(xyxy: List<Pair<MetricTimeSeries, MetricTimeSeries>>, idFirst: Boolean = true) {
        domain?.let { domain -> xyxy.forEach { this += tri.covid19.forecaster.utils.series(if (idFirst) it.first.metric else it.second.metric, domain, it.first, it.second) } }
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

    //endregion

    //region ACTIONS

    /** Load the next US state in alphabetical order. */
    fun goToNextUsState() {
        val states = UnitedStates.stateNames.toSortedSet()
        region = when {
            states.contains(region) -> states.rollAfter(region)
            else -> states.first()
        }
        autofit()
    }

    /** Load the next US state in alphabetical order. */
    fun goToPreviousUsState() {
        val states = UnitedStates.stateNames.toSortedSet()
        region = when {
            states.contains(region) -> states.rollBefore(region)
            else -> states.last()
        }
        autofit()
    }

    private fun <X> SortedSet<X>.rollAfter(x: X) = tailSet(x).elementAtOrNull(1) ?: first()
    private fun <X> SortedSet<X>.rollBefore(x: X) = headSet(x).reversed().elementAtOrNull(1) ?: last()

    /** Runs autofit using current config. */
    fun autofit() {
        curveFitter.updateFitLabel(mainSeries?.end ?: LocalDate.now())
        mainSeries?.let {
            curveFitter.autofitCumulativeSE(it)
        }
    }

    /** Loads selected forecast. */
    fun load(f: UserForecast) {
        region = f.region.id
        selectedMetric = f.metric
        curveFitter.curve = f.sigmoidCurve
        curveFitter.l = f.sigmoidParameters?.load as Number
        curveFitter.k = f.sigmoidParameters?.k as Number
        curveFitter.x0 = f.sigmoidParameters?.x0 as Number
        curveFitter.v = f.sigmoidParameters?.v as Number

        f.fitDayRange?.run {
            curveFitter.lastFitDay = endInclusive.minus(LocalDate.now()) + 1
            curveFitter.fitDays = size
        }
    }

    /** Save current config as new forecast. */
    fun save() {
        val empirical = mainSeries
        val day0 = domain?.start
        if (empirical != null && day0 != null) {
            userForecasts.add(curveFitter.createUserForecast(day0, empirical))
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
        get() = metrics.filter { "predicted peak" in it.metric }
    val PastForecasts.peakDays
        get() = metrics.filter { "days" in it.metric }

    val ExternalForecasts.filtered
        get() = forecasts.filter { showIhme && it.model == IHME || showLanl && it.model == LANL || showYyg && it.model == YYG }
                .flatMap { it.data }
    val ExternalForecasts.cumulative
        get() = filtered.filter { "totdea_" in it.metric }.toMutableList() +
                filtered.filter { it.metric containsOneOf listOf("q.05", "q.50", "q.95")} +
                filtered.filter { "predicted_total_death" in it.metric }
    val ExternalForecasts.deltas
        get() = filtered.filter { "deaths_" in it.metric }.toMutableList() +
                filtered.filter { it.metric containsOneOf listOf("q.05", "q.50", "q.95") }.map { it.deltas() } +
                filtered.filter { "predicted_total_death" in it.metric }.map { it.deltas() }

    //endregion

    infix fun String.containsOneOf(list: List<String>) = list.any { it in this }

}

