/*-
 * #%L
 * coda-data
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
package tri.math

import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector

typealias Jacobian = org.apache.commons.math3.util.Pair<RealVector, RealMatrix>

operator fun RealVector.get(i: Int) = getEntry(i)
operator fun RealVector.plus(v2: RealVector) = this.add(v2)
operator fun RealVector.minus(v2: RealVector) = this.subtract(v2)
operator fun RealVector.div(x: Double) = this.mapDivide(x)
operator fun Double.times(v: RealVector) = v.mapMultiply(this)

fun vec(vararg x: Number) = listOf(*x).map { it.toDouble() }.toDoubleArray().let { ArrayRealVector(it) }
