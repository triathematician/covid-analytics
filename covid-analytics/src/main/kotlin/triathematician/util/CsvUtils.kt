package triathematician.util

import java.io.PrintStream
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
    fun readData(file1: URL): Pair<List<String>, Sequence<List<String>>> {
        val lines = file1.readText().lines()
        val header = splitLine(lines[0])
        return header to lines.asSequence().drop(1).filter { it.isNotBlank() }.map { splitLine(it) }
    }
}

/** Reads lines of data from a URL. */
fun URL.csvLines() = CsvLineSplitter.readData(this).second

/** Reads lines of data from a URL. */
fun URL.csvKeyValues() = CsvLineSplitter.readData(this).let {
    (header, data) -> data.map { datum -> datum.mapIndexed { i, s -> header[i] to s }.toMap() }
}

/** Log a list of items as comma-separated CSV lines. */
fun List<Any>.logCsv(ps: PrintStream = System.out, prefix: String = "", sep: String = ",") = map {
    when (it) {
        is Int -> it
        is Number -> if (it.toDouble() >= 0.1) it.format(3) else it.format(6)
        else -> it
    }.toString()
}.map { if (',' in it) "\"$it\"" else it }
        .joinToString(sep).log(ps, prefix)