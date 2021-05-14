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

import tri.covid19.DEATHS
import tri.covid19.data.LocalCovidData.extractMetrics
import tri.covid19.data.LocalCovidData.forecasts
import tri.covid19.data.LocalCovidData.normalizedDataFile
import tri.timeseries.TimeSeries
import tri.timeseries.TimeSeriesFileProcessor
import tri.util.csvKeyValues
import java.io.File
import kotlin.time.ExperimentalTime

const val LANL = "LANL"

/** Loads LANL models. */
@ExperimentalTime
object LanlForecasts: TimeSeriesFileProcessor({ forecasts { it.name.startsWith("lanl") && it.extension == "csv" } },
        { normalizedDataFile("lanl-forecasts.csv") }) {

    override fun inprocess(file: File): List<TimeSeries> {
        val date = file.name.substringAfter("lanl-").substringBefore(".csv")
        return file.csvKeyValues(true)
                .filter { it["obs"] == "0" }.toList()
                .flatMap {
                    it.extractMetrics(source = LANL, regionField = "state", assumeUsState = true, dateField = "dates",
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
