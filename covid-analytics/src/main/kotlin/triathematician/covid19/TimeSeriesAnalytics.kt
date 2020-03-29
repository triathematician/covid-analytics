package triathematician.covid19

import kotlin.math.log2
import kotlin.math.max

/** Compute diffs between entries. */
fun List<Int>.changes() = (1 until size).map { get(it) - get(it - 1) }
/** Compute growth rates between entries. Can produce infinity. */
fun List<Number>.growthRates() = (1 until size).map { get(it).toDouble() / get(it - 1).toDouble() }
/** Compute doubling time based on constant growth rates. */
fun List<Number>.doublingTimes() = growthRates().map { 1/log2(it) }

/** Compute average over n entries. The first n-1 entries have partial averages. */
fun List<Number>.movingAverage(bucket: Int) = indices.map { subList(max(0, it-bucket+1), it + 1).average() }

/** Compute average of list of numbers. */
fun List<Number>.average() = sumByDouble { it.toDouble() } / size
