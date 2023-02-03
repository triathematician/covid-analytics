/*-
 * #%L
 * coda-time-0.4.0-SNAPSHOT
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
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
package tri.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

//region CALCULATIONS on [Double] and [Float]

/** Compute percentage change from this value to the provided value. */
fun Double.percentChangeTo(count: Double) = (count - this) / this

/** Rounds number to nearest value with given digits of precision. */
fun Double.round(digits: Int) = when (digits) {
    0 -> roundToInt().toDouble()
    1 -> (this * 10).roundToInt() / 10.0
    2 -> (this * 100).roundToInt() / 100.0
    3 -> (this * 1000).roundToInt() / 1000.0
    else -> (this * 10.0.pow(digits)).roundToInt() / 10.0.pow(digits)
}

/** Rounds number to nearest value with given digits of precision. */
fun Double.roundUp(digits: Int) = when (digits) {
    0 -> roundToInt().toDouble()
    1 -> (this * 10).roundToInt() / 10.0
    2 -> (this * 100).roundToInt() / 100.0
    3 -> (this * 1000).roundToInt() / 1000.0
    else -> ceil(this * 10.0.pow(digits)) / 10.0.pow(digits)
}

/** Rounds number to nearest value with given digits of precision. */
fun Double.roundDown(digits: Int) = when (digits) {
    0 -> roundToInt().toDouble()
    1 -> (this * 10).roundToInt() / 10.0
    2 -> (this * 100).roundToInt() / 100.0
    3 -> (this * 1000).roundToInt() / 1000.0
    else -> floor(this * 10.0.pow(digits)) / 10.0.pow(digits)
}

//endregion

//region NULLABLE CALCULATIONS

/** Utility converting NaN's where present to 0. */
fun Double?.nanToZero(): Double? = if (this != null && isNaN()) 0.0 else this

/** Compute percentage change from this value to the provided value. */
fun Double?.pctChangeTo(other: Double?) = when {
    this == null || other == null -> null
    this == 0.0 && other == 0.0 -> 0.0
    else -> percentChangeTo(other)
}

/** Calculate absolute change, with support for nullables. */
fun Double?.absChangeTo(other: Double?) = when {
    this == null || other == null -> null
    else -> other - this
}

//endregion
