package tri.math

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealVector
import org.apache.commons.math3.special.Erf
import kotlin.math.exp
import kotlin.math.pow

const val LINEAR = "Linear"
const val LOGISTIC = "Logistic"
const val GEN_LOGISTIC = "General Logistic"
const val GAUSSIAN = "Gaussian"
const val GOMPERTZ = "Gompertz"

val SIGMOID_MODELS = listOf(LINEAR, LOGISTIC, GEN_LOGISTIC, GAUSSIAN, GOMPERTZ)

/** Compute linear function. */
fun linear(x: Double, l: Double, k: Double, x0: Double) = maxOf(0.0, .5 * l * (1 + k * (x - x0)))
/** Compute logistic function. */
fun logistic(x: Double, l: Double, k: Double, x0: Double) = l / (1 + exp(-k * (x - x0)))
/** Compute generalized logistic function. */
fun generalLogistic(x: Double, l: Double, k: Double, x0: Double, v: Double) = l * (1 + exp(-k * (x - x0))).pow(-1 / v)
/** Compute error function. */
fun gaussianErf(x: Double, l: Double, k: Double, x0: Double) = l * (1 + Erf.erf(k * (x - x0))) / 2.0
/** Compute Gompertz function. */
fun gompertz(x: Double, l: Double, k: Double, x0: Double) = l * exp(-exp(-k * (x - x0)))

/** Stores parameters associated with a sigmoid curve. */
data class SigmoidParameters(val curve: String, val load: Double, val k: Double, val x0: Double, val v: Double?) {
    @get:JsonIgnore
    val parameters: DoubleArray
        get() = when (curve) {
            GEN_LOGISTIC -> doubleArrayOf(load, k, x0, v!!)
            else -> doubleArrayOf(load, k, x0)
        }
}

object SigmoidCurveFitting {

    /**
     * Fit cumulative series data using RMSE.
     * Uses the current curve and the parameter vector [l, k, x0, v].
     * @param shape the sigmoid curve
     * @param observedPoints the observed data points
     * @param initial initial parameters for fit
     * @param parameterValidator constrains parameters
     */
    fun fitCumulative(shape: String, observedPoints: List<Vector2D>, initial: SigmoidParameters, parameterValidator: ParameterValidator): SigmoidParameters {
        val observedTarget = observedPoints.map { it.y }.toDoubleArray()

        val problem = LeastSquaresBuilder()
                .start(vec(initial.load, initial.k, initial.x0, initial.v ?: 0))
                .model(solverFunCumulative(shape, observedPoints))
                .target(observedTarget)
                .maxEvaluations(100000)
                .maxIterations(100000)
                .parameterValidator(parameterValidator)
                .build()

        val optimum = LevenbergMarquardtOptimizer()
                .withCostRelativeTolerance(1.0e-9)
                .withParameterRelativeTolerance(1.0e-9)
                .optimize(problem)

        println("Cumulative fit parameters: ${optimum.point}")
        val optimalValues = optimum.point.toArray()
        return SigmoidParameters(shape, optimalValues[0], optimalValues[1], optimalValues[2], optimalValues[3])
    }

    /**
     * Fit incidence data using RMSE.
     * Uses the current curve and the parameter vector [l, k, x0, v].
     * @param shape the sigmoid curve
     * @param observedPoints the observed data points
     * @param initial initial parameters for fit
     * @param parameterValidator constrains parameters
     */
    fun fitIncidence(shape: String, observedPoints: List<Vector2D>, initial: SigmoidParameters, parameterValidator: ParameterValidator): SigmoidParameters {
        val observedDeltas = (1 until observedPoints.size).map { Vector2D(observedPoints[it].x, observedPoints[it].y - observedPoints[it-1].y)}
        val observedTarget = observedDeltas.map { it.y }.toDoubleArray()

        val problem = LeastSquaresBuilder()
                .start(vec(initial.load, initial.k, initial.x0, initial.v ?: 0))
                .model(solverFunIncidence(shape, observedDeltas))
                .target(observedTarget)
                .maxEvaluations(100000)
                .maxIterations(100000)
                .parameterValidator(parameterValidator)
                .build()

        val optimum = LevenbergMarquardtOptimizer()
                .withCostRelativeTolerance(1.0e-9)
                .withParameterRelativeTolerance(1.0e-9)
                .optimize(problem)

        println("Incidence fit parameters: ${optimum.point}")
        val optimalValues = optimum.point.toArray()
        return SigmoidParameters(shape, optimalValues[0], optimalValues[1], optimalValues[2], optimalValues[3])
    }

