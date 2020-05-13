package tri.covid19.data

import com.fasterxml.jackson.module.kotlin.readValue
import tri.regions.RegionLookup
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
import kotlin.time.measureTimedValue

@ExperimentalTime
fun main() {
    YygForecasts.processTo(File("../data/normalized/yyg-forecasts.json"))
    LanlForecasts.processTo(File("../data/normalized/lanl-forecasts.json"))
    IhmeForecasts.processTo(File("../data/normalized/ihme-forecasts.json"))
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
        return data.groupBy { it.region }.map { (region, data) ->
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
                                                   metricNameMapper: (String) -> String): List<MetricTimeSeries> {
        return keys.filter { metricFieldPattern(it) }.mapNotNull {
            val value = get(it)?.toDoubleOrNull()
            if (value == null) null else
                metric(get(regionField) ?: throw IllegalArgumentException(),
                        metricNameMapper(it),
                        get(dateField) ?: throw IllegalArgumentException(),
                        value)
        }
    }

    /** Easy way to construct metric from string value content. */
    protected open fun metric(region: String, metric: String, date: String, value: Double)
            = MetricTimeSeries(RegionLookup(region.maybeFixId()), metric, 0.0, date.toLocalDate(FORMAT), value)

    private fun String.maybeFixId() = when {
        addIdSuffixes && "$this, US" in UnitedStates.stateNames -> "$this, US"
        else -> this
    }

}

/** Load forecasts from local data. */
@ExperimentalTime
fun loadTimeSeries(path: String) = measureTimedValue {
    DefaultMapper.readValue<List<RegionTimeSeries>>(File(path))
}.also {
    println("Loaded data from $path in ${it.duration}")
}.value
