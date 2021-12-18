package tri.series

import kotlin.math.log2

typealias DoubleSeries = List<Double>
typealias NullableDoubleSeries = List<Double?>
typealias DoubleSeriesOp = (DoubleSeries) -> DoubleSeries

//region BASIC OPERATORS

fun cumulativeSums(startAt: Double = 0.0): DoubleSeriesOp = { it.cumulativeSums(startAt) }

fun deltas(offset: Int = 1, valueIfNone: Double = 0.0): DoubleSeriesOp = { it.deltas(offset, valueIfNone) }
fun changes(offset: Int = 1): DoubleSeriesOp = { it.changes(offset) }

//endregion

//region CALCULATIONS that return same size list

/** Casts to a series with non-null values. */
@Throws(NullPointerException::class)
fun NullableDoubleSeries.notNull() : DoubleSeries = map { it!! }

/** Series of the same size whose entries are cumulative sums. */
fun DoubleSeries.cumulativeSums(startAt: Double = 0.0): DoubleSeries {
    val res = ArrayList<Double>(size)
    var sum = startAt
    forEach {
        sum += it
        res += sum
    }
    return res
}

/** Compute diffs between entries, assuming the given [valueIfNone] if there are no entries at given offset. */
fun DoubleSeries.deltas(offset: Int = 1, valueIfNone: Double = 0.0) = indices.map {
    get(it) - getOrElse(it - offset) { valueIfNone }
}

//endregion

//region CALCULATIONS that return different size list

/** Compute changes from prior values, with given offset. The result has [offset] fewer entries. */
fun DoubleSeries.changes(offset: Int = 1): List<Double> = (offset until size).map {
    get(it) - get(it - offset)
}

/** Compute growth rates between entries (ratio of successive entries). Can produce infinity. */
fun DoubleSeries.growthRates(day0: Int = 0) = (1 until size).map {
    when (day0) {
        0 -> get(it) / get(it - 1)
        else -> (get(it) - get(maxOf(0, it - day0))) / (get(it - 1) - get(maxOf(0, it - day0)))
    }
}

/** Compute growth percentage between entries (ratio of change to average). */
fun DoubleSeries.symmetricGrowth() = (1 until size).map {
    (get(it) - get(it - 1)) / (.5 * (get(it) + get(it - 1)))
}

/**
 * Compute doubling time based on constant growth rates.
 * @param sinceDaysAgo how many days ago is "day 0" for computation of growth rates.
 */
fun DoubleSeries.doublingTimes(sinceDaysAgo: Int = 0) = growthRates(sinceDaysAgo).map {
    1 / log2(it)
}

//endregion

//region SLIDING WINDOW CALCULATIONS

/** Sliding window of at least n entries. Results in (size-n+1) entries. */
fun DoubleSeries.slidingWindow(n: Int, includePartialList: Boolean = false) = when {
    !includePartialList -> (n..size).map { subList(it - n, it) }
    else -> indices.map { subList(kotlin.math.max(0, it - n + 1), it + 1) }
}

/** Compute average over n entries. The first n-1 entries have partial averages if [includePartialList] is true. */
fun DoubleSeries.movingAverage(bucket: Int, nonZero: Boolean = false, includePartialList: Boolean = false) = slidingWindow(bucket, includePartialList).map {
    if (!nonZero) it.average() else it.filter { it != 0.0 }.average()
}
/** Compute sum over n entries. The first n-1 entries have partial sums if [includePartialList] is true. */
fun DoubleSeries.movingSum(bucket: Int, includePartialList: Boolean = false) = slidingWindow(bucket, includePartialList).map { it.sum() }

/** Ratio of n days (top) over m days (bottom). */
fun DoubleSeries.growthRatio(topBucket: Int, bottomBucket: Int): DoubleSeries {
    val top = movingSum(topBucket, false)
    val bottom = movingSum(bottomBucket, false)
    val size = minOf(top.size, bottom.size)
    val topLast = top.takeLast(size)
    val bottomLast = bottom.takeLast(size)
    return topLast.mapIndexed { i, v -> v / bottomLast[i] }
}

//endregion
