package tri.covid19.data

import tri.covid19.*
import tri.timeseries.ForecastId
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionInfo
import tri.util.csvKeyValues
import tri.util.toLocalDate
import java.net.URL

const val LANL = "LANL"

/** Loads LANL models. */
object LanlForecasts: CovidDataNormalizer(addIdSuffixes = true) {

    override fun sources() = forecasts { it.name.startsWith("lanl") && it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        val date = url.path.substringAfter("lanl-").substringBefore(".csv")
        return url.csvKeyValues()
                .filter { it["obs"] == "0" }.toList()
                .flatMap {
                    it.extractMetrics(regionField = "state", dateField = "dates",
                            metricFieldPattern = { it.startsWith("q.") },
                            metricPrefix = "$DEATHS $LANL-$date")
                }
    }

    fun forecastId(region: RegionInfo, fullMetricId: String): ForecastId {
        val s = fullMetricId.substringAfter(" ").substringBefore(" ")
        val date = s.substringAfter("-")
        return ForecastId(LANL, "$date-2000".toLocalDate(M_D_YYYY), region, DEATHS)
    }

}