package tri.covid19.forecaster.forecast

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import org.apache.commons.math3.exception.NoBracketingException
import org.apache.commons.math3.exception.TooManyEvaluationsException
import tornadofx.*
import tri.covid19.data.*
import tri.covid19.forecaster.history.METRIC_OPTIONS
import tri.covid19.forecaster.history.changeDoublingDataSeries
import tri.covid19.forecaster.history.hubbertSeries
import tri.timeseries.Forecast
import tri.covid19.forecaster.utils.ChartDataSeries
import tri.math.GEN_LOGISTIC
import tri.regions.RegionLookup
import tri.regions.UnitedStates
import tri.timeseries.MetricTimeSeries
import tri.timeseries.MinMaxFinder
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
class ForecastPanelModel(var listener: () -> Unit = {}) {

    //region UI BOUND PROPERTIES

    // metric selection
    internal var region by property("US")
    internal var selectedMetric by property(METRIC_OPTIONS[0])
    internal var perCapita by property(false)
    internal var smooth by property(true)
    internal var showLogisticPrediction by property(true)

    // user forecast
    internal val curveFitter = ForecastCurveFitter()
    internal val forecastInfoList = observableListOf<ForecastStats>()

    internal var showForecast by property(true)
    private var vActive by property(false)

    internal val _manualEquation = SimpleStringProperty(curveFitter.equation)
    internal val _manualPeak = SimpleStringProperty("")
    internal val _manualLogCumStdErr = SimpleStringProperty("")
    internal val _manualDeltaStdErr = SimpleStringProperty("")

    // other forecasts
    internal val otherForecasts = observableListOf(IHME, YYG).apply { onChange { listener() } }

    var showConfidence by property(true)
    var firstForecastDay: Number by property(90)
    var lastForecastDay: Number by property(120)
    private val forecastDateRange: DateRange
        get() = DateRange(curveFitter.numberToDate(firstForecastDay), curveFitter.numberToDate(lastForecastDay))

//    // historical forecasts
//    private var movingAverage by property(4)
//    private var projectionDays by property(10)

    //endregion

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> updateData(); listener() } }
    private fun <T> forecastProperty(prop: KMutableProperty1<*, T>) = curveFitter.getProperty(prop).apply {
        addListener { _ ->
            updateData()
            updateEquation()
            vActive = curveFitter.curve == GEN_LOGISTIC
            listener()
        }
    }

    internal val _region = property(ForecastPanelModel::region)
    internal val _selectedMetric = property(ForecastPanelModel::selectedMetric)
    internal val _perCapita = property(ForecastPanelModel::perCapita)
    internal val _smooth = property(ForecastPanelModel::smooth)
    internal val _showLogisticPrediction = property(ForecastPanelModel::showLogisticPrediction)

    internal val _showForecast = property(ForecastPanelModel::showForecast)
    internal val _vActive = property(ForecastPanelModel::vActive)
