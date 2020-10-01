/**
 * OSS from https://github.com/triathematician/covid-analytics
 */
package tri.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.*
import java.net.URL
import kotlin.reflect.KClass

/** Split lines of the CSV file, accommodating quotes and empty entries. */
object CsvLineSplitter {
    private val regex = "(?m)(?s)(?<=^)(?:[^\"\\r\\n]+|(?<=^|,)\"(?:\"\"|[^\"]+)+\"(?=,|$))+(?=$)".toRegex()
    private val inlineRegex = "(?m)(?<=^|,)(?:\"\"|(?:)|[^,\"\\r\\n]+|\"(?:\"\"|[^\"]+)+\")(?=,|$)".toRegex()

    /** Reads data from the given URL, returning the header line and content lines. */
    fun readData(url: URL) = readData { InputStreamReader(url.openStream()) }

    /** Reads data from the given string, returning the header line and content lines. */
    fun readData(string: String) = readData { StringReader(string) }

    /** Reads data from a reader, returning the header line and content lines. */
    fun readData(reader: () -> Reader): Pair<List<String>, List<List<String>>> {
        val header = splitLine(reader().firstLine()).map { it.javaTrim() }
        val otherLines = BufferedReader(reader()).lineSequence().drop(1).joinToString("\n")
        val others = regex.findAll(otherLines).map { it.value }.toList()
        return header to others.map { splitLine(it) }
    }

    /** Splits a comma-separated lines. An empty line will generate an exception. */
    fun splitLine(line: String): List<String> {
        require(line.isNotBlank())
        return inlineRegex.findAll(line).map {
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
    fun readData(url: URL) = readData { InputStreamReader(url.openStream()) }

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

val MAPPER = ObjectMapper().registerKotlinModule()

/** Maps lines of data from a string. */
fun <X> String.mapCsvKeyValues(op: (Map<String, String>) -> X) = csvKeyValues().map { op(it) }
/** Reads lines of data from a URL. */
fun String.csvKeyValues() = CsvLineSplitter.readData(this).keyValues()

/** Reads lines of data from a file. */
fun File.csvKeyValues() = toURI().toURL().csvKeyValues()
/** Reads lines of data from a file. */
fun File.csvKeyValuesFast() = toURI().toURL().csvKeyValuesFast()
/** Maps lines of data from a file. */
fun <X> File.mapCsvKeyValues(op: (Map<String, String>) -> X) = csvKeyValues().map { op(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValues() = csvKeyValues().map { MAPPER.convertValue<X>(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Reads lines of data from a URL. */
fun URL.csvLines() = CsvLineSplitter.readData(this).second
/** Reads lines of data from a URL. */
fun URL.csvKeyValues() = CsvLineSplitter.readData(this).keyValues()
/** For files that don't use escape quotes, reads lines of data from a URL. */
fun URL.csvKeyValuesFast() = CsvLineSplitterFast.readData(this).keyValues()
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> URL.mapCsvKeyValues() = csvKeyValues().map { MAPPER.convertValue<X>(it) }
/** For files that don't use escape quotes, maps lines of CSV data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> URL.mapCsvKeyValuesFast() = csvKeyValuesFast().map { MAPPER.convertValue<X>(it) }

/** Maps CSV file to target object, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> KClass<*>.csvResource(name: String) = java.getResource(name).mapCsvKeyValues<X>().toList()

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

fun Map<String, String>.string(n: String) = get(n)?.let { if (it.isEmpty()) null else it }
fun Map<String, String>.boolean(n: String) = get(n)?.let { "TRUE".equals(it, ignoreCase = true) } ?: false
fun Map<String, String>.int(n: String) = get(n)?.toIntOrNull() ?: get(n)?.toDoubleOrNull()?.toInt()
fun Map<String, String>.double(n: String) = get(n)?.toDoubleOrNull()

//endregion