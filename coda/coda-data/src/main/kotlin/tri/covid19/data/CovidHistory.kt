package tri.covid19.data

import tri.timeseries.MetricTimeSeries
import kotlin.time.ExperimentalTime

/** Access to history by region. */
@ExperimentalTime
object CovidHistory {

    val allData: List<MetricTimeSeries> by lazy {
        loadTimeSeries("../data/normalized/jhu-historical.json").flatMap { rts ->
            rts.metrics.map { it.toMetricTimeSeries(rts.areaId) }
        }
    }

}