package tri.util.csv

import tri.util.javaTrim
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.URL
import java.nio.charset.Charset

/** Split lines of the CSV file, accommodating quotes and empty entries. */
object CsvLineSplitter {
    internal val FIND_REGEX = "(?m)(?s)(?<=^)(?:[^\"\\r\\n]+|(?<=^|,)\"(?:\"\"|[^\"]+)+\"(?=,|$))+(?=$)".toRegex()
    internal val FIND_REGEX2 = "(?s)^(?:[^\"\\r\\n]+|(?<=^|,)\"(?:\"\"|[^\"]+)+\"(?=,|$))+$".toRegex()

    internal const val QUOTED = "(?<=^|,)\"(?:\"\"|[^\"]+)++\"(?=,|$)"
    internal const val PARTIAL = "(?:[^,\"\\r\\n]+|$QUOTED)"

    internal val MATCH_ONE_MULTILINE_CSV_RECORD = "(?s)^,*(?:$PARTIAL,*)+$".toRegex()
    internal val INLINE_REGEX = "(?m)(?<=^|,)(?:\"\"|(?:)|[^,\"\\r\\n]+|\"(?:\"\"|[^\"]+)+\")(?=,|$)".toRegex()

    /** Reads data from the given URL, returning the header line and content lines. */
    fun readData(splitOnNewLines: Boolean, url: URL, charset: Charset = Charsets.UTF_8) =
            readData(splitOnNewLines) { InputStreamReader(url.openStream(), charset) }

    /** Reads data from the given string, returning the header line and content lines. */
    fun readData(splitOnNewLines: Boolean, string: String) = readData(splitOnNewLines) { StringReader(string) }

    /**
     * Reads data from a reader, returning the header line and content lines.
     * @param splitOnNewLines if true, each line will be read as a separate record;
/*-
 * #%L
 * coda-data-0.4.0-SNAPSHOT
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
 if false (slower), multiple lines will be reconstituted into a single record
     */
    fun readData(splitOnNewLines: Boolean, reader: () -> Reader): Pair<List<String>, Sequence<List<String>>> {
        val header = splitLine(reader().firstLine())!!.map { it.javaTrim() }

        val seq = reader().buffered().lineSequence().drop(1)
        return header to (if (splitOnNewLines) seq else seq.reconstitute()).mapNotNull { splitLine(it) }

//        val otherLines = BufferedReader(reader()).lineSequence().drop(1)
//        val others = if (splitOnNewLines) otherLines else FIND_REGEX.findAll(otherLines.joinToString("\n")).map { it.value }
//        return header to others.mapNotNull { splitLine(it) }
    }

    /** Converts sequence from raw to one where line breaks inside quotes have been merged. */
    internal fun Sequence<String>.reconstitute(): Sequence<String> {
        val iterator = iterator()
        return object : Iterator<String> {
            var next = ""
            override fun hasNext(): Boolean {
                val seq = mutableListOf<String>()
                while (seq.isEmpty() || !MATCH_ONE_MULTILINE_CSV_RECORD.matches(seq.joinToString("\n"))) {
                    if (seq.size > 100) println("Multiline string: ${seq.size}")
                    if (iterator.hasNext()) iterator.next().let { if (it.isNotEmpty()) seq += it }
                    else return false
                }
                next = seq.joinToString("\n")
                return true
            }
            override fun next() = next
        }.asSequence()
    }

    /** Splits a comma-separated lines. An empty line will generate an exception. */
    fun splitLine(line: String): List<String>? {
        if (line.isBlank()) return null
        return INLINE_REGEX.findAll(line).map {
            var res = it.value
            while (res.startsWith("\"") && res.endsWith("\"")) {
                res = res.substring(1, res.length - 1)
            }
            res
        }.toList()
    }
}
