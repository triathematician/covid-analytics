/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import kotlin.math.abs
import kotlin.math.sqrt

//region ERRORS

/** Compute root mean squared error given list of points and given function. */
fun rootMeanSquareError(points: List<Vector2D>, function: (Double) -> Double)
        = sqrt(points.map { it.y - function(it.x) }.map { it * it }.sum() / points.size)

/** Compute mean absolute error given list of points and given function. */
fun meanAbsoluteError(points: List<Vector2D>, function: (Double) -> Double)
        = points.map { abs(it.y - function(it.x)) }.average()

/** Compute MAS error given list of points and given function. */
fun meanAbsoluteScaledError(points: List<Vector2D>, function: (Double) -> Double)
        = points.map { abs(it.y - function(it.x)) }.average() / (2 until points.size).map { abs(points[it].y - points[it-1].y) }.average()

//endregion

//region MATH UTILS

operator fun Number.div(x: Double) = toDouble() / x
operator fun Number.unaryMinus() = -toDouble()
operator fun Number.times(x: Double) = toDouble() * x
operator fun Number.plus(x: Double) = toDouble() + x
operator fun Double.minus(x: Number) = this - x.toDouble()
operator fun Double.div(x: Number) = this / x.toDouble()
operator fun Int.div(x: Number) = this / x.toDouble()

//endregion
