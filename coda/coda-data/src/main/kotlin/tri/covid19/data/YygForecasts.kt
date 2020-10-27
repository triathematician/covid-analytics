package tri.covid19.data

import tri.area.Lookup
import tri.covid19.*
import tri.timeseries.TimeSeries
import tri.area.AreaType
import tri.covid19.data.LocalCovidData.forecasts
import tri.covid19.data.LocalCovidData.metric
import tri.covid19.data.LocalCovidData.normalizedDataFile
import tri.timeseries.TimeSeriesFileProcessor
import tri.util.csvKeyValues
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URL

const val YYG = "YYG"

/** Loads YYG models. */
object YygForecasts: TimeSeriesFileProcessor({ forecasts { it.name.startsWith("yyg") && it.extension == "csv" } },
        { normalizedDataFile("yyg-forecasts.csv") }) {

    override fun inprocess(url: URL): List<TimeSeries> {
        val date = url.path.substringAfter("yyg-").substringBeforeLast("-")
        return url.csvKeyValues(true)
                .filter { it["predicted_deaths_mean"] != "" }.toList()
                .flatMap { it.extractMetrics(date) }
    }

    /** Extracts any number of metrics from given row of data, based on a field name predicate. */
    private fun Map<String, String>.extractMetrics(date: String): List<TimeSeries> {
        return keys.filter { it.startsWith("predicted_total_deaths") || it.startsWith("predicted_total_infected") }
                .filter { !get(it).isNullOrEmpty() }
                .mapNotNull {
                    metric(YYG, yygArea(get("region")!!, get("country")!!), false,
                            metricName(it, date), "", get("date")!!, get(it)!!.toDouble())
                }
    }

    private fun metricName(metric: String, date: String): String? {
        val revisedName = when (metric) {
            "predicted_total_deaths_mean" -> DEATHS
            "predicted_total_deaths_lower" -> "$DEATHS-lower"
            "predicted_total_deaths_upper" -> "$DEATHS-upper"
            "predicted_total_infected_mean" -> CASES
            "predicted_total_infected_lower" -> "$CASES-lower"
            "predicted_total_infected_upper" -> "$CASES-upper"
            else -> return null
        }
        return "$YYG-$date $revisedName"
    }

    private fun yygArea(region: String, country: String): String {
        if (region == "ALL" || region == "")
            return country
        val lookup = Lookup.areaOrNull(region) ?: Lookup.area("$region, $country")
        if (lookup.type != AreaType.UNKNOWN) {
            return lookup.id
        } else {
            throw IllegalArgumentException("Invalid: $region, $country")
        }
    }

}