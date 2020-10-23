package tri.covid19.data

import tri.area.Lookup
import tri.timeseries.TimeSeries
import tri.timeseries.TimeSeriesQuery
import tri.util.toLocalDate
import java.io.File
import java.time.format.DateTimeFormatter

/** Maintains access locations for local COVID data. */
object LocalCovidData : TimeSeriesQuery(JhuDailyReports, IhmeForecasts, LanlForecasts, YygForecasts) {

    //region QUERY METHODS

    //endregion

    val jhuCsseProcessedData = File("../data/normalized/jhucsse-processed.csv")

    /** Read forecasts from data dir by pattern. */
    fun jhuCsseDailyData(filter: (File) -> Boolean) = File("../data/historical/").walk().filter(filter)
            .map { it.toURI().toURL() }.toList().also { println("$this ${it.map { it.path.substringAfterLast('/') }}") }

    /** Read forecasts from data dir by pattern. */
    fun forecasts(filter: (File) -> Boolean) = File("../data/forecasts/").walk().filter(filter)
            .map { it.toURI().toURL() }.toList().also { println("$this ${it.map { it.path.substringAfterLast('/') } }") }

    /** Extracts any number of metrics from given row of data, based on a field name predicate. */
    fun Map<String, String>.extractMetrics(regionField: String, assumeUsState: Boolean = false, dateField: String,
                                           metricFieldPattern: (String) -> Boolean, metricNameMapper: (String) -> String?): List<TimeSeries> {
        return keys.filter { metricFieldPattern(it) }.mapNotNull {
            val value = get(it)?.toDoubleOrNull()
            val name = metricNameMapper(it)
            when {
                value == null || name == null -> null
                else -> metric(get(regionField) ?: throw IllegalArgumentException(), assumeUsState,
                        name, "",get(dateField) ?: throw IllegalArgumentException(), value)
            }
        }
    }

    private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

    /** Easy way to construct metric from string value content. */
    fun metric(areaId: String, assumeUsState: Boolean, metric: String?, group: String, date: String, value: Double) = metric?.let {
        val area = Lookup.areaOrNull(areaId, assumeUsState)!!
        TimeSeries(area.id, it, group, 0.0, date.toLocalDate(FORMAT), value)
    }

}