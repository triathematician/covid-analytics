package tri.covid19.data

import tri.covid19.*
import tri.regions.RegionLookup
import tri.regions.UnitedStates
import tri.timeseries.ForecastId
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionInfo
import tri.timeseries.RegionType
import tri.util.csvKeyValues
import tri.util.toLocalDate
import java.lang.IllegalArgumentException
import java.net.URL

const val YYG = "YYG"

/** Loads YYG models. */
object YygForecasts: CovidDataNormalizer(addIdSuffixes = true) {

    override fun sources() = forecasts { it.name.startsWith("yyg") && it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        val date = url.path.substringAfter("yyg-").substringBeforeLast("-")
        return url.csvKeyValues()
                .filter { it["actual_deaths"] != "0" }.toList()
                .flatMap { it.extractMetrics(date) }
    }

    /** Extracts any number of metrics from given row of data, based on a field name predicate. */
    private fun Map<String, String>.extractMetrics(date: String): List<MetricTimeSeries> {
        return keys.filter { it.startsWith("predicted_total_deaths") || it.startsWith("predicted_total_infected") }
                .filter { !get(it).isNullOrEmpty() }
                .map {
                    metric(yygRegion(get("region")!!, get("country")!!), metricName(it, date), get("date")!!, get(it)!!.toDouble())
                }
    }

    private fun metricName(metric: String, date: String): String {
        val revisedName = when (metric) {
            "predicted_total_deaths_mean" -> "$DEATHS"
            "predicted_total_deaths_lower" -> "$DEATHS-lower"
            "predicted_total_deaths_upper" -> "$DEATHS-upper"
            else -> metric
        }
        return "$YYG-$date $revisedName"
    }

    private fun yygRegion(region: String, country: String): String {
        if (region == "ALL" || region == "")
            return country
        val lookup = RegionLookup("$region, $country")
        if (lookup.type != RegionType.UNKNOWN) {
            return lookup.id
        } else {
            throw IllegalArgumentException("Invalid: $region, $country")
        }
    }

    fun forecastId(region: RegionInfo, fullMetricId: String): ForecastId {
        val s = fullMetricId.substringBefore(" ")
        val date = s.substringAfter("-")
        return ForecastId(YYG, "$date-2020".toLocalDate(M_D_YYYY), region, DEATHS)
    }

}