package tri.timeseries

import java.time.temporal.Temporal

/**
 * Data associated with a time series. Supports retrieving a value (and potentially a cumulative value) by date.
 *
 * Implementations may vary:
 *  - Values may be stored as lists, corresponding to a sequence of periodic dates, or in sparse tables.
 *  - Values may be either integers or doubles.
 *  - Missing values may be interpreted as nulls, zeros, or some other default value.
 *  - Cumulative values may or may not be supported; if they are, a single value is used to specify entries with "unknown" times.
 *
 * @param <X> date type
 * @param <V> value type
 */
interface TimeSeriesData<X, out V> where X : Temporal, X : Comparable<X> {

    /** Whether the series supports cumulative values. */
    val supportsCumulative: Boolean

    /** Assumed value when not within the range. */
    val defaultValue: V?

    /** Range of dates with relevant values. */
    val domain: ClosedRange<X>

    /** Get value by date. Should return null if there is no data for the date. */
    fun value(date: X): V?

    /** Get cumulative series. Should throw an exception if the value is not supported, or cannot be computed. */
    @Throws(TimeSeriesDataException::class)
    fun cumulative(): TimeSeriesData<X, V>

}