    /** Function used for fitting cumulative curve around the given observed points. */
    private fun solverFunCumulative(shape: String, observedPoints: List<Vector2D>) = MultivariateJacobianFunction { params ->
        val values = observedPoints.map { curve(shape, it.x, params) }

        val jacobian = Array2DRowRealMatrix(observedPoints.size, 4)
        observedPoints.forEachIndexed { i, o ->
            jacobian.setEntry(i, 0, curvePartial(shape, o.x, params, vec(1, 0, 0, 0)))
            jacobian.setEntry(i, 1, curvePartial(shape, o.x, params, vec(0, 1, 0, 0)))
            jacobian.setEntry(i, 2, curvePartial(shape, o.x, params, vec(0, 0, 1, 0)))
            jacobian.setEntry(i, 3, curvePartial(shape, o.x, params, vec(0, 0, 0, 1)))
        }

        Jacobian(ArrayRealVector(values.toDoubleArray()), jacobian)
    }

    /** Function used for fitting incidence curve around the given observed points. */
    private fun solverFunIncidence(shape: String, observedPoints: List<Vector2D>) = MultivariateJacobianFunction { params ->
        val values = observedPoints.map { curveDerivative(shape, it.x, params) }

        val jacobian = Array2DRowRealMatrix(observedPoints.size, 4)
        observedPoints.forEachIndexed { i, o ->
            jacobian.setEntry(i, 0, curveDerivativePartial(shape, o.x, params, vec(1, 0, 0, 0)))
            jacobian.setEntry(i, 1, curveDerivativePartial(shape, o.x, params, vec(0, 1, 0, 0)))
            jacobian.setEntry(i, 2, curveDerivativePartial(shape, o.x, params, vec(0, 0, 1, 0)))
            jacobian.setEntry(i, 3, curveDerivativePartial(shape, o.x, params, vec(0, 0, 0, 1)))
        }

        Jacobian(ArrayRealVector(values.toDoubleArray()), jacobian)
    }

    /** Compute curve for explicit set of parameters. */
    private fun curve(shape: String, x: Double, params: RealVector) = when(shape) {
        LINEAR -> linear(x, params[0], params[1], params[2])
        LOGISTIC -> logistic(x, params[0], params[1], params[2])
        GEN_LOGISTIC -> generalLogistic(x, params[0], params[1], params[2], params[3])
        GAUSSIAN -> gaussianErf(x, params[0], params[1], params[2])
        GOMPERTZ -> gompertz(x, params[0], params[1], params[2])
        else -> throw IllegalStateException()
    }

    /** Compute curve for explicit set of parameters. */
    private fun curveDerivative(shape: String, x: Double, params: RealVector)
            = curve(shape, x + .5, params) - curve(shape, x - .5, params)

    /** Compute partial derivative in given direction. */
    private fun curvePartial(shape: String, x: Double, params: RealVector, delta: RealVector)
            = (curve(shape, x, params + .0005*delta.unitVector()) - curve(shape, x, params - .0005*delta.unitVector())) / 1000.0

    /** Compute second partial derivative in given direction. */
    private fun curveDerivativePartial(shape: String, x: Double, params: RealVector, delta: RealVector)
            = (curveDerivative(shape, x, params + .005*delta.unitVector()) - curveDerivative(shape, x, params - .005*delta.unitVector())) / 100.0

}