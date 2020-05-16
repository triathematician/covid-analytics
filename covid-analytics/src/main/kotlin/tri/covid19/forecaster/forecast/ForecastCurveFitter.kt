package tri.covid19.forecaster.forecast

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.AllowedSolution
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealVector
import tornadofx.Vector2D
import tornadofx.property
import tri.math.*
import tri.timeseries.Forecast
import tri.timeseries.MetricTimeSeries
import tri.util.DateRange
import tri.util.minus
import tri.util.monthDay
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt

private const val MODEL_NAME = "User"

private val K_FIT_RANGE = 0.03..0.25
private val L_FIT_RANGE = 1E1..1E7
private val X0_FIT_RANGE = 10.0..200.0
private val V_FIT_RANGE = 1E-2..1E2

internal val DAY0 = LocalDate.of(2020, 1, 1)

/** Tools for fitting forecast to empirical data. January 1, 2020 is "day 0". */
class ForecastCurveFitter: (Number) -> Double {

    //region PROPERTIES

    var curve by property(SIGMOID_MODELS[3])
    var l: Number by property(70000.0)
    var k: Number by property(0.07)
    var x0: Number by property(90.0)
    var v: Number by property(1.0)

    private val now
        get() = LocalDate.now()
    val nowInt
        get() = dateToNumber(now)

    var firstFitDay: Number by property(nowInt - 20)
    var lastFitDay: Number by property(nowInt - 7)
    private val fitDateRange: DateRange
        get() = DateRange(firstFitDay.toDate, lastFitDay.toDate)

    var firstEvalDay: Number by property(nowInt - 6)
    var lastEvalDay: Number by property(nowInt)
    private val evalDateRange: DateRange
        get() = DateRange(firstEvalDay.toDate, lastEvalDay.toDate)

    var fitLabel by property("Automatically fit curves based on historical data.")

    //endregion

    //region DERIVED PROPERTIES

    val equation
        get() = when (curve) {
            LINEAR -> String.format("%.2f * (1 + %.2f * (x - %.2f)) / 2", l, k, x0)
            LOGISTIC -> String.format("%.2f / (1 + e^(-%.3f * (x - %.2f)))", l, k, x0)
            GEN_LOGISTIC -> String.format("%.2f / (1 + e^(-%.3f * (x - %.2f)))^(1/%.2f)", l, k, x0, v)
            GAUSSIAN -> String.format("%.2f * (1 + erf(-%.3f (x - %.2f)))/2", l, k, x0)
            GOMPERTZ -> String.format("%.2f * e^(-e^(-%.3f (x - %.2f)))", l, k, x0)
            else -> throw IllegalStateException()
        }

    //endregion

    //region PROPERTY UPDATES

    /** Converts day to int. */
    fun dateToNumber(date: LocalDate) = date - DAY0
    /** Converts int to day. */
    fun numberToDate(value: Number) = DAY0.plusDays(value.toLong())

    private val LocalDate.toNumber
        get() = dateToNumber(this)
    private val Number.toDate
        get() = numberToDate(this)
    private val Number.monthDay
        get() = numberToDate(this).monthDay

    /** Updates the label property with the range of dates being used for the fit. */
    fun updateFitLabel() {
        fitLabel = "Automatically fit curves based on historical data from ${firstFitDay.monthDay} to ${lastFitDay.monthDay}"
    }

    /** Compute estimated location of peak. */
    internal fun equationPeak(bracket: IntRange = 0..200): Pair<Double, Double> {
        val diffs = UnivariateFunction { x -> derivative(x) }
        val diffs2 = UnivariateFunction { x -> invoke(x + .01) - 2 * invoke(x) + invoke(x - .01) }
        val maxDay = bracket.maxBy { diffs.value(it.toDouble()) }!!
        val zero = BracketingNthOrderBrentSolver(1E-8, 5)
                .solve(100, diffs2, maxDay - 1.0, maxDay + 1.0, AllowedSolution.ANY_SIDE)
        return zero to (invoke(zero + .5) - invoke(zero - .5))
    }

