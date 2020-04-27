package tri.covid19.data

import tri.timeseries.MetricTimeSeries

/** Access to history by region. */
object CovidHistory {

    val allData: List<MetricTimeSeries> by lazy {
        loadTimeSeries("../data/normalized/jhu-historical.json").flatMap { rts ->
            rts.metrics.map { it.toMetricTimeSeries(rts.region.id, "") }
        }
    }

}