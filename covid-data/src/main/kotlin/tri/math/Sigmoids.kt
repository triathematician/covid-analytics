package tri.math

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.math3.special.Erf
import kotlin.math.exp
import kotlin.math.pow

/** Stores parameters associated with a sigmoid curve. */
data class SigmoidParameters(val curve: String, val load: Double, val k: Double, val x0: Double, val v: Double?) {
    @get:JsonIgnore
    val parameters: DoubleArray
        get() = when (curve) {
            GEN_LOGISTIC -> doubleArrayOf(load, k, x0, v!!)
            else -> doubleArrayOf(load, k, x0)
        }
}

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

const val LINEAR = "Linear"
const val LOGISTIC = "Logistic"
const val GEN_LOGISTIC = "General Logistic"
const val GAUSSIAN = "Gaussian"
const val GOMPERTZ = "Gompertz"

val SIGMOID_MODELS = listOf(LINEAR, LOGISTIC, GEN_LOGISTIC, GAUSSIAN, GOMPERTZ)