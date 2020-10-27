package tri.timeseries

import java.io.File
import java.io.FileOutputStream
import java.net.URL

/** Tool that supports both reading and processing input data to a normalized format, and storing that data locally so next time it can be more quickly retrieved. */
abstract class TimeSeriesProcessor {

    fun data(key: String? = null): List<TimeSeries> {
        val processed = loadProcessed(key)
        if (processed.isNotEmpty()) {
            println("Loaded processed ${processed.size} time series for key=$key using $this")
            return processed
        }

        val raw = loadRaw(key)
        if (raw.isNotEmpty()) {
            println("Loaded raw data. Now saving processed ${processed.size} time series for key=$key using $this")
            saveProcessed(key, raw)
            return raw
        }
        throw IllegalStateException("Could not find data: $key")
    }

    /** Load data from original source. */
    abstract fun loadRaw(key: String? = null): List<TimeSeries>

    /** Saves processed data, so it can be retrieved more quickly later. */
    abstract fun saveProcessed(key: String? = null, data: List<TimeSeries>)

    /** Load data from local source/cache, if possible. */
    abstract fun loadProcessed(key: String? = null): List<TimeSeries>

}

/** Processes raw files to processed files, reads processed files if possible. */
abstract class TimeSeriesFileProcessor(val raw: () -> List<URL>, val processed: () -> File): TimeSeriesProcessor() {
    override fun loadRaw(key: String?) = process(raw().flatMap { inprocess(it) })
    override fun saveProcessed(key: String?, data: List<TimeSeries>) = TimeSeriesFileFormat.writeSeries(data, FileOutputStream(processed()))
    override fun loadProcessed(key: String?) = processed().let { if (it.exists()) TimeSeriesFileFormat.readSeries(it) else listOf() }

    open fun process(series: List<TimeSeries>) = series.regroupAndMerge(coerceIncreasing = false)

    abstract fun inprocess(url: URL): List<TimeSeries>
}