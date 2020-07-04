package tri.covid19.data

import tri.covid19.*
import tri.timeseries.ForecastId
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionInfo
import tri.util.csvKeyValues
import tri.util.toLocalDate
import java.net.URL
import kotlin.IllegalStateException

const val IHME = "IHME"

/** Loads IHME models. */
object IhmeForecasts: CovidDataNormalizer(addIdSuffixes = true) {

    override fun sources() = forecasts { it.name.startsWith("ihme") && it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        val date = url.path.substringAfter("ihme-").substringBefore(".csv")
        return url.csvKeyValues()
                .filter { it["totdea_lower"] != it["totdea_upper"] }.toList()
                .flatMap {
                    it.extractMetrics(regionField = "location_name", dateField = "date",
                            metricFieldPattern = { "_" in it && !it.startsWith("location") },
                            metricNameMapper = { metricName(it, date) })
                }
    }

    private fun metricName(metric: String, date: String): String? {
        return "$IHME-$date " + when (metric.substringBefore("_")) {
            "totdea" -> DEATHS
            "allbed" -> BEDS
            "ICUbed" -> ICU
            "InvVen" -> VENTILATORS
            "admis" -> ADMITS
            else -> return null
        } + when (metric.substringAfter("_")) {
            "mean" -> ""
            "lower" -> "-lower"
            "upper" -> "-upper"
            else -> return null
        }
    }

}