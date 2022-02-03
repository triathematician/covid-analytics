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

import tri.area.Lookup
import tri.timeseries.TimeSeries
import tri.util.info
import tri.util.toLocalDate
import java.io.File
import java.time.format.DateTimeFormatter

/** Maintains access locations for local COVID data. */
object LocalCovidData {

    internal val dataDir by lazy { listOf(".\\data", "..\\data", "..\\..\\data", "..\\..\\..\\data", "..\\..\\..\\..\\data")
            .firstOrNull { File(it).exists() }
            .let { File(it) }
            .also { info<LocalCovidData>("Data dir: ${it.absolutePath}") } }
    internal fun normalizedDataFile(s: String) = File(dataDir, "normalized/$s")
    internal val jhuCsseProcessedData by lazy { normalizedDataFile("jhucsse-processed.csv") }

    /** Read forecasts from data dir by pattern. */
    internal fun jhuCsseDailyData(filter: (File) -> Boolean) = File(dataDir, "historical/").walk().filter(filter).toList()
            .also { info<LocalCovidData>("$this ${it.map { it.path.substringAfterLast('/') }}") }

    /** Read forecasts from data dir by pattern. */
    internal fun forecasts(filter: (File) -> Boolean) = File(dataDir, "forecasts/").walk().filter(filter).toList()
            .also { info<LocalCovidData>("$this ${it.map { it.path.substringAfterLast('/') } }") }

    /** Extracts any number of metrics from given row of data, based on a field name predicate. */
    internal fun Map<String, String>.extractMetrics(source: String, regionField: String, assumeUsState: Boolean = false, dateField: String,
                                           metricFieldPattern: (String) -> Boolean, metricNameMapper: (String) -> String?): List<TimeSeries> {
        return keys.filter { metricFieldPattern(it) }.mapNotNull {
            val value = get(it)?.toDoubleOrNull()
            val name = metricNameMapper(it)
            when {
                value == null || name == null -> null
                else -> metric(source, get(regionField) ?: throw IllegalArgumentException(), assumeUsState,
                        name, "",get(dateField) ?: throw IllegalArgumentException(), value)
            }
        }
    }

    private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

    /** Easy way to construct metric from string value content. */
    internal fun metric(source: String, areaId: String, assumeUsState: Boolean, metric: String?, qualifier: String, date: String, value: Double) = metric?.let {
        val area = Lookup.areaOrNull(areaId, assumeUsState)!!
        TimeSeries(source, area.id, it, qualifier, 0.0, date.toLocalDate(FORMAT), value)
    }

}
