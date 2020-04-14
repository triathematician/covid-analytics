package triathematician.covid19.reports

import triathematician.covid19.COUNTRY_ID_FILTER
import triathematician.covid19.US_COUNTY_ID_FILTER
import triathematician.covid19.US_STATE_ID_FILTER
import triathematician.covid19.sources.dailyReports
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.logCsv
import triathematician.util.log
import java.io.File
import java.io.PrintStream
import java.time.Instant
import java.time.ZoneId

//
// Exports data to file in various formats
//

fun main() {
    exportIndicatorData(US_COUNTY_ID_FILTER, { it.removeSuffix(", US") }, File("reports/us_county_indicators.csv").asPrintStream())
    exportIndicatorData(US_STATE_ID_FILTER, { it.removeSuffix(", US") }, File("reports/us_state_indicators.csv").asPrintStream())
    exportIndicatorData(COUNTRY_ID_FILTER, { it }, File("reports/country_indicators.csv").asPrintStream())
}

fun File.asPrintStream() = PrintStream(outputStream())

/** Import all data and export as time series. */
fun exportIndicatorData(idFilter: (String) -> Boolean = { true }, idTransform: (String) -> String, ps: PrintStream) {
    listOf("State", "Metric", "Date", "Value").log(ps, sep = ",")
    dailyReports()
            .filter { idFilter(it.id) }
            .onEach { it.id = idTransform(it.id) }
            .filter { it.values.any { it > 0.0 }}
            .flatMap { it.indicators() }
            .forEach { it.logCsv(ps) }
}

fun MetricTimeSeries.indicators(): List<List<Any>> {
    return valuesAsMap.filter { it.value.isFinite() }.map {
        listOf(id, metric, Instant.from(it.key.atStartOfDay(ZoneId.systemDefault())), if (intSeries) it.value.toInt() else it.value)
    }
}