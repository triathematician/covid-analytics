/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 Elisha Peterson
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
package tri.timeseries

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/** Tool that supports both reading and processing input data to a normalized format, and storing that data locally so next time it can be more quickly retrieved. */
abstract class TimeSeriesProcessor {

    /** Load data by source. */
    fun data(source: String? = null) = loadProcessedData()?.bySource(source) ?: reloadRawData().bySource(source)

    /** Forces data to be reprocessed from source files. */
    fun reloadRawData(): List<TimeSeries> {
        val raw = loadRaw()
        if (raw.isNotEmpty()) {
            println("Loaded raw data. Now saving ${raw.size} time series using ${this::class.simpleName}")
            saveProcessed(raw)
            return raw
        }
        throw IllegalStateException("Could not find data")
    }

    /** Loads already processed data, if present. */
    fun loadProcessedData(): List<TimeSeries>? {
        val processed = loadProcessed()
        if (processed.isNotEmpty()) {
            println("Loaded processed ${processed.size} time series using ${this::class.simpleName}")
            return processed
        }
        return null
    }

    /** Load data from original source. */
    abstract fun loadRaw(): List<TimeSeries>

    /** Saves processed data, so it can be retrieved more quickly later. */
    abstract fun saveProcessed(data: List<TimeSeries>)

    /** Load data from local source/cache, if possible. */
    abstract fun loadProcessed(): List<TimeSeries>

    private fun List<TimeSeries>.bySource(source: String? = null) = if (source == null) this else filter { it.source == source }

}

/** Processes raw files to processed files, reads processed files if possible. */
@ExperimentalTime
abstract class TimeSeriesFileProcessor(val rawSources: () -> List<File>, val processed: () -> File): TimeSeriesProcessor() {
    override fun loadRaw() = process(rawSources().flatMap { file ->
        measureTimedValue {
            println("Loading data from $file...")
            inprocess(file)
        }.let {
            println("Loaded ${it.value.size} rows in ${it.duration} from $file")
            it.value
        }
    })

    override fun loadProcessed(): List<TimeSeries> {
        val file = processed()
        val timestamp = if (file.exists()) file.lastModified() else null
        return if (file.exists()) {
            measureTimedValue {
                if (Charset.defaultCharset() != Charsets.UTF_8) {
                    println("Default charset is ${Charset.defaultCharset()}; loading files with UTF-8 instead.")
                }
                TimeSeriesFileFormat.readSeries(file, Charsets.UTF_8)
            }.let {
                println("Loaded ${it.value.size} processed time series in ${it.duration} from $file")
                it.value
            }
        } else {
            listOf()
        }
    }

    override fun saveProcessed(data: List<TimeSeries>) = TimeSeriesFileFormat.writeSeries(data, FileOutputStream(processed()), Charsets.UTF_8)

    open fun process(series: List<TimeSeries>) = series.regroupAndMerge(coerceIncreasing = false)

    abstract fun inprocess(file: File): List<TimeSeries>
}
