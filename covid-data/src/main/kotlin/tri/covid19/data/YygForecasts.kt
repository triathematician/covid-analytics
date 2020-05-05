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
                    metric(yygRegion(get("region")!!, get("country")!!),
                            "$YYG-$date $it", get("date")!!, get(it)!!)
                }
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
        val date = fullMetricId.substringAfter("YYG-").substringBefore(" ")
        return ForecastId(YYG, "$date-2000".toLocalDate(M_D_YYYY), region, DEATHS)
    }

}