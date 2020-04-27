package tri.covid19.forecaster.utils

import tri.timeseries.MetricTimeSeries
import tri.util.DateRange

/** A named set of (x,y) data points. */
data class ChartDataSeries(var id: String, var points: DataPoints)

/** Construct series from time series, using indices of metric's domain for x values. */
fun series(id: String, s: MetricTimeSeries) = ChartDataSeries(id, s.domain.mapIndexed { i, d -> i to s.getOrNull(d) }.filterNullValues())

/** Construct series from time series, using indices of alternate domain for x values. */
fun series(id: String, domain: DateRange, s: MetricTimeSeries) = ChartDataSeries(id, domain.mapIndexed { i, d -> i to s.getOrNull(d) }.filterNullValues())

/** Construct series from two time series, using the common domain between the two. */
fun series(id: String, domain: DateRange, x: MetricTimeSeries, y: MetricTimeSeries) = ChartDataSeries(id, domain.mapIndexed { _, d -> x.getOrNull(d) to y.getOrNull(d) }.filterNullValues())

/** Filters out nulls from pairs. */
private fun <X, Y> List<Pair<X?, Y?>>.filterNullValues() = filter { it.first != null && it.second != null } as List<Pair<X, Y>>