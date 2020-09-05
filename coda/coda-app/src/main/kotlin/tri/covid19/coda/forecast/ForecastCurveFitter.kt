package tri.covid19.coda.forecast

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.AllowedSolution
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import tornadofx.property
import tri.math.*
import tri.timeseries.Forecast
import tri.timeseries.MetricTimeSeries
import tri.util.DateRange
import tri.util.minus
import tri.util.monthDay
import java.time.LocalDate

private const val MODEL_NAME = "User"

private val K_FIT_RANGE = 0.03..0.25
private val L_FIT_RANGE = 1E1..1E7
private val X0_FIT_RANGE = 10.0..200.0
private val V_FIT_RANGE = 1E-2..1E2

internal val DAY0 = LocalDate.of(2020, 1, 1)

/** Tools for fitting forecast to empirical data. January 1, 2020 is "day 0". */
class ForecastCurveFitter: (Number) -> Double {

    //region PROPERTIES

    var curve by property(Sigmoid.GOMPERTZ)
    var l: Number by property(70000.0)
    var k: Number by property(0.07)
    var x0: Number by property(90.0)
    var v: Number by property(1.0)

    val sigmoidParameters: SigmoidParameters
        get() = SigmoidParameters(curve, l.toDouble(), k.toDouble(), x0.toDouble(), v.toDouble())

    private val now
        get() = LocalDate.now()
    val nowInt
        get() = dateToNumber(now)

    var firstFitDay: Number by property(nowInt - 20)
    var lastFitDay: Number by property(nowInt - 7)
    private val fitDateRange: DateRange
        get() = DateRange(firstFitDay.toDate, lastFitDay.toDate)

    var fitCumulative: Boolean by property(true)

    var firstEvalDay: Number by property(nowInt - 6)
    var lastEvalDay: Number by property(nowInt)
    private val evalDateRange: DateRange
        get() = DateRange(firstEvalDay.toDate, lastEvalDay.toDate)

    var fitLabel by property("Automatically fit curves based on historical data.")

    val equation
        get() = when (curve) {
            Sigmoid.LINEAR -> String.format("%.2f * (1 + %.2f * (x - %.2f)) / 2", l, k, x0)
            Sigmoid.QUADRATIC -> String.format("%.2f * (x - %.2f)^2 + %.2f", k, x0, l)
            Sigmoid.LOGISTIC -> String.format("%.2f / (1 + e^(-%.3f * (x - %.2f)))", l, k, x0)
            Sigmoid.GEN_LOGISTIC -> String.format("%.2f / (1 + e^(-%.3f * (x - %.2f)))^(1/%.2f)", l, k, x0, v)
            Sigmoid.GAUSSIAN -> String.format("%.2f * (1 + erf(-%.3f (x - %.2f)))/2", l, k, x0)
            Sigmoid.GOMPERTZ -> String.format("%.2f * e^(-e^(-%.3f (x - %.2f)))", l, k, x0)
        }

    //endregion

    //region PROPERTY UPDATES

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
        val f = Forecast(MODEL_NAME, LocalDate.now(), empirical.area, empirical.metric, listOf(series))

        return ForecastStats(f).apply {
            sigmoidParameters = this@ForecastCurveFitter.sigmoidParameters
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

    /** Current curve value. */
    operator fun invoke(date: LocalDate, shift: Double) = invoke(date.toNumber + shift)

    /** Current curve value. */
    override fun invoke(x: Number) = when (curve) {
        Sigmoid.LINEAR -> linear(x.toDouble())
        Sigmoid.QUADRATIC -> quadratic(x.toDouble())
        Sigmoid.LOGISTIC -> logistic(x.toDouble())
        Sigmoid.GEN_LOGISTIC -> generalLogistic(x.toDouble())
        Sigmoid.GAUSSIAN -> gaussianErf(x.toDouble())
        Sigmoid.GOMPERTZ -> gompertz(x.toDouble())
    }

    /** Estimate derivative of curve at x. */
    fun derivative(x: Double) = 100*(invoke(x + .005) - invoke(x - .005))

    /** Linear function. */
    fun linear(x: Double) = linear(x, l.toDouble(), k.toDouble(), x0.toDouble())
    /** Quadratic function. */
    fun quadratic(x: Double) = quadratic(x, l.toDouble(), k.toDouble(), x0.toDouble())

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

    fun autofit(series: MetricTimeSeries?) {
        updateFitLabel()

        if (series != null) {
            val validator = if (curve == Sigmoid.QUADRATIC)
                ParameterValidator { v ->
                    vec(v[0].coerceIn(-1E5, 1E5), v[1].coerceIn(0.0..2.0), v[2].coerceIn(-1E2, 1E2), v[3])
                }
            else ParameterValidator { v ->
                vec(v[0].coerceIn(L_FIT_RANGE), v[1].coerceIn(K_FIT_RANGE), v[2].coerceIn(X0_FIT_RANGE), v[3].coerceIn(V_FIT_RANGE))
            }
            val params = when (fitCumulative) {
                true -> SigmoidCurveFitting.fitCumulative(curve, empiricalDataForFitting(series)!!, sigmoidParameters, validator)
                else -> SigmoidCurveFitting.fitIncidence(curve, empiricalDataForFitting(series)!!, sigmoidParameters, validator)
            }
            l = params.load
            k = params.k
            x0 = params.x0
            params.v?.let { v = it }
        }
    }

    internal fun empiricalDataForFitting(empirical: MetricTimeSeries?): List<Vector2D>? {
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

    //endregion

}