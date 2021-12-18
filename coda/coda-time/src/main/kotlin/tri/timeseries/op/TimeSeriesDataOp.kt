package tri.timeseries.op

import tri.timeseries.IndexedTimeSeriesData
import tri.timeseries.TimeSeriesData
import tri.util.DateRange
import tri.util.YearMonthRange
import tri.util.YearRange
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.Temporal

/** Alters data within a single time series. */
typealias TimeSeriesDataOp<X, V> = (TimeSeriesData<X, V>) -> TimeSeriesData<X, V>

/** Combines data from two time series. */
typealias TimeSeriesDataOp2<X, V> = (TimeSeriesData<X, V>, TimeSeriesData<X, V>) -> TimeSeriesData<X, V>

/** Aggregates data from many time series. */
typealias TimeSeriesDataAggregator<X, V> = (List<TimeSeriesData<X, V>>) -> TimeSeriesData<X, V>


// MERGE OPERATIONS

/** Reduces time series by given operation, using the operator to collapse values across all series into a single value. */
fun <X, V> List<IndexedTimeSeriesData<X, V>>.mergeValues(op: (List<V>) -> V): List<V> where X : Temporal, X : Comparable<X> {
    return dateRange().map { date -> op(map { it.value(date)!! }) }
}

fun <X> List<IndexedTimeSeriesData<X, *>>.dateRange(): Iterable<X> where X : Temporal, X : Comparable<X> {
    return map { it.domain }.union()
}

fun <X : Comparable<X>> List<ClosedRange<X>>.union(): Iterable<X> where X : Temporal {
    if (isEmpty()) throw IndexOutOfBoundsException()
    return iterate(minOf { it.start }, maxOf { it.endInclusive })
}

private fun <X : Temporal> iterate(from: X, to: X) = when (from) {
    is LocalDate -> DateRange((from as LocalDate)..(to as LocalDate))
    is YearMonth -> YearMonthRange((from as YearMonth)..(to as YearMonth))
    is Year -> YearRange((from as Year)..(to as Year))
    else -> throw UnsupportedOperationException()
} as Iterable<X>

//endregion