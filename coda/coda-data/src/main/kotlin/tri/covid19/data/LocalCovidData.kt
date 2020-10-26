package tri.covid19.data

import tri.area.Lookup
import tri.timeseries.TimeSeries
import tri.timeseries.TimeSeriesQuery
import tri.util.toLocalDate
import java.io.File
import java.time.format.DateTimeFormatter

/** Maintains access locations for local COVID data. */
object LocalCovidData : TimeSeriesQuery(JhuDailyReports, IhmeForecasts, LanlForecasts, YygForecasts) {

    internal val dataDir = object : Iterator<File> { var file = File("").absoluteFile
        override fun hasNext() = file.parentFile != null
        override fun next() = file.also { file = file.parentFile }
    }.asSequence().map { File(it, "data/") }.first { it.exists() }
    internal fun normalizedDataFile(s: String) = File(dataDir, "normalized/$s")
    internal val jhuCsseProcessedData = normalizedDataFile("jhucsse-processed.csv")

    /** Read forecasts from data dir by pattern. */
    internal fun jhuCsseDailyData(filter: (File) -> Boolean) = File(dataDir, "historical/").walk().filter(filter)
            .map { it.toURI().toURL() }.toList().also { println("$this ${it.map { it.path.substringAfterLast('/') }}") }

    /** Read forecasts from data dir by pattern. */
    internal fun forecasts(filter: (File) -> Boolean) = File(dataDir, "forecasts/").walk().filter(filter)
            .map { it.toURI().toURL() }.toList().also { println("$this ${it.map { it.path.substringAfterLast('/') } }") }

    /** Extracts any number of metrics from given row of data, based on a field name predicate. */
    internal fun Map<String, String>.extractMetrics(source: String, regionField: String, assumeUsState: Boolean = false, dateField: String,
                                           metricFieldPattern: (String) -> Boolean, metricNameMapper: (String) -> String?): List<TimeSeries> {
        return keys.filter { metricFieldPattern(it) }.mapNotNull {
            val value = get(it)?.toDoubleOrNull()
            val name = metricNameMapper(it)
            when {
                value == null || name == null -> null
                else -> metric(source, get(regionField) ?: throw IllegalArgumentException(), assumeUsState,
                        name, "",get(dateField) ?: throw IllegalArgumentException(), value)
            }
        }
    }

    private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

    /** Easy way to construct metric from string value content. */
    internal fun metric(source: String, areaId: String, assumeUsState: Boolean, metric: String?, qualifier: String, date: String, value: Double) = metric?.let {
        val area = Lookup.areaOrNull(areaId, assumeUsState)!!
        TimeSeries(source, area.id, it, qualifier, 0.0, date.toLocalDate(FORMAT), value)
    }

}