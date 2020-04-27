package tri.covid19.data

import tri.timeseries.MetricTimeSeries
import tri.util.csvKeyValues
import java.net.URL

const val IHME = "IHME"

/** Loads IHME models. */
object IhmeForecasts: CovidDataNormalizer() {
    override fun sources() = forecasts { it.name.startsWith("ihme") && it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        val date = url.path.substringAfter("ihme-").substringBefore(".csv")
        return url.csvKeyValues()
                .filter { it["totdea_lower"] != it["totdea_upper"] }.toList()
                .flatMap {
                    it.extractMetrics(regionField = "location_name", dateField = "date",
                            metricFieldPattern = { "_" in it && !it.startsWith("location") },
                            metricPrefix = "$IHME-$date")
                }
    }
}