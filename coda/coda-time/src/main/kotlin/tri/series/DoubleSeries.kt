package tri.series

import kotlin.math.log2

typealias NumberSeries = List<Double>
typealias NumberSeriesOp = (NumberSeries) -> NumberSeries

fun partialSums(startAt: Double = 0.0): NumberSeriesOp = { it.partialSums(startAt) }

fun deltas(offset: Int = 1): NumberSeriesOp = { it.deltas(offset) }
fun changes(offset: Int = 1): NumberSeriesOp = { it.changes(offset) }

//region CALCULATIONS that return same size list

/** Construct partial sums of values. Optionally provide a global addend. */
fun NumberSeries.partialSums(startAt: Double = 0.0): NumberSeries {
    val res = mutableListOf<Double>()
    var sum = startAt
    forEach {
        sum += it
        res += sum
    }
    return res
}

/** Compute diffs between entries, assuming the given [valueIfNone] if there are no entries at given offset. */
fun NumberSeries.deltas(offset: Int = 1, valueIfNone: Double = 0.0) = indices.map {
    get(it) - getOrElse(it - offset) { valueIfNone }
}

//endregion

//region CALCULATIONS that return different size list

/** Compute changes from prior values, with given offset. The result has [offset] fewer entries. */
fun NumberSeries.changes(offset: Int = 1): List<Double> = (offset until size).map {
    get(it) - get(it - offset)
}

/** Compute growth rates between entries (ratio of successive entries). Can produce infinity. */
fun NumberSeries.growthRates(day0: Int = 0) = (1 until size).map {
    when (day0) {
        0 -> get(it) / get(it - 1)
        else -> (get(it) - get(maxOf(0, it - day0))) / (get(it - 1) - get(maxOf(0, it - day0)))
    }
}

/** Compute growth percentage between entries (ratio of change to average). */
fun NumberSeries.symmetricGrowth() = (1 until size).map {
    (get(it) - get(it - 1)) / (.5 * (get(it) + get(it - 1)))
}

/**
 * Compute doubling time based on constant growth rates.
 * @param sinceDaysAgo how many days ago is "day 0" for computation of growth rates.
 */
fun NumberSeries.doublingTimes(sinceDaysAgo: Int = 0) = growthRates(sinceDaysAgo).map {
    1 / log2(it)
}

//endregion

//region SLIDING WINDOW CALCULATIONS

/** Sliding window of at least n entries. Results in (size-n+1) entries. */
fun NumberSeries.slidingWindow(n: Int, includePartialList: Boolean = false) = when {
    !includePartialList -> (n..size).map { subList(it - n, it) }
    else -> indices.map { subList(kotlin.math.max(0, it - n + 1), it + 1) }
}

/** Compute average over n entries. The first n-1 entries have partial averages if [includePartialList] is true. */
fun NumberSeries.movingAverage(bucket: Int, nonZero: Boolean = false, includePartialList: Boolean = false) = slidingWindow(bucket, includePartialList).map {
    if (!nonZero) it.average() else it.filter { it != 0.0 }.average()
}
/** Compute sum over n entries. The first n-1 entries have partial sums if [includePartialList] is true. */
fun NumberSeries.movingSum(bucket: Int, includePartialList: Boolean = false) = slidingWindow(bucket, includePartialList).map { it.sum() }

/** Ratio of n days (top) over m days (bottom). */
fun NumberSeries.growthRatio(topBucket: Int, bottomBucket: Int): NumberSeries {
    val top = movingSum(topBucket, false)
    val bottom = movingSum(bottomBucket, false)
    val size = minOf(top.size, bottom.size)
    val topLast = top.takeLast(size)
    val bottomLast = bottom.takeLast(size)
    val res = topLast.mapIndexed { i, v -> v / bottomLast[i] }
    return res
}

//endregion
