package tri.timeseries

import tri.series.DoubleSeries
import tri.series.NullableDoubleSeries
import tri.series.cumulativeSums
import tri.series.notNull
import tri.util.YearMonthRange
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** [TimeSeriesData] with values for a consecutive series of dates. */
abstract class MonthlyTimeSeriesData<V>(_defaultValue: V?, _start: YearMonth, _values: List<V>, _initialCumulativeValue: V?) :
        IndexedTimeSeriesData<YearMonth, V>(_defaultValue, _start, _values, _initialCumulativeValue) {

    override fun date(i: Int) = start.plusMonths(i.toLong())!!
    override fun indexOf(date: YearMonth) = ChronoUnit.MONTHS.between(start, date).toInt()

    override val domain = YearMonthRange(start, end)

}

/** [TimeSeriesData] with a consecutive region of dates, with double values. */
class MonthlyTimeSeriesDoubleData(_defaultValue: Double?, _start: YearMonth, _values: DoubleSeries, _initialCumulativeValue: Double?) : MonthlyTimeSeriesData<Double>(_defaultValue, _start, _values, _initialCumulativeValue) {

    override fun cumulative(): MonthlyTimeSeriesDoubleData {
        if (!supportsCumulative) throw TimeSeriesDataException("Cumulative data not supported.")
        return try {
            MonthlyTimeSeriesDoubleData(null, start, values.cumulativeSums(initialCumulativeValue!!), null)
        } catch (x: Exception) {
            throw TimeSeriesDataException("Data cannot be aggregated", x)
        }
    }

}

/** [TimeSeriesData] with a consecutive region of dates, with nullable double values. */
class MonthlyTimeSeriesNullableDoubleData(_defaultValue: Double?, _start: YearMonth, _values: NullableDoubleSeries, _initialCumulativeValue: Double?) : MonthlyTimeSeriesData<Double?>(_defaultValue, _start, _values, _initialCumulativeValue) {

    /** Gets this with assured non-null values. Requires either a non-null default value, or no missing values. */
    @Throws(TimeSeriesDataException::class)
    fun nonNull(): MonthlyTimeSeriesDoubleData {
        if (defaultValue == null && null in values) throw TimeSeriesDataException("Cannot make non-nullable: missing values and no default.")

        return MonthlyTimeSeriesDoubleData(defaultValue, start, values.notNull(), initialCumulativeValue)
    }

    override fun cumulative() = nonNull().cumulative()

}