package tri.covid19.data

import tri.covid19.*
import tri.covid19.data.LocalCovidData.extractMetrics
import tri.covid19.data.LocalCovidData.forecasts
import tri.timeseries.TimeSeries
import tri.timeseries.TimeSeriesFileProcessor
import tri.util.csvKeyValues
import java.io.File
import java.net.URL

const val LANL = "LANL"

/** Loads LANL models. */
object LanlForecasts: TimeSeriesFileProcessor({ forecasts { it.name.startsWith("lanl") && it.extension == "csv" } }, { File("../data/normalized/lanl-forecasts.json") }) {

    override fun inprocess(url: URL): List<TimeSeries> {
        val date = url.path.substringAfter("lanl-").substringBefore(".csv")
        return url.csvKeyValues(true)
                .filter { it["obs"] == "0" }.toList()
                .flatMap {
                    it.extractMetrics(regionField = "state",
                            assumeUsState = true,
                            dateField = "dates",
                            metricFieldPattern = { it.startsWith("q.") },
                            metricNameMapper = { metricName(it, date) })
                }
    }

    private fun metricName(metric: String, date: String): String? {
        val revisedName = when (metric) {
            "q.05" -> "$DEATHS-lower"
            "q.50" -> DEATHS
            "q.95" -> "$DEATHS-upper"
            else -> return null
        }
        return "$LANL-$date $revisedName"
    }

}