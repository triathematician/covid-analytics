package triathematician.covid19.reports

import triathematician.covid19.*
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.logCsv
import java.io.File
import java.io.PrintStream
import java.time.Instant
import java.time.ZoneId

//
// Exports data to file in various formats
//

fun main() {
    usCountyData().apply {
        exportCaseHotspots("us_county")
        exportMortalityHotspots("us_county")
        exportIndicators("us_county")
    }

    usStateData().apply {
        exportCaseHotspots("us_state")
        exportMortalityHotspots("us_state")
        exportIndicators("us_state")
    }

    countryData().apply {
        exportCaseHotspots("country")
        exportMortalityHotspots("country")
        exportIndicators("country")
    }
}

fun File.asPrintStream() = PrintStream(outputStream())

//region HOTSPOT EXPORT

private fun List<MetricTimeSeries>.date() = map { it.end }.max().toString()

/** Import all data and export per-capita case hotspot report. */
fun List<MetricTimeSeries>.exportCaseHotspots(target: String)
        = exportCaseHotspots(File("reports/${target}_case_hotspots_${date()}.csv").asPrintStream())

/** Import all data and export per-capita case hotspot report. */
fun List<MetricTimeSeries>.exportCaseHotspots(ps: PrintStream) {
    HotspotInfo.header.logCsv(ps)
    hotspotPerCapitaInfo(CASES_PER_100K,  0) { it >= 0 }
            .sortedByDescending { it.totalSeverity * 10000 + it.value.toDouble() }
            .forEach { it.toList().logCsv(ps) }
}

/** Import all data and export per-capita death hotspot report. */
fun List<MetricTimeSeries>.exportMortalityHotspots(target: String)
        = exportMortalityHotspots(File("reports/${target}_mortality_hotspots_${date()}.csv").asPrintStream())

/** Import all data and export per-capita death hotspot report. */
fun List<MetricTimeSeries>.exportMortalityHotspots(ps: PrintStream) {
    HotspotInfo.header.logCsv(ps)
    hotspotPerCapitaInfo(DEATHS_PER_100K,  0) { it >= 0 }
            .sortedByDescending { it.totalSeverity * 10000 + it.value.toDouble() }
            .forEach { it.toList().logCsv(ps) }
}

//endregion

//region INDICATOR EXPORT

/** Import all data and export as indicators. */
fun List<MetricTimeSeries>.exportIndicators(target: String) = exportIndicators(File("reports/${target}_indicators.csv").asPrintStream())

/** Import all data and export as indicators. */
fun List<MetricTimeSeries>.exportIndicators(ps: PrintStream) {
    listOf("Region", "FIPS", "Metric", "Date", "Value").logCsv(ps)
    filter { it.values.any { it > 0.0 } }
            .flatMap { it.indicators() }
            .forEach { it.logCsv(ps) }
}

fun MetricTimeSeries.indicators() = valuesAsMap.filter { it.value.isFinite() }.map {
    listOf(id, id2, metric, Instant.from(it.key.atStartOfDay(ZoneId.systemDefault())), if (intSeries) it.value.toInt() else it.value)
}

//endregion