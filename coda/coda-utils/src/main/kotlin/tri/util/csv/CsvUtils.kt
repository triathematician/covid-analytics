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
package tri.util.csv

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import tri.util.format
import tri.util.log
import tri.util.url
import java.io.File
import java.io.PrintStream
import java.io.Reader
import java.net.URL
import kotlin.reflect.KClass

val MAPPER: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

/** Get first line, removing BOM markers. */
internal fun Reader.firstLine() = useLines {
    it.first().substringAfter("\uFEFF").substringAfter("ï»¿")
}

//region EXTENSION FUNCTIONS FOR SPECIFIC SOURCES

/** Reads lines of data from a string. */
fun String.csvKeyValues(splitOnNewLines: Boolean = true) = CsvLineSplitter.readData(splitOnNewLines, this).keyValues()
/** Maps lines of data from a string. */
fun <X> String.mapCsvKeyValues(splitOnNewLines: Boolean, op: (Map<String, String>) -> X) = csvKeyValues(splitOnNewLines).map { op(it) }
/** Maps lines of data from a string to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> String.mapCsvKeyValues(splitOnNewLines: Boolean = true) = csvKeyValues(splitOnNewLines).map { MAPPER.convertValue<X>(it) }

/** Reads lines of data from a string. */
fun String.csvKeyValuesFast() = CsvLineSplitterFast.readData(this).keyValues()
/** Maps lines of data from a string. */
fun <X> String.mapCsvKeyValuesFast(op: (Map<String, String>) -> X) = csvKeyValuesFast().map { op(it) }
/** Maps lines of data from a string to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> String.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Reads lines of data from a file. */
fun File.csvKeyValues(splitOnNewLines: Boolean = true) = url.csvKeyValues(splitOnNewLines)
/** Maps lines of data from a file. */
fun <X> File.mapCsvKeyValues(splitOnNewLines: Boolean = true, op: (Map<String, String>) -> X) = csvKeyValues(splitOnNewLines).map { op(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValues(splitOnNewLines: Boolean = true) = csvKeyValues(splitOnNewLines).map { MAPPER.convertValue<X>(it) }

/** Reads lines of data from a file. */
fun File.csvKeyValuesFast() = url.csvKeyValuesFast()
/** Maps lines of data from a file. */
fun <X> File.mapCsvKeyValuesFast(op: (Map<String, String>) -> X) = csvKeyValuesFast().map { op(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Reads lines of data from a URL. */
fun URL.csvLines(splitOnNewLines: Boolean) = CsvLineSplitter.readData(splitOnNewLines, this).second
/** Maps lines of data from a URL. */
fun <X> URL.mapCsvKeyValues(splitOnNewLines: Boolean = true, op: (Map<String, String>) -> X) = csvKeyValues(splitOnNewLines).map { op(it) }
/** Reads lines of data from a URL. */
fun URL.csvKeyValues(splitOnNewLines: Boolean = true) = CsvLineSplitter.readData(splitOnNewLines, this).keyValues()
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> URL.mapCsvKeyValues(splitOnNewLines: Boolean = true) = csvKeyValues(splitOnNewLines).map { MAPPER.convertValue<X>(it) }

/** For files that don't use escape quotes, reads lines of data from a URL. */
fun URL.csvKeyValuesFast() = CsvLineSplitterFast.readData(this).keyValues()
/** Maps lines of data from a URL. */
fun <X> URL.mapCsvKeyValuesFast(op: (Map<String, String>) -> X) = csvKeyValuesFast().map { op(it) }
/** For files that don't use escape quotes, maps lines of CSV data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> URL.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Maps CSV file to target object, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> KClass<*>.csvResource(splitOnNewLines: Boolean, name: String) = java.getResource(name).mapCsvKeyValues<X>(splitOnNewLines).toList()

//endregion

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

fun Map<String, String>.stringNonnull(n: String) = get(n)?.let { it.ifEmpty { null } } ?: throw UnsupportedOperationException("Unexpected $n = ${get(n)}")
fun Map<String, String>.string(n: String) = get(n)?.let { it.ifEmpty { null } }
fun Map<String, String>.boolean(n: String) = get(n)?.let { "TRUE".equals(it, ignoreCase = true) } ?: false
fun Map<String, String>.int(n: String) = get(n)?.toIntOrNull() ?: get(n)?.toDoubleOrNull()?.toInt()
fun Map<String, String>.double(n: String) = get(n)?.toDoubleOrNull()

//endregion
