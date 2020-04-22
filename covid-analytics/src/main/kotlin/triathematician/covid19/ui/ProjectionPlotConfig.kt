package triathematician.covid19.ui

import javafx.beans.property.SimpleStringProperty
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.AllowedSolution
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
import org.apache.commons.math3.exception.NoBracketingException
import org.apache.commons.math3.special.Erf.erf
import tornadofx.getProperty
import tornadofx.property
import triathematician.covid19.CovidTimeSeriesSources
import triathematician.covid19.DEATHS
import triathematician.covid19.sources.IhmeProjections
import triathematician.timeseries.MetricTimeSeries
import triathematician.timeseries.dateRange
import triathematician.util.DateRange
import triathematician.util.minus
import java.lang.IllegalStateException
import java.time.LocalDate
import kotlin.math.*
import kotlin.reflect.KMutableProperty1

const val LOGISTIC = "Logistic"
const val GEN_LOGISTIC = "General Logistic"
const val GAUSSIAN = "Gaussian"
const val GOMPERTZ = "Gompertz"
val SIGMOID_MODELS = listOf(LOGISTIC, GEN_LOGISTIC, GAUSSIAN, GOMPERTZ)

/** Config for logistic projection. */
class ProjectionPlotConfig(var onChange: () -> Unit = {}) {

    // region and metrics
    internal var region by property("US")
    internal var selectedMetric by property(METRIC_OPTIONS[0])
    internal var showIhme by property(true)

    // manual fit
    internal var showManual by property(true)
    private var manualModel by property(SIGMOID_MODELS[0])
    private var l: Number by property(1000.0)
    private var k: Number by property(1.0)
    private var x0: Number by property(1.0)
    private var v: Number by property(1.0)
    private var vActive by property(false)

    //    private var showR2 by property(false)
    internal val _manualEquation = SimpleStringProperty(equationString())

    //    internal val _r2 = SimpleDoubleProperty(0.0)
    internal val _manualPeak = SimpleStringProperty("")
    internal val _manualDeltaStdErr = SimpleStringProperty("")
    internal val _manualLogCumStdErr = SimpleStringProperty("")

    // statistical fit
    private var showFit by property(false)
    private var fitModel by property(SIGMOID_MODELS[0])

    // projection history
    private var movingAverage by property(4)
    private var projectionDays by property(10)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> updateData(); onChange() } }

    internal val _region = property(ProjectionPlotConfig::region)
    internal val _selectedRegion = property(ProjectionPlotConfig::selectedMetric)
    internal val _showIhme = property(ProjectionPlotConfig::showIhme)

    internal val _showManual = property(ProjectionPlotConfig::showManual)
    internal val _manualModel = property(ProjectionPlotConfig::manualModel).apply { addListener { _ -> vActive = manualModel == GEN_LOGISTIC; updateEquation() } }
    internal val _l = property(ProjectionPlotConfig::l).apply { addListener { _ -> updateEquation() } }
    internal val _k = property(ProjectionPlotConfig::k).apply { addListener { _ -> updateEquation() } }
    internal val _x0 = property(ProjectionPlotConfig::x0).apply { addListener { _ -> updateEquation() } }
    internal val _v = property(ProjectionPlotConfig::v).apply { addListener { _ -> updateEquation() } }
    internal val _vActive = property(ProjectionPlotConfig::vActive)
