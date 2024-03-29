/*-
 * #%L
 * coda-data
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
import tri.util.toLocalDate
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import kotlin.math.roundToInt

/** Writes to/from files, with one series per row, similar to a CSV format. */
object TimeSeriesFileFormat {

    /** Reads several series from a file. */
    fun readSeries(file: File, charset: Charset) =
            BufferedReader(FileReader(file, charset))
                    .useLines(::readLines)

    /** Reads several series from a file. */
    fun readSeries(url: URL, charset: Charset) =
            BufferedReader(InputStreamReader(url.openStream(), charset))
                    .useLines(::readLines)

    /** Reads several series from an input stream. */
    fun readSeries(inputStream: InputStream, charset: Charset) =
            BufferedReader(InputStreamReader(inputStream, charset))
                    .useLines(::readLines)

    /** Reads a sequence of lines as time series objects. */
    private fun readLines(seq: Sequence<String>): List<TimeSeries> =
            seq.map { readSeries(it) }.toList()

    /** Writes several series to the writer. */
    fun writeSeries(m: List<TimeSeries>, out: OutputStream, charset: Charset) =
        PrintStream(out, false, charset).use { ps ->
            m.forEach { writeSeries(it, ps) }
        }

    /** Writes a single series to the writer. */
    fun writeSeries(m: TimeSeries, out: PrintStream) {
        out.println(writeSeriesAsString(m))
    }

    /** Writes a single series to the writer. */
    fun writeSeriesAsString(m: TimeSeries): String {
        val head = sequenceOf(m.source, m.areaId, m.metric, m.qualifier, m.intSeries,
                if (m.intSeries) m.defValue.toInt() else m.defValue, m.start).joinToString("\t")
        val values = m.values.joinToString("\t") { it.writeValue(m.intSeries) }
        return head + "\t" + values
    }

    /** Reads a series from a writer line. */
    fun readSeries(line: String): TimeSeries {
        val split = line.split("\t")
        return TimeSeries(split[0], split[1], split[2], split[3], split[4].toBoolean(), split[5].toDouble(), split[6].toLocalDate(),
                split.subList(7, split.size).map { it.readValue() })
    }

    /** Write double value to TSFF. */
    private fun Double.writeValue(intSeries: Boolean) = when {
        isNaN() -> ""
        this == Double.POSITIVE_INFINITY -> "Inf"
        this == Double.NEGATIVE_INFINITY -> "-Inf"
        intSeries -> roundToInt().toString()
        else -> toString()
    }

    /** Reads value from TSFF. */
    private fun String.readValue(): Double = when (this) {
        "" -> Double.NaN
        "Inf" -> Double.POSITIVE_INFINITY
        "-Inf" -> Double.NEGATIVE_INFINITY
        else -> toDouble()
    }

}
