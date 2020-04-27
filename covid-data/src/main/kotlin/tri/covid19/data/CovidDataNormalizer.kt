package tri.covid19.data

import com.fasterxml.jackson.module.kotlin.readValue
import tri.regions.UnitedStates
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionTimeSeries
import tri.timeseries.regroupAndMerge
import tri.util.DefaultMapper
import tri.util.toLocalDate
import java.io.File
import java.net.URL
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
fun main() {
//    LanlForecasts.processTo(File("../data/normalized/lanl-forecasts.json"))
//    IhmeForecasts.processTo(File("../data/normalized/ihme-forecasts.json"))
    JhuDailyReports.processTo(File("../data/normalized/jhu-historical.json"))
}

private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

/** Translates data from a source file to a common format. */
abstract class CovidDataNormalizer {

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
        return data.groupBy { it.group }.map { (region, data) ->
            val metrics = data.regroupAndMerge(coerceIncreasing).filter { it.values.any { it > 0.0 } }
            RegionTimeSeries(region, metrics)
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
                                                   metricPrefix: String): List<MetricTimeSeries> {
        return keys.filter { metricFieldPattern(it) }.map {
            metric(get(regionField) ?: throw IllegalArgumentException(),
                    "$metricPrefix $it",
                    get(dateField) ?: throw IllegalArgumentException(),
                    get(it) ?: throw IllegalArgumentException())
        }
    }

    /** Easy way to construct metric from string value content. */
    protected open fun metric(region: String, metric: String, date: String, value: String)
            = MetricTimeSeries(region.normalized(), "", metric, 0.0, date.toLocalDate(FORMAT), value.toDouble())

    /** Normalizes ID's by region. */
    protected open fun String.normalized() = when {
        UnitedStates.stateNames.contains(this) -> "$this, US"
        this == "United States of America" -> "US"
        else -> this
    }

}

/** Load forecasts from local data. */
fun loadTimeSeries(path: String) = DefaultMapper.readValue<List<RegionTimeSeries>>(File(path))