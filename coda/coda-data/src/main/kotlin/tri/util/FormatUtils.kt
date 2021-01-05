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
package tri.util

import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.round

/** Format number with given number of digits. */
fun Number.format(digits: Int) = "%.${digits}f".format(this)

/** Format a number with digits for presenting to user. */
fun Number.userFormat(): String {
    val x = toDouble().absoluteValue
    return when {
        this is Int || this is Long -> NumberFormat.getNumberInstance(Locale.US).format(this)
        x < 0.000001 -> "0"
        x >= 1000 -> NumberFormat.getNumberInstance(Locale.US).format(this.nearestInt)
        x >= 10.0 -> this.nearestInt.toString()
        x >= 1.0 -> format(1)
        x < 0.01 -> format(3)
        else -> format(2)
    }
}

/** Integer/long nearest a number. */
private val Number.nearestInt
    get() = if (this.toLong().absoluteValue > Int.MAX_VALUE) round(toDouble()).toLong() else round(toDouble()).toInt()
/** Format a number as a percentage. */
fun Number.percentFormat(digits: Int = 0) = NumberFormat.getPercentInstance().also { it.minimumFractionDigits = digits }.format(this)

fun String.javaTrim() = trim { it <= ' ' }
fun String?.nonBlankOrNull() = if (this.isNullOrBlank()) null else this

/** Formats integers using given range of digits. */
fun numberFormat(integerDigitRange: IntRange) = NumberFormat.getInstance().apply {
    minimumIntegerDigits = integerDigitRange.first
    maximumFractionDigits = integerDigitRange.last
}
