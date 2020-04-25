package triathematician.util

import java.io.PrintStream

/** Split lines of the CSV file, accommodating quotes and empty entries. */
object CsvLineSplitter {
    private val regex = "(?<=^|,)(?:\"{2}|(?:)|[^,\"\\r\\n]+|\"(?:\"{2}|[^\"]+)+\")(?=,|\$)".toRegex()
    fun splitLine(line: String): List<String> {
        return regex.findAll(line).map {
            var res = it.value
            while (res.startsWith("\"") && res.endsWith("\"")) {
                res = res.substring(1, res.length - 1)
            }
            res
        }.toList()
    }
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