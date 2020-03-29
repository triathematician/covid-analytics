package triathematician.util

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