/*-
 * #%L
 * coda-data-0.2.9-SNAPSHOT
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

import tri.timeseries.*
import java.io.InputStream
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/** Processes URLs to a remote location. */
@ExperimentalTime
abstract class TimeSeriesUrlCachingProcessor(val rawSources: () -> List<URL>,
                                             val processed: () -> URL,
                                             val resourceLoader: (URL) -> InputStream,
                                             val resourceExists: (URL) -> Boolean): TimeSeriesProcessor() {

    override fun toString() = "TimeSeriesUrlCachingProcessor ${rawSources()}"

    override fun loadProcessed(): List<TimeSeries> {
        val url = processed()
        return TimeSeriesCachingProcessor.logLoadProcessedResource(url, resourceExists(url)) {
            TimeSeriesFileFormat.readSeries(resourceLoader(it), Charsets.UTF_8)
        }
    }

    open fun process(series: List<TimeSeries>) = measureTimedValue {
        series.regroupAndMax(coerceIncreasing = false, replaceZerosWithPrevious = false)
    }.let {
        processingNote("Regrouped data into ${it.value.size} time series in ${it.duration}")
        it.value
    }

    override fun loadRaw() = process(rawSources().flatMap { url ->
        measureTimedValue {
            processingNote("Loading data from $url...")
            inprocess(url)
        }.let {
            processingNote("Loaded ${it.value.size} rows in ${it.duration} from $url")
            it.value
        }
    })

    abstract fun inprocess(url: URL): List<TimeSeries>

}
