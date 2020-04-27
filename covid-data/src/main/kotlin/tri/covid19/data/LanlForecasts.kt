package tri.covid19.data

import tri.covid19.DEATHS
import tri.timeseries.MetricTimeSeries
import tri.util.csvKeyValues
import java.net.URL

const val LANL = "LANL"

/** Loads LANL models. */
object LanlForecasts: CovidDataNormalizer() {

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

}