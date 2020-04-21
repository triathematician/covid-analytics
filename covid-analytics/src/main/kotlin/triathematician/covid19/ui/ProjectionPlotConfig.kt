package triathematician.covid19.ui

import org.apache.commons.math3.special.Erf.erf
import tornadofx.getProperty
import tornadofx.property
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.DateRange
import triathematician.util.minus
import java.lang.IllegalStateException
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.pow
import kotlin.reflect.KMutableProperty1

const val LOGISTIC = "Logistic"
const val GEN_LOGISTIC = "General Logistic"
const val GAUSSIAN = "Gaussian"
const val GOMPERTZ = "Gompertz"
val SIGMOID_MODELS = listOf(LOGISTIC, GEN_LOGISTIC, GAUSSIAN, GOMPERTZ)

/** Config for logistic projection. */
class ProjectionPlotConfig(var onChange: () -> Unit = {}) {

    // region and metrics
    var region by property("US")
    var selectedMetric by property(METRIC_OPTIONS[0])
    var showIhme by property(true)

    // manual fit
    var showManual by property(true)
    var manualModel by property(SIGMOID_MODELS[0])
    var l: Number by property(1.0)
    var k: Number by property(1.0)
    var x0: Number by property(1.0)
    var v: Number by property(1.0)
    var showR2 by property(false)

    // statistical fit
    var showFit by property(false)
    var fitModel by property(SIGMOID_MODELS[0])

    // projection history
    var movingAverage by property(4)
    var projectionDays by property(10)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> onChange() } }

    val _region = property(ProjectionPlotConfig::region)
    val _selectedRegion = property(ProjectionPlotConfig::selectedMetric)
    val _showIhme = property(ProjectionPlotConfig::showIhme)

    val _showManual = property(ProjectionPlotConfig::showManual)
    val _manualModel = property(ProjectionPlotConfig::manualModel)
    val _l = property(ProjectionPlotConfig::l)
    val _k = property(ProjectionPlotConfig::k)
    val _x0 = property(ProjectionPlotConfig::x0)
    val _v = property(ProjectionPlotConfig::v)
    val _showR2 = property(ProjectionPlotConfig::showR2)

    val _showFit = property(ProjectionPlotConfig::showFit)
    val _fitModel = property(ProjectionPlotConfig::fitModel)

    val _movingAverage = property(ProjectionPlotConfig::movingAverage)
    val _projectionDays = property(ProjectionPlotConfig::projectionDays)

    //endregion

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
    fun gaussianErf(x: Double) = l * erf(k * (x -x0))
    /** Compute Gompertz function at given # of days. */
    fun gompertz(x: Double) = l * exp(-exp(-k * (x - x0)))

//    /** Function transform for sigmoids. */
//    private fun ((Double) -> Double).normalized(): (Double) -> Double = { l * invoke(k*(it-x0)) }

    //region MATH UTILS

    operator fun Number.div(x: Double) = toDouble() / x
    operator fun Number.unaryMinus() = -toDouble()
    operator fun Number.times(x: Double) = toDouble() * x
    operator fun Double.minus(x: Number) = this - x.toDouble()
    operator fun Double.div(x: Number) = this / x.toDouble()
    operator fun Int.div(x: Number) = this / x.toDouble()

    //endregion
}