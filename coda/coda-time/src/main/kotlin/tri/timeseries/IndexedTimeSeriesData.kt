package tri.timeseries

import tri.series.DoubleSeries
import tri.series.NullableDoubleSeries
import tri.series.cumulativeSums
import tri.series.notNull
import java.time.LocalDate
import java.time.temporal.Temporal

/** [TimeSeriesData] with values for a consecutive series of dates. */
abstract class IndexedTimeSeries<X, V>(
        override val defaultValue: V?,
        val start: X,
        val values: List<V>,
        /** Cumulative values start here; if null, cumulative is not supported. */
        val initialCumulativeValue: V?,
) : TimeSeriesData<X, V> where X : Temporal, X : Comparable<X> {

    val size = values.size
    val end = date(values.size - 1)

    override val supportsCumulative = initialCumulativeValue != null

    override fun value(date: X): V? = values.getOrElse(indexOf(date)) { defaultValue }

    //region DATE INDICES

    /** Get date by index. */
    abstract fun date(i: Int): X
    /** Get index by date. */
    abstract fun indexOf(date: X): Int

    //endregion
}
