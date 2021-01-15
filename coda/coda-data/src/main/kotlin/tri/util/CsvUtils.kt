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
package tri.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import java.io.*
import java.lang.UnsupportedOperationException
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URL
import java.nio.charset.Charset
import kotlin.reflect.KClass

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
     * @param splitOnNewLines if true, each line will be read as a separate record; if false (slower), multiple lines will be reconstituted into a single record
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

/** Split lines of the CSV file, without quotes. */
object CsvLineSplitterFast {
    /** Reads data from the given URL, returning the header line and content lines. */
    fun readData(url: URL, charset: Charset = Charsets.UTF_8) = readData { InputStreamReader(url.openStream(), charset) }

    /** Reads data from the given string, returning the header line and content lines. */
    fun readData(string: String) = readData { StringReader(string) }

    /** Reads data from a reader, returning the header line and content lines. */
    fun readData(reader: () -> Reader) = splitLine(reader().firstLine()) to BufferedReader(reader()).lineSequence().drop(1).map { splitLine(it) }

    /** Splits a comma-separated lines. An empty line will generate an exception. */
    fun splitLine(line: String) = line.split(",").map { it.javaTrim() }
}

/** Get first line, removing BOM markers. */
private fun Reader.firstLine() = useLines {
    it.first().substringAfter("\uFEFF").substringAfter("ï»¿")
}

val MAPPER = ObjectMapper().registerKotlinModule().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

/** Maps lines of data from a string. */
fun <X> String.mapCsvKeyValues(splitOnNewLines: Boolean, op: (Map<String, String>) -> X) = csvKeyValues(splitOnNewLines).map { op(it) }
/** Reads lines of data from a URL. */
fun String.csvKeyValues(splitOnNewLines: Boolean = true) = CsvLineSplitter.readData(splitOnNewLines,this).keyValues()

/** Reads lines of data from a file. */
fun File.csvKeyValues(splitOnNewLines: Boolean = true) = url.csvKeyValues(splitOnNewLines)
/** Reads lines of data from a file. */
fun File.csvKeyValuesFast() = url.csvKeyValuesFast()
/** Maps lines of data from a file. */
fun <X> File.mapCsvKeyValues(splitOnNewLines: Boolean = true, op: (Map<String, String>) -> X) = csvKeyValues(splitOnNewLines).map { op(it) }
/** Maps lines of data from a file. */
fun <X> File.mapCsvKeyValuesFast(op: (Map<String, String>) -> X) = csvKeyValuesFast().map { op(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValues(splitOnNewLines: Boolean = true) = csvKeyValues(splitOnNewLines).map { MAPPER.convertValue<X>(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Reads lines of data from a URL. */
fun URL.csvLines(splitOnNewLines: Boolean) = CsvLineSplitter.readData(splitOnNewLines, this).second
/** Reads lines of data from a URL. */
fun URL.csvKeyValues(splitOnNewLines: Boolean = true) = CsvLineSplitter.readData(splitOnNewLines, this).keyValues()
/** For files that don't use escape quotes, reads lines of data from a URL. */
fun URL.csvKeyValuesFast() = CsvLineSplitterFast.readData(this).keyValues()
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> URL.mapCsvKeyValues(splitOnNewLines: Boolean = true) = csvKeyValues(splitOnNewLines).map { MAPPER.convertValue<X>(it) }
/** For files that don't use escape quotes, maps lines of CSV data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> URL.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Maps CSV file to target object, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> KClass<*>.csvResource(splitOnNewLines: Boolean, name: String) = java.getResource(name).mapCsvKeyValues<X>(splitOnNewLines).toList()

//region CONVERTING TO KEY VALUES

/** Pairs up header with content. */
private fun Pair<List<String>, List<List<String>>>.keyValues() = second.map {
    datum -> datum.filterIndexed { i, _ -> checkSize(datum, i, first.size) }
        .mapIndexed { i, s -> first[i] to s }.toMap()
}

/** Pairs up header with content. */
private fun Pair<List<String>, Sequence<List<String>>>.keyValues() = second.map {
    datum -> datum.filterIndexed { i, _ -> checkSize(datum, i, first.size) }
        .mapIndexed { i, s -> first[i] to s }.toMap()
}

/** Checks size against header. */
private fun checkSize(datum: List<String>, i: Int, expected: Int) = when {
    i >= expected -> {
        println("More columns than expected: \n[[[\n   - ${datum.joinToString("\n   - ")}\n]]]")
        false
    }
    else -> true
}

//endregion

//region CSV WRITING

/** Log a list of items as comma-separated CSV lines. */
fun List<Any>.logCsv(ps: PrintStream = System.out, prefix: String = "", sep: String = ",") = map {
    when (it) {
        is Int -> it
        is Number -> if (it.toDouble() >= 0.1) it.format(3) else it.format(6)
        else -> it
    }.toString()
}.joinToString(sep) { if (',' in it) "\"$it\"" else it }.log(ps, prefix)

//endregion

//region GETTING VALUES FROM STRING KEY-VALUE MAPS

fun Map<String, String>.stringNonnull(n: String) = get(n)?.let { if (it.isEmpty()) null else it } ?: throw UnsupportedOperationException("Unexpected $n = ${get(n)}")
fun Map<String, String>.string(n: String) = get(n)?.let { if (it.isEmpty()) null else it }
fun Map<String, String>.boolean(n: String) = get(n)?.let { "TRUE".equals(it, ignoreCase = true) } ?: false
fun Map<String, String>.int(n: String) = get(n)?.toIntOrNull() ?: get(n)?.toDoubleOrNull()?.toInt()
fun Map<String, String>.double(n: String) = get(n)?.toDoubleOrNull()

//endregion
