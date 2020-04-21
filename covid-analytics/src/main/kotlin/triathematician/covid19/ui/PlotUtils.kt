package triathematician.covid19.ui

import triathematician.timeseries.MetricTimeSeries
import triathematician.util.DateRange

typealias DataPoints = List<Pair<Number, Number>>

/** A named set of (x,y) data points. */
data class DataSeries(var id: String, var points: DataPoints) {

    /** Construct series with given date range. */
    constructor(domain: DateRange, series: MetricTimeSeries) : this(series.metric, domain.mapIndexed { i, d -> i to series[d] })

    /** Construct series with given date range. */
    constructor(domain: DateRange, x: MetricTimeSeries, y: MetricTimeSeries) : this(y.metric, domain.map { d -> x[d] to y[d] })

    /** Construct series with given date range, skipping anything outside of an exclusion domain. */
    constructor(totalDomain: DateRange, inDomain: DateRange?, series: MetricTimeSeries) : this(series.metric,
            totalDomain.mapIndexed { i, d -> if (inDomain != null && d in inDomain) i to series[d] else null }.filterNotNull())

    /** Construct series with given date range. */
    constructor(totalDomain: DateRange, inDomain: DateRange?, x: MetricTimeSeries, y: MetricTimeSeries) : this(x.metric,
            totalDomain.mapNotNull { d -> if (inDomain != null && d in inDomain) x[d] to y[d] else null })

}