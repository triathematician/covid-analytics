package triathematician.covid19.ui

import org.apache.commons.math3.special.Erf.erf
import tornadofx.getProperty
import tornadofx.property
import kotlin.math.exp
import kotlin.math.pow
import kotlin.reflect.KMutableProperty1

val SIGMOID_MODELS = listOf("Logistic", "General Logistic", "Gaussian", "Gompertz")

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