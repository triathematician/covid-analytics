package tri.covid19.data

import tri.timeseries.MetricTimeSeries
import triathematician.covid19.COUNTRY_ID_FILTER
import triathematician.covid19.US_CBSA_ID_FILTER
import triathematician.covid19.US_COUNTY_ID_FILTER
import triathematician.covid19.US_STATE_ID_FILTER
import kotlin.time.ExperimentalTime

/** Access to history by region. */
@ExperimentalTime
object CovidHistory {

    val allData: List<MetricTimeSeries> by lazy {
        loadTimeSeries("../data/normalized/jhu-historical.json").flatMap { rts ->
            rts.metrics.map { it.toMetricTimeSeries(rts.region.id, "") }
        }
    }

}