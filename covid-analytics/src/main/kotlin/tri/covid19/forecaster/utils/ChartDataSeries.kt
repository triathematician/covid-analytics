package tri.covid19.forecaster.utils

import tri.timeseries.MetricTimeSeries
import tri.util.DateRange
import java.time.LocalDate

/** A named set of (x,y) data points. */
data class ChartDataSeries(var id: String, var points: DataPoints) {
    fun maxX() = points.map { it.first.toDouble() }.max()
    fun maxY() = points.map { it.second.toDouble() }.max()
}

/** Construct series from time series, using indices of metric's domain for x values. */
fun series(id: String, s: MetricTimeSeries) = ChartDataSeries(id, s.domain.mapIndexed { i, d -> i to s.getOrNull(d) }.filterNullValues())

/** Construct series from time series, using indices of alternate domain for x values. */
fun series(id: String, domain: DateRange, s: MetricTimeSeries) = ChartDataSeries(id, domain.mapIndexed { i, d -> i to s.getOrNull(d) }.filterNullValues())

/** Construct series from two time series, using the common domain between the two. */
fun series(id: String, x: MetricTimeSeries, y: MetricTimeSeries) = series(id, x.domain.intersect(y.domain) ?: emptySet(), x, y)

/** Construct series from two time series, using the common domain between the two. */
fun series(id: String, domain: Iterable<LocalDate>, x: MetricTimeSeries, y: MetricTimeSeries) = ChartDataSeries(id, domain.mapIndexed { _, d -> x.getFinite(d) to y.getFinite(d) }.filterNullValues())

/** Get finite value from data series. */
fun MetricTimeSeries.getFinite(d: LocalDate) = getOrNull(d)?.let { if (it.isFinite()) it else null }

/** Filters out nulls from pairs. */
private fun <X, Y> List<Pair<X?, Y?>>.filterNullValues() = filter { it.first != null && it.second != null } as List<Pair<X, Y>>