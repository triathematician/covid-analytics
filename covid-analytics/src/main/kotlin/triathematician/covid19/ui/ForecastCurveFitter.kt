package triathematician.covid19.ui

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.AllowedSolution
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector
import org.apache.commons.math3.special.Erf
import tornadofx.Vector2D
import tornadofx.property
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.DateRange
import triathematician.util.monthDay
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt


const val LOGISTIC = "Logistic"
const val GEN_LOGISTIC = "General Logistic"
const val GAUSSIAN = "Gaussian"
const val GOMPERTZ = "Gompertz"

val SIGMOID_MODELS = listOf(LOGISTIC, GEN_LOGISTIC, GAUSSIAN, GOMPERTZ)

private val K_FIT_RANGE = 0.04..0.25
private val L_FIT_RANGE = 1E1..1E7
private val X0_FIT_RANGE = 10.0..100.0
private val V_FIT_RANGE = 1E-2..1E2

/** Tools for fitting forecast to empirical data. */
class ForecastCurveFitter: (Number) -> Double {

    //region CURVE VARS

    var curve by property(SIGMOID_MODELS[3])
    var l: Number by property(70000.0)
    var k: Number by property(0.07)
    var x0: Number by property(40.0)
    var v: Number by property(1.0)

    val equation: String
        get() = when (curve) {
            LOGISTIC -> String.format("%.2f / (1 + e^(-%.3f * (x - %.2f)))", l, k, x0)
            GEN_LOGISTIC -> String.format("%.2f / (1 + e^(-%.3f * (x - %.2f)))^(1/%.2f)", l, k, x0, v)
            GAUSSIAN -> String.format("%.2f * (1 + erf(-%.3f (x - %.2f)))/2", l, k, x0)
            GOMPERTZ -> String.format("%.2f * e^(-e^(-%.3f (x - %.2f)))", l, k, x0)
            else -> throw IllegalStateException()
        }

    var days: Number by property(21)
    var day0: Number by property(0)

    var fitLabel by property("Fit curves to different ranges of historical data.")

    //endregion

    //region PROPERTY UPDATES

    /** Updates the label property with the range of dates being used for the fit. */
    fun updateFitLabel(lastDate: LocalDate) {
        val first = lastDate.plusDays(day0.toLong() - days.toLong() + 1)
        val last = lastDate.plusDays(day0.toLong())
        fitLabel = "Fit curves to data from ${first.monthDay} to ${last.monthDay}"
    }

    /** Domain used for fitting. */
    private fun fitDomain(domain: DateRange) = domain.shift(day0.toInt(), day0.toInt()).tail(days.toInt())

    /** Compute estimated location of peak. */
    internal fun equationPeak(bracket: IntRange = 0..200): Pair<Double, Double> {
        val diffs = UnivariateFunction { x -> derivative(x) }
        val diffs2 = UnivariateFunction { x -> invoke(x + .01) - 2 * invoke(x) + invoke(x - .01) }
        val maxDay = bracket.maxBy { diffs.value(it.toDouble()) }!!
        val zero = BracketingNthOrderBrentSolver(1E-8, 5)
                .solve(100, diffs2, maxDay - 1.0, maxDay + 1.0, AllowedSolution.ANY_SIDE)
        return zero to (invoke(zero + .5) - invoke(zero - .5))
    }

    //endregion

    //region COMPUTE FUNCTIONS

    /** Current curve value. */
    override fun invoke(x: Number) = when (curve) {
        LOGISTIC -> logistic(x.toDouble())
        GEN_LOGISTIC -> generalLogistic(x.toDouble())
        GAUSSIAN -> gaussianErf(x.toDouble())
        GOMPERTZ -> gompertz(x.toDouble())
        else -> throw IllegalStateException()
    }

    /** Estimate derivative of curve at x. */
    fun derivative(x: Double) = 100*(invoke(x + .005) - invoke(x - .005))

    /** Compute logistic function at given # of days. */
    fun logistic(x: Double) = l / (1 + exp(-k * (x - x0)))
    /** Compute generalized logistic function at given # of days. */
    fun generalLogistic(x: Double) = l * (1 + exp(-k * (x - x0))).pow(-1 / v)
    /** Compute error function at given # of days. */
    fun gaussianErf(x: Double) = l * (1 + Erf.erf(k * (x - x0))) / 2.0
    /** Compute Gompertz function at given # of days. */
    fun gompertz(x: Double) = l * exp(-exp(-k * (x - x0)))

    //endregion

    //region FITTING AND STATS

    private fun empiricalDataForFitting(empirical: MetricTimeSeries?): List<Vector2D>? {
        val domain = empirical?.let { fitDomain(it.domain) } ?: return null
        return empirical.values.mapIndexed { i, v -> empirical.date(i) to Vector2D(i.toDouble(), v) }
                .filter { it.first in domain }.map { it.second }
    }

    /**
     * Compute standard error for the cumulative (raw) time series data.
     * @param empirical the empirical data
     * @param numDays # of days to use for fit
     */
    fun cumulativeStandardError(empirical: MetricTimeSeries?, shift: Double): Double? {
        val observedPoints = empiricalDataForFitting(empirical) ?: return null
        return standardError(observedPoints) { invoke(it + shift) }
    }

