/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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

import tri.area.AreaInfo
import tri.covid19.*
import tri.covid19.data.LocalCovidData.extractMetrics
import tri.covid19.data.LocalCovidData.forecasts
import tri.timeseries.MetricInfo
import tri.timeseries.TimeSeries
import tri.timeseries.TimeSeriesFileProcessor
import tri.util.csvKeyValues
import java.io.File
import kotlin.time.ExperimentalTime

const val IHME = "IHME"

/** Loads IHME models. */
@ExperimentalTime
object IhmeForecasts : TimeSeriesFileProcessor({ forecasts { it.name.startsWith("ihme") && it.extension == "csv" } },
        { LocalCovidData.normalizedDataFile("ihme-forecasts.csv") }) {

    private val EXCLUDE_LOCATIONS = listOf(
            "Other Counties, WA", "Life Care Center, Kirkland, WA", "King and Snohomish Counties (excluding Life Care Center), WA",
            "Valencian Community", "Mexico City")

    override fun metricsProvided() = setOf(DEATHS, BEDS, ICU, VENTILATORS, ADMITS).map { MetricInfo(it) }.toSet()

    override fun inprocess(file: File): List<TimeSeries> {
        val date = file.name.substringAfter("ihme-").substringBefore(".csv")
        return file.csvKeyValues(true)
                .filter { it["totdea_lower"] != it["totdea_upper"] }.toList()
                .filter { it["location_name"] !in EXCLUDE_LOCATIONS }
                .flatMap {
                    it.extractMetrics(source = IHME, dateField = "date", regionField = "location_name", assumeUsState = true,
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
