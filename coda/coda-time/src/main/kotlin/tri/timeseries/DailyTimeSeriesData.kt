package tri.timeseries

import tri.series.DoubleSeries
import tri.series.NullableDoubleSeries
import tri.series.cumulativeSums
import tri.series.notNull
import tri.util.DateRange
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** [TimeSeriesData] with values for a consecutive series of dates. */
abstract class DailyTimeSeries<V>(_defaultValue: V?, _start: LocalDate, _values: List<V>, _initialCumulativeValue: V?) :
        IndexedTimeSeries<LocalDate, V>(_defaultValue, _start, _values, _initialCumulativeValue) {

    override fun date(i: Int) = start.plusDays(i.toLong())!!
    override fun indexOf(date: LocalDate) = ChronoUnit.DAYS.between(start, date).toInt()

    override val domain = DateRange(start, end)

}

/** [TimeSeriesData] with a consecutive region of dates, with double values. */
class DailyTimeDoubleSeries(_defaultValue: Double?, _start: LocalDate, _values: DoubleSeries, _initialCumulativeValue: Double?) : DailyTimeSeries<Double>(_defaultValue, _start, _values, _initialCumulativeValue) {

    override fun cumulative(): DailyTimeDoubleSeries {
        if (!supportsCumulative) throw TimeSeriesDataException("Cumulative data not supported.")
        return try {
            DailyTimeDoubleSeries(null, start, values.cumulativeSums(initialCumulativeValue!!), null)
        } catch (x: Exception) {
            throw TimeSeriesDataException("Data cannot be aggregated", x)
        }
    }

}

/** [TimeSeriesData] with a consecutive region of dates, with nullable double values. */
class DailyTimeNullableDoubleSeries(_defaultValue: Double?, _start: LocalDate, _values: NullableDoubleSeries, _initialCumulativeValue: Double?) : DailyTimeSeries<Double?>(_defaultValue, _start, _values, _initialCumulativeValue) {

    /** Gets this with assured non-null values. Requires either a non-null default value, or no missing values. */
    @Throws(TimeSeriesDataException::class)
    fun nonNull(): DailyTimeDoubleSeries {
        if (defaultValue == null && null in values) throw TimeSeriesDataException("Cannot make non-nullable: missing values and no default.")

        return DailyTimeDoubleSeries(defaultValue, start, values.notNull(), initialCumulativeValue)
    }

    override fun cumulative() = nonNull().cumulative()

}