    /**
     * Creates new forecast from current settings.
     * @param empirical empirical data for metrics
     */
    fun userForecastInfo(empirical: MetricTimeSeries): ForecastStats {
        val forecastDomain = DateRange(DAY0, JULY31)
        val forecastValues = forecastDomain.map { invoke(it.minus(DAY0)) }
        val series = empirical.copy(metric = "${empirical.metric} (user forecast)", start = DAY0, values = forecastValues)
        val f = Forecast(MODEL_NAME, LocalDate.now(), empirical.region, empirical.metric, listOf(series))

        return ForecastStats(f).apply {
            sigmoidParameters = SigmoidParameters(curve, l.toDouble(), k.toDouble(), x0.toDouble(), v.toDouble())
            totalValue = l

            val peak = equationPeak()
            peakDay = DAY0.plusDays(peak.first.toLong())
            peakValue = peak.second

            forecastDays[APR30] = derivative(APR30.minus(DAY0).toDouble())
            forecastDays[MAY31] = derivative(MAY31.minus(DAY0).toDouble())
            forecastDays[JUNE30] = derivative(JUNE30.minus(DAY0).toDouble())
            forecastDays[JULY31] = derivative(JULY31.minus(DAY0).toDouble())

            forecastTotals[APR30] = invoke(APR30.minus(DAY0).toDouble())
            forecastTotals[MAY31] = invoke(MAY31.minus(DAY0).toDouble())
            forecastTotals[JUNE30] = invoke(JUNE30.minus(DAY0).toDouble())
            forecastTotals[JULY31] = invoke(JULY31.minus(DAY0).toDouble())

            fitDateRange = empirical.domain.intersect(this@ForecastCurveFitter.fitDateRange)
            rmsErrorCumulative = cumulativeRmse(empirical = empirical)
            rmsErrorDelta = deltaRmse(empirical = empirical)
            masErrorCumulative = cumulativeMase(empirical = empirical)
            masErrorDelta = deltaMase(empirical = empirical)
        }
    }


    /**
     * Creates new forecast from current settings.
     * @param empirical empirical data for metrics
     */
    fun forecastStats(forecast: Forecast, empirical: MetricTimeSeries) = ForecastStats(forecast).apply {
        sigmoidParameters = null
        fitDateRange = null

        val totals = forecast.data.first { forecast.metric in it.metric && "lower" !in it.metric && "upper" !in it.metric }
        val deltas = totals.deltas()
        totalValue = totals.lastValue
        deltas.peak().apply {
            peakDay = first
            peakValue = second
        }

        arrayOf(APR30, MAY31, JUNE30, JULY31).forEach {
            forecastTotals[it] = totals[it]
            forecastDays[it] = deltas[it]
        }

        rmsErrorCumulative = cumulativeRmse(totals, empirical)
        rmsErrorDelta = deltaRmse(deltas, empirical)

        masErrorCumulative = cumulativeMase(totals, empirical)
        masErrorDelta = deltaMase(deltas, empirical)
    }

    //endregion

    //region COMPUTE FUNCTIONS

    /** Current curve value. */
    fun invoke(date: LocalDate) = invoke(date.toNumber)

    /** Current curve value. */
    override fun invoke(x: Number) = when (curve) {
        LINEAR -> linear(x.toDouble())
        LOGISTIC -> logistic(x.toDouble())
        GEN_LOGISTIC -> generalLogistic(x.toDouble())
        GAUSSIAN -> gaussianErf(x.toDouble())
        GOMPERTZ -> gompertz(x.toDouble())
        else -> throw IllegalStateException()
    }

    /** Estimate derivative of curve at x. */
    fun derivative(x: Double) = 100*(invoke(x + .005) - invoke(x - .005))

    /** Linear function. */
    fun linear(x: Double) = linear(x, l.toDouble(), k.toDouble(), x0.toDouble())
    /** Compute logistic function at given # of days. */
    fun logistic(x: Double) = logistic(x, l.toDouble(), k.toDouble(), x0.toDouble())
    /** Compute generalized logistic function at given # of days. */
    fun generalLogistic(x: Double) = generalLogistic(x, l.toDouble(), k.toDouble(), x0.toDouble(), v.toDouble())
    /** Compute error function at given # of days. */
    fun gaussianErf(x: Double) = gaussianErf(x, l.toDouble(), k.toDouble(), x0.toDouble())
    /** Compute Gompertz function at given # of days. */
    fun gompertz(x: Double) = gompertz(x, l.toDouble(), k.toDouble(), x0.toDouble())

