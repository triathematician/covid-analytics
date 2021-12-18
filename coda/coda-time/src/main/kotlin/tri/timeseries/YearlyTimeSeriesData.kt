package tri.timeseries

import tri.series.DoubleSeries
import tri.series.NullableDoubleSeries
import tri.series.cumulativeSums
import tri.series.notNull
import tri.util.YearRange
import java.time.Year
import java.time.temporal.ChronoUnit

/** [TimeSeriesData] with values for a consecutive series of dates. */
abstract class YearlyTimeSeriesData<V>(_defaultValue: V?, _start: Year, _values: List<V>, _initialCumulativeValue: V?) :
        IndexedTimeSeriesData<Year, V>(_defaultValue, _start, _values, _initialCumulativeValue) {

    override fun date(i: Int) = start.plusYears(i.toLong())!!
    override fun indexOf(date: Year) = ChronoUnit.YEARS.between(start, date).toInt()

    override val domain = YearRange(start, end)

}

/** [TimeSeriesData] with a consecutive region of dates, with double values. */
class YearlyTimeSeriesDoubleData(_defaultValue: Double?, _start: Year, _values: DoubleSeries, _initialCumulativeValue: Double?) : YearlyTimeSeriesData<Double>(_defaultValue, _start, _values, _initialCumulativeValue) {

    override fun cumulative(): YearlyTimeSeriesDoubleData {
        if (!supportsCumulative) throw TimeSeriesDataException("Cumulative data not supported.")
        return try {
            YearlyTimeSeriesDoubleData(null, start, values.cumulativeSums(initialCumulativeValue!!), null)
        } catch (x: Exception) {
            throw TimeSeriesDataException("Data cannot be aggregated", x)
        }
    }

}

/** [TimeSeriesData] with a consecutive region of dates, with nullable double values. */
class YearlyTimeSeriesNullableDoubleData(_defaultValue: Double?, _start: Year, _values: NullableDoubleSeries, _initialCumulativeValue: Double?) : YearlyTimeSeriesData<Double?>(_defaultValue, _start, _values, _initialCumulativeValue) {

    /** Gets this with assured non-null values. Requires either a non-null default value, or no missing values. */
    @Throws(TimeSeriesDataException::class)
    fun nonNull(): YearlyTimeSeriesDoubleData {
        if (defaultValue == null && null in values) throw TimeSeriesDataException("Cannot make non-nullable: missing values and no default.")

        return YearlyTimeSeriesDoubleData(defaultValue, start, values.notNull(), initialCumulativeValue)
    }

    override fun cumulative() = nonNull().cumulative()

}