//    internal val _showR2 = property(ProjectionPlotConfig::showR2)

    internal val _showFit = property(ProjectionPlotConfig::showFit)
    internal val _fitModel = property(ProjectionPlotConfig::fitModel)

    internal val _movingAverage = property(ProjectionPlotConfig::movingAverage)
    internal val _projectionDays = property(ProjectionPlotConfig::projectionDays)

    //endregion

    //region DATA SERIES

    /** All time series data for the region and selected metric. */
    var regionMetrics = listOf<MetricTimeSeries>()
    /** The primary time series for the selected metric. */
    var mainSeries: MetricTimeSeries? = null
    /** Projections. */
    var otherProjections = listOf<MetricTimeSeries>()
    /** Manual projection. */
    var manualProjection: MetricTimeSeries? = null
    /** Domain for raw data. */
    var domain: DateRange? = null
    /** Domain for projection data. */
    var ihmeDomain: DateRange? = null
    /** Domain for plotted data. */
    var totalDomain: DateRange? = null

    private fun updateData() {
        regionMetrics = CovidTimeSeriesSources.dailyReports({ it == region }).filter { selectedMetric in it.metric }
        mainSeries = regionMetrics.firstOrNull { it.metric == selectedMetric }
        otherProjections = IhmeProjections.allProjections.filter { it.id == region }

        domain = mainSeries?.dateRange
        ihmeDomain = otherProjections.dateRange
        totalDomain = domain?.let { DateRange(it.start, it.endInclusive.plusDays(30)) }

        manualProjection = if (!showManual) null else totalDomain?.let { manualProjection(it, it.start) }
    }

    internal fun cumulativeDataSeries() = listOfNotNull(series(domain, mainSeries), series(totalDomain, manualProjection)) +
            regionMetrics.filter { "predicted" in it.metric && "peak" !in it.metric }.mapNotNull { series(domain, it) } +
            otherProjections.filter { showIhme && selectedMetric == DEATHS && "change" !in it.metric }.mapNotNull { series(totalDomain, ihmeDomain, it) }

    internal fun dailyDataSeries() = listOfNotNull(series(domain, mainSeries?.deltas()), series(totalDomain, manualProjection?.deltas())) +
            regionMetrics.filter { "predicted peak" in it.id }.mapNotNull { series(domain, it) } +
            otherProjections.filter { showIhme && "change" in it.metric }.mapNotNull { series(totalDomain, ihmeDomain, it) }

    internal fun hubbertDataSeries() = listOfNotNull(mainSeries?.hubbertSeries(7)).mapNotNull { series(domain?.plus(1, 0), it.first, it.second) } +
            listOfNotNull(manualProjection?.hubbertSeries(1)).mapNotNull { series(totalDomain?.plus(1, 0), it.first, it.second) } +
            otherProjections.filter { showIhme && selectedMetric == DEATHS && "change" !in it.metric }.map { it.hubbertSeries(1) }
                    .mapNotNull { series(totalDomain, ihmeDomain?.plus(1, 0), it.first, it.second) }

    internal fun peakDataSeries() = regionMetrics.filter { "days" in it.id }.mapNotNull { series(domain, it) }

    private fun series(dom: DateRange?, s: MetricTimeSeries?) = if (dom != null && s != null) DataSeries(dom, s) else null
    private fun series(dom: DateRange?, x: MetricTimeSeries?, y: MetricTimeSeries?) = if (dom != null && x != null && y != null) DataSeries(dom, x, y) else null
    private fun series(dom: DateRange?, dom2: DateRange?, s: MetricTimeSeries?) = if (dom != null && dom2 != null && s != null) DataSeries(dom, dom2, s) else null
    private fun series(dom: DateRange?, dom2: DateRange?, x: MetricTimeSeries?, y: MetricTimeSeries?) = if (dom != null && dom2 != null && x != null && y != null) DataSeries(dom, dom2, x, y) else null

    //endregion

    //region MANUAL CURVE

    /** Generate time series function using given domain. */
    fun manualProjection(domain: DateRange, zeroDay: LocalDate) = MetricTimeSeries(id = region, metric = "$selectedMetric (curve)",
            intSeries = false, start = zeroDay, values = domain.map { it.minus(zeroDay) }.map { curve(it) })

    /** Current curve value. */
    private fun curve(x: Number) = when (manualModel) {
        LOGISTIC -> logistic(x.toDouble())
        GEN_LOGISTIC -> generalLogistic(x.toDouble())
        GAUSSIAN -> gaussianErf(x.toDouble())
        GOMPERTZ -> gompertz(x.toDouble())
        else -> throw IllegalStateException()
    }

    /** Compute logistic function at given # of days. */
    fun logistic(x: Double) = l / (1 + exp(-k * (x - x0)))

    /** Compute generalized logistic function at given # of days. */
    fun generalLogistic(x: Double) = l * (1 + exp(-k * (x - x0))).pow(-1 / v)

    /** Compute error function at given # of days. */
    fun gaussianErf(x: Double) = l * (1 + erf(k * (x - x0))) / 2.0

    /** Compute Gompertz function at given # of days. */
    fun gompertz(x: Double) = l * exp(-exp(-k * (x - x0)))

