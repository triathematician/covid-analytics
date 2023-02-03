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

import tri.timeseries.TimeSeries
import tri.util.measureTimedValue
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/** Processes raw files to processed files, reads processed files if possible. */
abstract class TimeSeriesFileProcessor(val rawSources: () -> List<File>, processed: () -> File): TimeSeriesCachingProcessor(processed) {

    override fun toString() = "TimeSeriesFileProcessor ${rawSources()}"

    override fun loadRaw() = process(rawSources().flatMap { file ->
        measureTimedValue {
            processingNote("Loading data from $file...")
            inprocess(file)
        }.let {
            processingNote("Loaded ${it.value.size} rows in ${it.duration} from $file")
            it.value
        }
    })

    abstract fun inprocess(file: File): List<TimeSeries>

    override fun saveProcessed(data: List<TimeSeries>) =
            TimeSeriesFileFormat.writeSeries(data, BufferedOutputStream(FileOutputStream(processed())), Charsets.UTF_8)

}
