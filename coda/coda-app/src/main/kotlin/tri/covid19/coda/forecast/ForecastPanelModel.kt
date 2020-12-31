package tri.covid19.coda.forecast

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import org.apache.commons.math3.exception.NoBracketingException
import org.apache.commons.math3.exception.TooManyEvaluationsException
import tornadofx.*
import tri.area.Lookup
import tri.area.USA
import tri.area.Usa
import tri.covid19.coda.history.METRIC_OPTIONS
import tri.covid19.coda.history.changeDoublingDataSeries
import tri.covid19.coda.history.hubbertSeries
import tri.covid19.coda.utils.ChartDataSeries
import tri.covid19.data.*
import tri.math.Sigmoid
import tri.timeseries.Forecast
import tri.timeseries.TimeSeries
import tri.util.DateRange
import tri.util.minus
import tri.util.userFormat
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.time.ExperimentalTime

/** Config for logistic projection. */
@ExperimentalTime
class ForecastPanelModel(var listener: () -> Unit = {}) {

    //region UI BOUND PROPERTIES

    // metric selection
    internal var areaId by property(USA.id)
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

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> updateData();
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
 listener() } }
    private fun <T> forecastProperty(prop: KMutableProperty1<*, T>) = curveFitter.getProperty(prop).apply {
        addListener { _ ->
            updateData()
            updateEquation()
            vActive = curveFitter.curve == Sigmoid.GEN_LOGISTIC
            listener()
        }
    }

    internal val _region = property(ForecastPanelModel::areaId)
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

    /** List of areas available for panel. */
    val areas: SortedSet<String> by lazy {
        val dataAreas = LocalCovidDataQuery.areas.map { it.id }
        val forecastAreas = CovidForecasts.allForecasts.map { it.areaId }.toSet()
        (dataAreas + forecastAreas).toSortedSet()
    }

    /** Domain for raw data. */
    var domain: DateRange? = null

    /** The primary time series for the selected metric. */
    val mainSeries = SimpleObjectProperty<TimeSeries?>()
    /** User's projection. */
    var userForecast: TimeSeries? = null

    /** Past forecasts. */
    var pastForecasts = PastForecasts()
    /** Other forecasts. */
    var externalForecasts = ExternalForecasts()

    private fun updateData() {
        val areaMetrics = CovidTimeSeriesSources.dailyReports(Lookup.area(areaId), selectedMetric)
        mainSeries.value = areaMetrics.firstOrNull { it.metric == selectedMetric }?.restrictNumberOfStartingZerosTo(0)
        domain = mainSeries.value?.domain?.shift(0, 30)

        val shift = if (smooth) -3.5 else 0.0
        userForecast = when {
            !showForecast -> null
            domain == null -> null
            else -> TimeSeries("User Forecast", areaId, "$selectedMetric (curve)", "",false, 0.0, domain!!.start,
                    domain!!.map { d -> curveFitter(d, shift) })
        }

        pastForecasts.metrics = areaMetrics.filter { showLogisticPrediction && ("predicted" in it.metric || "peak" in it.metric) }
        externalForecasts.forecasts = CovidForecasts.allForecasts
                .filter { it.model in otherForecasts }
                .filter { it.areaId == areaId && it.metric == selectedMetric }
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

    private fun TimeSeries.residuals(empirical: TimeSeries?): TimeSeries? {
        empirical ?: return null
        val commonDomain = domain.intersect(empirical.domain)
        commonDomain ?: return null
        return copy(start = commonDomain.start, values = commonDomain.map { empirical[it] - get(it) })
    }

    private fun dataseries(op: MutableList<ChartDataSeries>.() -> Unit) = mutableListOf<ChartDataSeries>().apply { op() }
    private fun MutableList<ChartDataSeries>.series(s: TimeSeries?) { series(listOfNotNull(s)) }
    private fun MutableList<ChartDataSeries>.series(s: List<TimeSeries>) {
        domain?.let { domain -> s.forEach { this += tri.covid19.coda.utils.series(it.metric, domain, it) } }
    }
    private fun MutableList<ChartDataSeries>.series(xy: Pair<TimeSeries, TimeSeries>?, idFirst: Boolean = true) { series(listOfNotNull(xy), idFirst) }
    private fun MutableList<ChartDataSeries>.series(xyxy: List<Pair<TimeSeries, TimeSeries>>, idFirst: Boolean = true) {
        domain?.let { domain -> xyxy.forEach { this += tri.covid19.coda.utils.series(if (idFirst) it.first.metric else it.second.metric, domain, it.first, it.second) } }
    }

    private fun TimeSeries.maybeSmoothed() = if (smooth) movingAverage(7) else this

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
        val states = Usa.stateNames.toSortedSet()
        areaId = when {
            states.contains(areaId) -> states.rollAfter(areaId)
            else -> states.first()
        }
        autofit()
    }

    /** Load the next US state in alphabetical order. */
    fun goToPreviousUsState() {
        val states = Usa.stateNames.toSortedSet()
        areaId = when {
            states.contains(areaId) -> states.rollBefore(areaId)
            else -> states.last()
        }
        autofit()
    }

    private fun <X> SortedSet<X>.rollAfter(x: X) = tailSet(x).elementAtOrNull(1) ?: first()
    private fun <X> SortedSet<X>.rollBefore(x: X) = headSet(x).reversed().elementAtOrNull(0) ?: last()

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
        areaId = f.region.id
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
    class PastForecasts(var metrics: List<TimeSeries> = listOf())
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