//    /** Function transform for sigmoids. */
//    private fun ((Double) -> Double).normalized(): (Double) -> Double = { l * invoke(k*(it-x0)) }

    /** Updates equation label. */
    private fun updateEquation() {
        _manualEquation.value = equationString()
        _manualPeak.value = try {
            val (x, y) = equationPeak()
            String.format("%.2f", y) + " on day " + String.format("%.1f", x)
        } catch (x: NoBracketingException) {
            ""
        }
        _manualDeltaStdErr.value = "SE = ${String.format("%.1f", equationDeltaSE())} (per day)"
        _manualLogCumStdErr.value = "SE = ${String.format("%.2f", equationCumSE())} (totals, last 3 weeks)"
    }

    private fun equationDeltaSE(): Any {
        val deltas = mainSeries?.deltas()?.values ?: return "?"
        val sse = deltas.mapIndexed { i, y -> y - (curve(i - 3.5 + .5) - curve(i - 3.5 - .5)) }.map { it * it }.sum()
        return sqrt(sse / deltas.size)
    }

    private fun equationCumSE(): Any {
        val logs = mainSeries?.values?.map { it } ?: return ""
        val n = logs.size
        val sse = logs.mapIndexed { i, y -> y - curve(i - 3.5) }.takeLast(21).map { it * it }.sum()
        return sqrt(sse / minOf(n, 21))
    }

    private fun equationString() = when (manualModel) {
//            LOGISTIC -> "L / (1 + e^(-k(x-x0)))"
        LOGISTIC -> String.format("%.2f / (1 + e^(-%.2f * (x - %.2f)))", l, k, x0)
        GEN_LOGISTIC -> String.format("%.2f / (1 + e^(-%.2f * (x - %.2f)))^(1/%.2f)", l, k, x0, v)
        GAUSSIAN -> String.format("%.2f * (1 + erf(-%.2f (x - %.2f)))/2", l, k, x0)
        GOMPERTZ -> String.format("%.2f * e^(-e^(-%.2f (x - %.2f)))", l, k, x0)
        else -> throw IllegalStateException()
    }

    private fun equationPeak(): Pair<Double, Double> {
        val diffs = object : UnivariateFunction {
            override fun value(x: Double) = curve(x + .01) - curve(x - .01)
        }
        val diffs2 = object : UnivariateFunction {
            override fun value(x: Double) = curve(x + .01) - 2 * curve(x) + curve(x - .01)
        }
        val maxDay = (0..200).maxBy { diffs.value(it.toDouble()) }!!
        val zero = BracketingNthOrderBrentSolver(1E-8, 5)
                .solve(100, diffs2, maxDay - 1.0, maxDay + 1.0, AllowedSolution.ANY_SIDE)
        return zero to (curve(zero + .5) - curve(zero - .5))
    }

    //endregion

}

//region MATH UTILS

operator fun Number.div(x: Double) = toDouble() / x
operator fun Number.unaryMinus() = -toDouble()
operator fun Number.times(x: Double) = toDouble() * x
operator fun Double.minus(x: Number) = this - x.toDouble()
operator fun Double.div(x: Number) = this / x.toDouble()
operator fun Int.div(x: Number) = this / x.toDouble()

//endregion