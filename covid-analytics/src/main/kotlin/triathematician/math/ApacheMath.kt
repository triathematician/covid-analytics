package triathematician.math

import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector
import tornadofx.Vector2D
import kotlin.math.sqrt

typealias Jacobian = org.apache.commons.math3.util.Pair<RealVector, RealMatrix>

operator fun RealVector.get(i: Int) = getEntry(i)
operator fun RealVector.plus(v2: RealVector) = this.add(v2)
operator fun RealVector.minus(v2: RealVector) = this.subtract(v2)
operator fun RealVector.div(x: Double) = this.mapDivide(x)
operator fun Double.times(v: RealVector) = v.mapMultiply(this)

fun vec(vararg x: Number) = listOf(*x).map { it.toDouble() }.toDoubleArray().let { ArrayRealVector(it) }
