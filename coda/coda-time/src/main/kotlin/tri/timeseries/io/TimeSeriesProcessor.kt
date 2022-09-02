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
package tri.timeseries.io

import tri.timeseries.MetricInfo
import tri.timeseries.TimeSeries
import tri.util.measureTime

/** Tool that supports both reading and processing input data to a normalized format, and storing that data locally so next time it can be more quickly retrieved. */
abstract class TimeSeriesProcessor {
    /** Load data by source. */
    fun data(source: String? = null) = loadProcessedData()?.bySource(source) ?: reloadRawData().bySource(source)

    //region DATA LOADING

    /** Forces data to be reprocessed from source files. */
    fun reloadRawData(): List<TimeSeries> {
        val raw = loadRaw()
        if (raw.isNotEmpty()) {
            measureTime {
                saveProcessed(raw)
            }.let {
                processingNote("Saved ${raw.size} time series using ${this::class.simpleName} in $it.")
            }
            return raw
        }
        processingNote("No data returned when loading data from $this.")
        return listOf()
    }

    /** Loads already processed data, if present. */
    fun loadProcessedData(): List<TimeSeries>? {
        val processed = loadProcessed()
        if (processed.isNotEmpty()) {
            return processed
        }
        return null
    }

    //endregion

    /** List of metric/qualifier pairs provided by this processor. */
    abstract fun metricsProvided(): Set<MetricInfo>

    /** Load data from original source. */
    abstract fun loadRaw(): List<TimeSeries>
    /** Saves processed data, so it can be retrieved more quickly later. */
    abstract fun saveProcessed(data: List<TimeSeries>)
    /** Load data from local source/cache, if possible. */
    abstract fun loadProcessed(): List<TimeSeries>

    private fun List<TimeSeries>.bySource(source: String? = null) = if (source == null) this else filter { it.source == source }
}

internal fun processingNote(text: String) = println("[${ansiYellow("DATA")}] $text")

private fun ansiYellow(text: String) = "$ANSI_YELLOW$text$ANSI_RESET"

private const val ANSI_YELLOW = "\u001B[33m"
private const val ANSI_RESET = "\u001B[0m"