    //endregion

    //region FITTING AND STATS

    private fun empiricalDataForFitting(empirical: MetricTimeSeries?): List<Vector2D>? {
        val domain = empirical?.let { fitDateRange.intersect(empirical.domain) } ?: return null
        return domain.map { Vector2D(it.toNumber.toDouble(), empirical[it]) }
    }

    private fun empiricalDataForEvaluation(empirical: MetricTimeSeries?): List<Vector2D>? {
        val domain = empirical?.let { evalDateRange.intersect(empirical.domain) } ?: return null
        return domain.map { Vector2D(it.toNumber.toDouble(), empirical[it]) }
    }

    /**
     * Compute standard error for the cumulative (raw) time series data.
     * @param curve optional curve to evaluate
     * @param empirical the empirical data
     * @return error
     */
    fun cumulativeRmse(curve: MetricTimeSeries? = null, empirical: MetricTimeSeries?): Double? {
        val observedPoints = empiricalDataForEvaluation(empirical) ?: return null
        return rootMeanSquareError(observedPoints) { n ->
            curve?.let { it[numberToDate(n)] } ?: invoke(n)
        }
    }

    /**
     * Compute standard error for the delta (day-over-day change) of this curve compared to provided empirical data.
     * @param deltaCurve optional curve to evaluate
     * @param empirical the empirical data
     * @return error
     */
    fun deltaRmse(deltaCurve: MetricTimeSeries? = null, empirical: MetricTimeSeries?): Double? {
        val observedPoints = empiricalDataForEvaluation(empirical?.deltas()) ?: return null
        return rootMeanSquareError(observedPoints) { n ->
            deltaCurve?.let { it[numberToDate(n)] } ?: derivative(n)
        }
    }

    /**
     * Compute MAS error for the cumulative (raw) time series data.
     * @param curve optional curve to evaluate
     * @param empirical the empirical data
     * @return error
     */
    fun cumulativeMase(curve: MetricTimeSeries? = null, empirical: MetricTimeSeries?): Double? {
        val observedPoints = empiricalDataForEvaluation(empirical) ?: return null
        return meanAbsoluteScaledError(observedPoints) { n ->
            curve?.let { it[numberToDate(n)] } ?: invoke(n)
        }
    }

    /**
     * Compute MAS error for the delta (day-over-day change) of this curve compared to provided empirical data.
     * @param deltaCurve optional curve to evaluate
     * @param empirical the empirical data
     * @return error
     */
    fun deltaMase(deltaCurve: MetricTimeSeries? = null, empirical: MetricTimeSeries?): Double? {
        val observedPoints = empiricalDataForEvaluation(empirical?.deltas()) ?: return null
        return meanAbsoluteScaledError(observedPoints) { n ->
            deltaCurve?.let { it[numberToDate(n)] } ?: derivative(n)
        }
    }

    /**
     * Autofit series data using the cumulative SE.
     * Uses the current curve and the parameter vector [l, k, x0, v].
     * @param series the series to fit
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
        LINEAR -> linear(x, params[0], params[1], params[2])
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

/** Compute root mean squared error given list of points and given function. */
fun rootMeanSquareError(points: List<Vector2D>, function: (Double) -> Double)
        = sqrt(points.map { it.y - function(it.x) }.map { it * it }.sum() / points.size)

/** Compute mean absolute error given list of points and given function. */
fun meanAbsoluteError(points: List<Vector2D>, function: (Double) -> Double)
        = points.map { abs(it.y - function(it.x)) }.average()

/** Compute MAS error given list of points and given function. */
fun meanAbsoluteScaledError(points: List<Vector2D>, function: (Double) -> Double)
        = points.map { abs(it.y - function(it.x)) }.average() / (2 until points.size).map { abs(points[it].y - points[it-1].y)}.average()

operator fun Number.div(x: Double) = toDouble() / x
operator fun Number.unaryMinus() = -toDouble()
operator fun Number.times(x: Double) = toDouble() * x
operator fun Number.plus(x: Double) = toDouble() + x
operator fun Double.minus(x: Number) = this - x.toDouble()
operator fun Double.div(x: Number) = this / x.toDouble()
operator fun Int.div(x: Number) = this / x.toDouble()

//endregion