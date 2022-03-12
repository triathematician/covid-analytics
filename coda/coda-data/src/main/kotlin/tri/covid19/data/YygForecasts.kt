/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.covid19.data

import tri.area.AreaType
import tri.area.Lookup
import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.data.LocalCovidData.forecasts
import tri.covid19.data.LocalCovidData.metric
import tri.covid19.data.LocalCovidData.normalizedDataFile
import tri.timeseries.MetricInfo
import tri.timeseries.TimeSeries
import tri.timeseries.io.TimeSeriesFileProcessor
import tri.util.csvKeyValues
import java.io.File
import kotlin.time.ExperimentalTime

const val YYG = "YYG"

/** Loads YYG models. */
@ExperimentalTime
object YygForecasts: TimeSeriesFileProcessor({ forecasts { it.name.startsWith("yyg") && it.extension == "csv" } },
        { normalizedDataFile("yyg-forecasts.csv") }) {

    // TODO - part of the metrics are dynamically generated -- need to adjust for multiple forecasts?
    override fun metricsProvided() = setOf("$DEATHS-lower", DEATHS, "$DEATHS-upper", "$CASES-lower", CASES, "$CASES-upper").map { MetricInfo(it) }.toSet()

    override fun inprocess(file: File): List<TimeSeries> {
        val date = file.name.substringAfter("yyg-").substringBeforeLast("-")
        return file.csvKeyValues(true)
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