//
//    internal val _movingAverage = property(ForecastPanelModel::movingAverage)
//    internal val _projectionDays = property(ForecastPanelModel::projectionDays)

    internal val _fitLabel = forecastProperty(ForecastCurveFitter::fitLabel)
    internal val _curve = forecastProperty(ForecastCurveFitter::curve)
    internal val _l = forecastProperty(ForecastCurveFitter::l)
    internal val _k = forecastProperty(ForecastCurveFitter::k)
    internal val _x0 = forecastProperty(ForecastCurveFitter::x0)
    internal val _v = forecastProperty(ForecastCurveFitter::v)

    internal val _firstFitDay = forecastProperty(ForecastCurveFitter::firstFitDay).apply { onChange { autofit() }}
    internal val _lastFitDay = forecastProperty(ForecastCurveFitter::lastFitDay).apply { onChange { autofit() }}
    internal val _fitCumulative = forecastProperty(ForecastCurveFitter::fitCumulative).apply { onChange { autofit() }}

    internal val _firstEvalDay = curveFitter.getProperty(ForecastCurveFitter::firstEvalDay).apply { onChange { calcErrors() }}
    internal val _lastEvalDay = curveFitter.getProperty(ForecastCurveFitter::lastEvalDay).apply { onChange { calcErrors() }}

    internal val _showConfidence = property(ForecastPanelModel::showConfidence)
    internal val _firstForecastDay = property(ForecastPanelModel::firstForecastDay)
    internal val _lastForecastDay = property(ForecastPanelModel::lastForecastDay)

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
    val mainSeries = SimpleObjectProperty<MetricTimeSeries?>()
    /** User's projection. */
    var userForecast: MetricTimeSeries? = null

    /** Past forecasts. */
    var pastForecasts = PastForecasts()
    /** Other forecasts. */
    var externalForecasts = ExternalForecasts()

    private fun updateData() {
        val regionMetrics = CovidTimeSeriesSources.dailyReports(RegionLookup(region), selectedMetric)
        mainSeries.value = regionMetrics.firstOrNull { it.metric == selectedMetric }?.restrictNumberOfStartingZerosTo(0)
        domain = mainSeries.value?.domain?.shift(0, 30)

        val shift = if (smooth) -3.5 else 0.0
        userForecast = when {
            !showForecast -> null
            domain == null -> null
            else -> MetricTimeSeries(RegionLookup(region), "$selectedMetric (curve)", false, 0.0, domain!!.start,
                    domain!!.map { d -> curveFitter(d, shift) })
        }

        pastForecasts.metrics = regionMetrics.filter { showLogisticPrediction && ("predicted" in it.metric || "peak" in it.metric) }
        externalForecasts.forecasts = CovidForecasts.allForecasts
                .filter { it.model in otherForecasts }
                .filter { it.region.id == region && it.metric == selectedMetric }
                .filter { it.forecastDate in forecastDateRange }
    }

    //endregion

    //region SERIES BUILDERS

    internal fun cumulativeDataSeries() = dataseries {
        series(mainSeries.value?.maybeSmoothed())
        series(userForecast)
        series(pastForecasts.cumulative)
        series(externalForecasts.cumulative)
    }

    internal fun dailyDataSeries() = dataseries {
        series(mainSeries.value?.deltas()?.maybeSmoothed())
        series(userForecast?.deltas())
        series(pastForecasts.deltas)
        series(externalForecasts.cumulative.map { it.deltas() })
    }

    internal fun hubbertDataSeries() = dataseries {
        series(mainSeries.value?.hubbertSeries(7))
        series(userForecast?.hubbertSeries(1))
        series(externalForecasts.cumulative.map { it.hubbertSeries(1) })
    }

    internal fun changeDoublingDataSeries() = dataseries {
        series(mainSeries.value?.changeDoublingDataSeries(7))
        series(userForecast?.changeDoublingDataSeries(1))
        series(externalForecasts.cumulative.map { it.changeDoublingDataSeries(1) })
    }

    internal fun residualDataSeries() = dataseries {
        val daily = mainSeries.value?.deltas()?.maybeSmoothed()
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
            "${y.userFormat()} on day ${curveFitter.numberToDate(x) ?: "?"}"
        } catch (x: NoBracketingException) {
            ""
        }

        val se1 = curveFitter.cumulativeRmse(empirical = mainSeries.value)
        val se2 = curveFitter.deltaRmse(empirical = mainSeries.value)

        _manualLogCumStdErr.value = "RMSE = ${se1?.userFormat() ?: "?"} (totals)"
        _manualDeltaStdErr.value = "RMSE = ${se2?.userFormat() ?: "?"} (per day)"
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
        try {
            curveFitter.autofit(mainSeries.value)
        } catch (x: TooManyEvaluationsException) {
            alert(Alert.AlertType.ERROR, "Too many evaluations during curve fit.")
        }
    }

    /** Recalculates errors. */
    fun calcErrors() {
        updateEquation()
    }

    /** Loads selected forecast. */
    fun load(f: ForecastStats) {
        region = f.region.id
        selectedMetric = f.metric
        curveFitter.curve = f.sigmoidCurve
        curveFitter.l = f.sigmoidParameters?.load as Number
        curveFitter.k = f.sigmoidParameters?.k as Number
        curveFitter.x0 = f.sigmoidParameters?.x0 as Number
        curveFitter.v = f.sigmoidParameters?.v as Number

        f.fitDateRange?.run {
            curveFitter.firstFitDay = start.minus(LocalDate.now())
            curveFitter.lastFitDay = endInclusive.minus(LocalDate.now())
        }
    }

    /** Save current config as new forecast. */
    fun save() {
        val empirical = mainSeries.value
        if (empirical != null) {
            forecastInfoList.add(curveFitter.userForecastInfo(empirical))
        }
    }

    /** Save all other forecasts. */
    fun saveExternalForecastsToTable() {
        val empirical = mainSeries.value
        if (empirical != null) {
            externalForecasts.forecasts.filter { it.model in otherForecasts }
                    .forEach { forecastInfoList.add(curveFitter.forecastStats(it, empirical)) }
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
        get() = forecasts.filter { it.model in otherForecasts }.flatMap {
            it.data.filter { showConfidence || ("lower" !in it.metric && "upper" !in it.metric) }
        }
    val ExternalForecasts.cumulative
        get() = filtered.toMutableList()
    val ExternalForecasts.deltas
        get() = filtered.map { it.deltas() }

    //endregion


}

