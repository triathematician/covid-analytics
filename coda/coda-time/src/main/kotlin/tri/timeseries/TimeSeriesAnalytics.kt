/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
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
package tri.timeseries

import kotlin.math.log2
import kotlin.math.max

/** Sliding window of at least n entries. Results in (size-n+1) entries. */
fun List<Double>.slidingWindow(n: Int, includePartialList: Boolean = false) = when {
    !includePartialList -> (n..size).map { subList(it - n, it) }
    else -> indices.map { subList(max(0, it - n + 1), it + 1) }
}

/** Compute diffs between entries. */
fun List<Double>.deltas(offset: Int = 1) = (0 until size).map {
    get(it) - getOrElse(it - offset) { 0.0 }
}

/** Construct partial sums of values. */
fun List<Double>.partialSums(): List<Double> {
    val res = mutableListOf<Double>()
    var sum = 0.0
    forEach {
        sum += it
        res += sum
    }
    return res
}

/** Compute changes from prior values, with given offset. The result has [offset] fewer entries. */
fun List<Double>.changes(offset: Int = 1): List<Double> = (offset until size).map { get(it) - get(it - offset) }

/**
 * Compute percent change from last to next.
 * Result will be [Double.NaN] if dividing 0 by 0, or [Double.POSITIVE_INFINITY]/[Double.NEGATIVE_INFINITY] if dividing by 0 otherwise.
 */
fun List<Double>.percentChanges(offset: Int = 1): List<Double> = (0 until size).map {
    (getOrElse(it - offset) { Double.NaN }).percentChangeTo(get(it))
}

/** Compute growth rates between entries (ratio of successive entries). Can produce infinity. */
fun List<Double>.growthRates(day0: Int = 0) = (1 until size).map {
    when (day0) {
        0 -> get(it) / get(it - 1)
        else -> (get(it) - get(maxOf(0, it-day0))) / (get(it - 1) - get(maxOf(0, it-day0)))
    }
}

/** Compute growth percentage between entries (ratio of change to average). */
fun List<Double>.symmetricGrowth() = (1 until size).map { (get(it) - get(it - 1)) / (.5 * (get(it) + get(it - 1))) }

/**
 * Compute doubling time based on constant growth rates.
 * @param sinceDaysAgo how many days ago is "day 0" for computation of growth rates.
 */
fun List<Double>.doublingTimes(sinceDaysAgo: Int = 0) = growthRates(sinceDaysAgo).map {
    1/log2(it)
}

/** Compute average over n entries. The first n-1 entries have partial averages if [includePartialList] is true. */
fun List<Double>.movingAverage(bucket: Int, nonZero: Boolean = false, includePartialList: Boolean = false) = slidingWindow(bucket, includePartialList).map {
    if (!nonZero) it.average() else it.filter { it != 0.0 }.average()
}
/** Compute sum over n entries. The first n-1 entries have partial sums if [includePartialList] is true. */
fun List<Double>.movingSum(bucket: Int, includePartialList: Boolean = false) = slidingWindow(bucket, includePartialList).map { it.sum() }
/** Ratio of n days (top) over m days (bottom). */
fun List<Double>.growthRatio(topBucket: Int, bottomBucket: Int): List<Double> {
    val top = movingSum(topBucket, false)
    val bottom = movingSum(bottomBucket, false)
    val size = minOf(top.size, bottom.size)
    val topLast = top.takeLast(size)
    val bottomLast = bottom.takeLast(size)
    val res = topLast.mapIndexed { i, v -> v / bottomLast[i] }
    return res
}

/** Compute percentage change from this value to the provided value. */
fun Double.percentChangeTo(count: Double) = (count - this) / this