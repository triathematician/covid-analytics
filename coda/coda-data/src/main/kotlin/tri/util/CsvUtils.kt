/**
 * OSS from https://github.com/triathematician/covid-analytics
 */
package tri.util

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.*
import java.net.URL

/** Split lines of the CSV file, accommodating quotes and empty entries. */
object CsvLineSplitter {
    private val regex = "(?<=^|,)(?:\"{2}|(?:)|[^,\"\\r\\n]+|\"(?:\"{2}|[^\"]+)+\")(?=,|\$)".toRegex()

    /** Splits a comma-separated lines. An empty line will generate an exception. */
    fun splitLine(line: String): List<String> {
        require(line.isNotBlank())
        return regex.findAll(line).map {
            var res = it.value
            while (res.startsWith("\"") && res.endsWith("\"")) {
                res = res.substring(1, res.length - 1)
            }
            res
        }.toList()
    }

    /** Reads data from the given URL, returning the header line and content lines. */
    fun readData(url: URL) = readData { InputStreamReader(url.openStream()) }

    /** Reads data from the given string, returning the header line and content lines. */
    fun readData(string: String) = readData { StringReader(string) }

    /** Reads data from a reader, returning the header line and content lines. */
    fun readData(reader: () -> Reader): Pair<List<String>, Sequence<List<String>>> {
        val line0 = reader().useLines {
            // remove BOM markers in the file before reading header line
            it.first().substringAfter("\uFEFF").substringAfter("ï»¿")
        }
        val otherLines = BufferedReader(reader()).lineSequence().drop(1)
        val header = splitLine(line0).map { it.javaTrim() }
        return header to otherLines.filter { it.isNotBlank() }.map { splitLine(it) }
    }
}

/** Maps lines of data from a string. */
fun <X> String.mapCsvKeyValues(op: (Map<String, String>) -> X) = csvKeyValues().map { op(it) }
/** Reads lines of data from a URL. */
fun String.csvKeyValues() = CsvLineSplitter.readData(this).keyValues()

/** Reads lines of data from a file. */
fun File.csvKeyValues() = toURI().toURL().csvKeyValues()
/** Maps lines of data from a file. */
fun <X> File.mapCsvKeyValues(op: (Map<String, String>) -> X) = csvKeyValues().map { op(it) }
/** Maps lines of data from a file to a data class, using Jackson [ObjectMapper] for conversions. */
inline fun <reified X> File.mapCsvKeyValues() = csvKeyValues().map { ObjectMapper().convertValue(it, X::class.java) }

/** Reads lines of data from a URL. */
fun URL.csvLines() = CsvLineSplitter.readData(this).second
/** Reads lines of data from a URL. */
fun URL.csvKeyValues() = CsvLineSplitter.readData(this).keyValues()

/** Pairs up header with content. */
private fun Pair<List<String>, Sequence<List<String>>>.keyValues() = second.map { datum -> datum
        .filterIndexed { i, _ -> if (i >= first.size) println("More columns than expected: \n[[[\n   - ${datum.joinToString("\n   - ")}\n]]]"); i < first.size }
        .mapIndexed { i, s -> first[i] to s }.toMap() }

/** Log a list of items as comma-separated CSV lines. */
fun List<Any>.logCsv(ps: PrintStream = System.out, prefix: String = "", sep: String = ",") = map {
    when (it) {
        is Int -> it
        is Number -> if (it.toDouble() >= 0.1) it.format(3) else it.format(6)
        else -> it
    }.toString()
}.joinToString(sep) { if (',' in it) "\"$it\"" else it }.log(ps, prefix)

fun Map<String, String>.string(n: String) = get(n)?.let { if (it.isEmpty()) null else it }
fun Map<String, String>.boolean(n: String) = get(n)?.let { "TRUE".equals(it, ignoreCase = true) } ?: false
fun Map<String, String>.int(n: String) = get(n)?.toIntOrNull() ?: get(n)?.toDoubleOrNull()?.toInt()
fun Map<String, String>.double(n: String) = get(n)?.toDoubleOrNull()