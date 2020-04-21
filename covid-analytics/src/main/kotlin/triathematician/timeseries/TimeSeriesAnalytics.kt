package triathematician.timeseries

import kotlin.math.log2
import kotlin.math.max

/** Sliding window of at least n entries. Results in (size-n+1) entries. */
fun List<Double>.slidingWindow(n: Int, includePartialList: Boolean = false) = when {
    includePartialList -> (n..size).map { subList(it - n, it) }
    else -> indices.map { subList(max(0, it - n + 1), it + 1) }
}

/** Compute diffs between entries. */
fun List<Double>.deltas() = (1 until size).map { get(it) - get(it - 1) }
/** Compute growth rates between entries (ratio of successive entries). Can produce infinity. */
fun List<Double>.growthRates() = (1 until size).map { get(it) / get(it - 1) }
/** Compute growth percentage between entries (ratio of change to total). */
fun List<Double>.growthPercentages() = (1 until size).map { (get(it) - get(it - 1)) / get(it) }

/** Compute doubling time based on constant growth rates. */
fun List<Double>.doublingTimes() = growthRates().map { 1/log2(it) }

/** Compute average over n entries. The first n-1 entries have partial averages. */
fun List<Double>.movingAverage(bucket: Int, includePartialList: Boolean = true) = slidingWindow(bucket, includePartialList).map { it.average() }

/** Compute average of list of numbers. */
fun List<Double>.average() = sumByDouble { it.toDouble() } / size
