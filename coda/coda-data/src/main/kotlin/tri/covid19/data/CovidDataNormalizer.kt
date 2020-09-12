package tri.covid19.data

import com.fasterxml.jackson.module.kotlin.readValue
import tri.area.AreaInfo
import tri.area.Lookup
import tri.area.Usa
import tri.timeseries.*
import tri.util.DefaultMapper
import tri.util.toLocalDate
import java.io.File
import java.net.URL
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@ExperimentalTime
fun main() {
//    YygForecasts.processTo(File("../data/normalized/yyg-forecasts.json"))
//    LanlForecasts.processTo(File("../data/normalized/lanl-forecasts.json"))
//    IhmeForecasts.processTo(File("../data/normalized/ihme-forecasts.json"))
    JhuDailyReports.processTo(File("../data/normalized/jhu-historical.json"))
}

private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

/** Translates data from a source file to a common format. */
abstract class CovidDataNormalizer(val addIdSuffixes: Boolean = false) {

    /** List of files to process. */
    abstract fun sources(): List<URL>

    /** Processes a single file to create time series. */
    abstract fun readSource(url: URL): List<MetricTimeSeries>

    /** Read forecasts from data dir by pattern. */
    protected fun historicalData(filter: (File) -> Boolean) = File("../data/historical/").walk().filter(filter)
            .map { it.toURI().toURL() }.toList().also { println("$this ${it.map { it.path.substringAfterLast('/') } }") }

    /** Read forecasts from data dir by pattern. */
    protected fun forecasts(filter: (File) -> Boolean) = File("../data/forecasts/").walk().filter(filter)
            .map { it.toURI().toURL() }.toList().also { println("$this ${it.map { it.path.substringAfterLast('/') } }") }

    /** Combine results of multiple files into series grouped by region. */
    open fun processTimeSeries(data: List<MetricTimeSeries>, coerceIncreasing: Boolean = false): List<RegionTimeSeries> {
        return data.groupBy { it.area }.map { (region, data) ->
            val metrics = data.regroupAndMerge(coerceIncreasing).filter { it.values.any { it > 0.0 } }
            RegionTimeSeries(region, *metrics.toTypedArray())
        }
    }

    /** Execute the normalizer. */
    @ExperimentalTime
    fun processTo(target: File) {
        measureTime {
            val series = sources().flatMap { readSource(it) }
            val processed = processTimeSeries(series)
            DefaultMapper.writerWithDefaultPrettyPrinter().writeValue(target, processed)
        }.also {
            println("Processed $this in $it")
        }
    }

    /** Extracts any number of metrics from given row of data, based on a field name predicate. */
    protected open fun Map<String, String>.extractMetrics(regionField: String, dateField: String,
                                                   metricFieldPattern: (String) -> Boolean,
                                                   metricNameMapper: (String) -> String?): List<MetricTimeSeries> {
        return keys.filter { metricFieldPattern(it) }.mapNotNull {
            val value = get(it)?.toDoubleOrNull()
            val name = metricNameMapper(it)
            when {
                value == null || name == null -> null
                else -> metric(get(regionField) ?: throw IllegalArgumentException(),
                        name,get(dateField) ?: throw IllegalArgumentException(), value)
            }
        }
    }

    /** Easy way to construct metric from string value content. */
    protected open fun metric(region: String, metric: String?, date: String, value: Double)
            = metric?.let { MetricTimeSeries(Lookup.area(region), it, 0.0, date.toLocalDate(FORMAT), value) }

    fun forecastId(model: String, area: AreaInfo, fullMetricId: String): ForecastId? {
        val s = fullMetricId.substringBefore(" ")
        val date = s.substringAfter("-")
        val metric = fullMetricId.substringAfter(" ").substringBefore("-")
        return ForecastId(model, "$date-2020".toLocalDate(M_D_YYYY), area, metric)
    }
}

/** Load forecasts from local data. */
@ExperimentalTime
fun loadTimeSeries(path: String) = loadTimeSeries(File(path))

/** Load forecasts from local data. */
@ExperimentalTime
fun loadTimeSeries(file: File) = measureTimedValue {
    DefaultMapper.readValue<List<RegionTimeSeries>>(file)
}.also {
    println("Loaded data from $file in ${it.duration}")
}.value
