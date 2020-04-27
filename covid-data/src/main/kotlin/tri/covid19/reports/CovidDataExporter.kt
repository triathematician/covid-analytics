package tri.covid19.reports
//
//import tri.covid19.*
//import tri.covid19.CovidTimeSeriesSources.countryData
//import tri.covid19.CovidTimeSeriesSources.usCountyData
//import tri.covid19.CovidTimeSeriesSources.usStateData
//import tri.timeseries.MetricTimeSeries
//import tri.timeseries.RegionTimeSeries
//import tri.util.DefaultMapper
//import tri.util.logCsv
//import java.io.File
//import java.io.PrintStream
//import java.time.Instant
//import java.time.ZoneId
//
////
//// Exports data to file in various formats
////
//
//fun main() {
//    usCountyData().apply {
//        exportCaseHotspots("us_county")
//        exportMortalityHotspots("us_county")
//        exportIndicators("us_county")
//        exportIndicatorsJson("us_county")
//    }
//
//    usStateData().apply {
//        exportCaseHotspots("us_state")
//        exportMortalityHotspots("us_state")
//        exportIndicators("us_state")
//        exportIndicatorsJson("us_state")
//    }
//
//    countryData().apply {
//        exportCaseHotspots("country")
//        exportMortalityHotspots("country")
//        exportIndicators("country")
//        exportIndicatorsJson("country")
//    }
//}
//
//fun File.asPrintStream() = PrintStream(outputStream())
//
////region HOTSPOT EXPORT
//
//private fun List<MetricTimeSeries>.date() = map { it.end }.max().toString()
//
///** Import all data and export per-capita case hotspot report. */
//fun List<MetricTimeSeries>.exportCaseHotspots(target: String)
//        = exportCaseHotspots(File("reports/${target}_case_hotspots_${date()}.csv").asPrintStream())
//
///** Import all data and export per-capita case hotspot report. */
//fun List<MetricTimeSeries>.exportCaseHotspots(ps: PrintStream) {
//    HotspotInfo.header.logCsv(ps)
//    hotspotPerCapitaInfo(CASES_PER_100K,  0) { it >= 0 }
//            .sortedByDescending { it.totalSeverity * 10000 + it.value.toDouble() }
//            .forEach { it.toList().logCsv(ps) }
//}
//
///** Import all data and export per-capita death hotspot report. */
//fun List<MetricTimeSeries>.exportMortalityHotspots(target: String)
//        = exportMortalityHotspots(File("reports/${target}_mortality_hotspots_${date()}.csv").asPrintStream())
//
///** Import all data and export per-capita death hotspot report. */
//fun List<MetricTimeSeries>.exportMortalityHotspots(ps: PrintStream) {
//    HotspotInfo.header.logCsv(ps)
//    hotspotPerCapitaInfo(DEATHS_PER_100K,  0) { it >= 0 }
//            .sortedByDescending { it.totalSeverity * 10000 + it.value.toDouble() }
//            .forEach { it.toList().logCsv(ps) }
//}
//
////endregion
//
////region INDICATOR EXPORT
//
///** Import all data and export as indicators. */
//fun List<MetricTimeSeries>.exportIndicators(target: String) = exportIndicators(File("reports/${target}_indicators.csv").asPrintStream())
//
///** Import all data and export as indicators. */
//fun List<MetricTimeSeries>.exportIndicatorsJson(target: String) = exportIndicatorsJson(File("reports/${target}_indicators.json").asPrintStream())
//
///** Import all data and export as indicators. */
//fun List<MetricTimeSeries>.exportIndicators(ps: PrintStream) {
//    listOf("Region", "FIPS", "Metric", "Date", "Value").logCsv(ps)
//    filter { it.values.any { it > 0.0 } }
//            .flatMap { it.indicators() }
//            .forEach { it.logCsv(ps) }
//}
//
///** Import all data and export as indicators. */
//fun List<MetricTimeSeries>.exportIndicatorsJson(ps: PrintStream) {
//    val data = groupBy { it.id }.map { RegionTimeSeries(it.key, it.value.filter { it.values.any { it > 0.0 } }) }
//    DefaultMapper.writerWithDefaultPrettyPrinter().writeValue(ps, data)
//}
//
//fun MetricTimeSeries.indicators() = valuesAsMap.filter { it.value.isFinite() }.map {
//    listOf(id, id2, metric, Instant.from(it.key.atStartOfDay(ZoneId.systemDefault())), if (intSeries) it.value.toInt() else it.value)
//}
//
////endregion