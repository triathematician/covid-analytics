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

    private fun metricName(metric: String, date: String): String {
        val revisedName = when (metric) {
            "totdea_mean" -> "$DEATHS"
            "totdea_lower" -> "$DEATHS-lower"
            "totdea_upper" -> "$DEATHS-upper"
            else -> metric
        }
        return "$IHME-$date $revisedName"
    }

    fun forecastId(region: RegionInfo, fullMetricId: String): ForecastId {
        val s = fullMetricId.substringBefore(" ")
        val date = s.substringAfter("-")
        val metric = when {
            "admi" in fullMetricId.toLowerCase() -> ADMITS
            "dea" in fullMetricId.toLowerCase() -> DEATHS
            "bed" in fullMetricId.toLowerCase() -> BEDS
            "ven" in fullMetricId.toLowerCase() -> VENTILATORS
            "icu" in fullMetricId.toLowerCase() -> ICU
            else -> throw IllegalStateException()
        }
        return ForecastId(IHME, "$date-2020".toLocalDate(M_D_YYYY), region, metric)
    }

}