    /**
     * Compute standard error for the delta (day-over-day change) of this curve compared to provided empirical data.
     * @param shift # of days to add to empirical data (e.g. if averaged) to match the appropriate x value on the curve
     */
    fun deltaStandardError(empirical: MetricTimeSeries?, shift: Double): Double? {
        val observedPoints = empiricalDataForFitting(empirical?.deltas()) ?: return null
        return standardError(observedPoints) { derivative(it + shift) }
    }

    /**
     * Autofit series data using the cumulative SE.
     * Uses the current curve and the parameter vector [l, k, x0, v].
     * @param series the series to fit
     * @param range range of dates to use for fitting
     */
    fun autofitCumulativeSE(series: MetricTimeSeries) {
        val observedPoints = empiricalDataForFitting(series)!!
        val observedTarget = observedPoints.map { it.y }.toDoubleArray()

        val problem = LeastSquaresBuilder()
                .start(vec(l, k, x0, v))
                .model(solverFun(observedPoints))
                .target(observedTarget)
                .maxEvaluations(100000)
                .maxIterations(100000)
                .parameterValidator { v -> vec(v[0].coerceIn(L_FIT_RANGE), v[1].coerceIn(K_FIT_RANGE),
                        v[2].coerceIn(X0_FIT_RANGE), v[3].coerceIn(V_FIT_RANGE)) }
                .build()

        val optimum = LevenbergMarquardtOptimizer()
                .withCostRelativeTolerance(1.0e-9)
                .withParameterRelativeTolerance(1.0e-9)
                .optimize(problem)

        val optimalValues = optimum.point.toArray()
        println("Best fit: ${optimum.point}")
        l = optimalValues[0]
        k = optimalValues[1]
        x0 = optimalValues[2]
        v = optimalValues[3]
    }

    /** Function used for fitting curve around the given observed points. */
    private fun solverFun(observedPoints: List<Vector2D>) = MultivariateJacobianFunction { params ->
        val values = observedPoints.map { curve(it.x, params) }

        val jacobian = Array2DRowRealMatrix(observedPoints.size, 4)
        observedPoints.forEachIndexed { i, o ->
            jacobian.setEntry(i, 0, curvePartial(o.x, params, vec(1, 0, 0, 0)))
            jacobian.setEntry(i, 1, curvePartial(o.x, params, vec(0, 1, 0, 0)))
            jacobian.setEntry(i, 2, curvePartial(o.x, params, vec(0, 0, 1, 0)))
            jacobian.setEntry(i, 3, curvePartial(o.x, params, vec(0, 0, 0, 1)))
        }

        Jacobian(ArrayRealVector(values.toDoubleArray()), jacobian)
    }

    /** Compute curve for explicit set of parameters. */
    private fun curve(x: Double, params: RealVector) = when(curve) {
        LOGISTIC -> logistic(x, params[0], params[1], params[2])
        GEN_LOGISTIC -> generalLogistic(x, params[0], params[1], params[2], params[3])
        GAUSSIAN -> gaussianErf(x, params[0], params[1], params[2])
        GOMPERTZ -> gompertz(x, params[0], params[1], params[2])
        else -> throw IllegalStateException()
    }

    /** Compute partial derivative in given direction. */
    private fun curvePartial(x: Double, params: RealVector, delta: RealVector)
        = (curve(x, params + .0005*delta.unitVector()) - curve(x, params - .0005*delta.unitVector())) / 1000.0

    //endregion

}

//region MATH UTILS

typealias Jacobian = org.apache.commons.math3.util.Pair<RealVector, RealMatrix>

private operator fun RealVector.get(i: Int) = getEntry(i)
private operator fun RealVector.plus(v2: RealVector) = this.add(v2)
private operator fun RealVector.minus(v2: RealVector) = this.subtract(v2)
private operator fun RealVector.div(x: Double) = this.mapDivide(x)
private operator fun Double.times(v: RealVector) = v.mapMultiply(this)

private fun vec(vararg x: Number) = listOf(*x).map { it.toDouble() }.toDoubleArray().let { ArrayRealVector(it) }

/** Compute logistic function. */
fun logistic(x: Double, l: Double, k: Double, x0: Double) = l / (1 + exp(-k * (x - x0)))
/** Compute generalized logistic function. */
fun generalLogistic(x: Double, l: Double, k: Double, x0: Double, v: Double) = l * (1 + exp(-k * (x - x0))).pow(-1 / v)
/** Compute error function. */
fun gaussianErf(x: Double, l: Double, k: Double, x0: Double) = l * (1 + Erf.erf(k * (x - x0))) / 2.0
/** Compute Gompertz function. */
fun gompertz(x: Double, l: Double, k: Double, x0: Double) = l * exp(-exp(-k * (x - x0)))

operator fun Number.div(x: Double) = toDouble() / x
operator fun Number.unaryMinus() = -toDouble()
operator fun Number.times(x: Double) = toDouble() * x
operator fun Number.plus(x: Double) = toDouble() + x
operator fun Double.minus(x: Number) = this - x.toDouble()
operator fun Double.div(x: Number) = this / x.toDouble()
operator fun Int.div(x: Number) = this / x.toDouble()

/** Compute standard error given list of points and given function. */
private fun standardError(points: List<Vector2D>, function: (Double) -> Double)
        = sqrt(points.map { it.y - function(it.x) }.map { it * it }.sum() / points.size)

//endregion