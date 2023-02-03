/*-
 * #%L
 * coda-data-0.2.9-SNAPSHOT
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
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

import tri.timeseries.*
import tri.util.measureTimedValue
import java.io.File
import java.nio.charset.Charset

/** Processes files from a "raw" source and saves them to a processed file location. */
abstract class TimeSeriesCachingProcessor(val processed: () -> File): TimeSeriesProcessor() {

    override fun loadProcessed(): List<TimeSeries> {
        val file = processed()
        return logLoadProcessedResource(file, file.exists()) {
            TimeSeriesFileFormat.readSeries(it, Charsets.UTF_8)
        }
    }

    fun deleteProcessedFile() = processed().delete()

    open fun process(series: List<TimeSeries>) = measureTimedValue {
        series.regroupAndMax(coerceIncreasing = false, replaceZerosWithPrevious = false)
    }.let {
        processingNote("Regrouped data into ${it.value.size} time series in ${it.duration}")
        it.value
    }

    companion object {
        /** Load operation with integrated logging, supports reading from alternative resource types. */
        fun <X> logLoadProcessedResource(resource: X, exists: Boolean, readOp: (X) -> List<TimeSeries>): List<TimeSeries> {
            return if (exists) {
                measureTimedValue {
                    if (Charset.defaultCharset() != Charsets.UTF_8) {
                        processingNote("Default charset is ${Charset.defaultCharset()}. Loading files with UTF-8 instead.")
                    }
                    readOp(resource)
                }.let {
                    processingNote("Loaded ${it.value.size} processed time series in ${it.duration} from $resource")
                    it.value
                }
            } else {
                processingNote("Processed file not found -- will reload raw data: $resource")
                listOf()
            }
        }
    }
}
