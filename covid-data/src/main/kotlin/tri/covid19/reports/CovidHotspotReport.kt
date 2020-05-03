package tri.covid19.reports

import tri.covid19.DEATHS
import tri.covid19.risk_DoublingTime
import tri.covid19.risk_PerCapitaDeathsPerDay
import tri.regions.PopulationLookup
import tri.timeseries.*

//
//import tri.covid19.*
//import tri.covid19.COUNTRY_ID_FILTER
//import tri.covid19.US_COUNTY_ID_FILTER
//import tri.covid19.US_STATE_ID_FILTER
//import tri.regions.lookupPopulation
//import tri.timeseries.MetricTimeSeries
//import tri.timeseries.deltas
//import tri.timeseries.doublingTimes
//import tri.timeseries.movingAverage
//import tri.util.log
//import kotlin.math.absoluteValue
//
////
//// computes "hotspot" metrics, looking at growth/trends of mortality rates
////
//
//fun main() {
//    printHotspotMovers("US COUNTY HOTSPOTS (MAJOR MOVES)", DEATHS_PER_100K, 0, idFilter = US_COUNTY_ID_FILTER, min = 1.0, minDelta = 3)
//    printHotspotMovers("US STATE HOTSPOTS (MAJOR MOVES)", DEATHS_PER_100K, 50000, idFilter = US_STATE_ID_FILTER, min = 0.0)
//    printHotspotMovers("COUNTRY HOTSPOTS (MAJOR MOVES)", DEATHS_PER_100K, 100000, idFilter = COUNTRY_ID_FILTER, min = 0.01)
//
//    println("")
//    println("")
//    println("")
//
//    printHotspots("US STATE HOTSPOTS", DEATHS_PER_100K, 0, idFilter = US_STATE_ID_FILTER, min = 0.0)
//    printHotspots("US COUNTY HOTSPOTS", DEATHS_PER_100K, 50000, idFilter = US_COUNTY_ID_FILTER, min = 1.0)
//    printHotspots("COUNTRY HOTSPOTS", DEATHS_PER_100K, 100000, idFilter = COUNTRY_ID_FILTER, min = 0.01)
//}
//
///** Print regions where hotspots of given metric have gotten much better or worse. */
//fun printHotspotMovers(header: String,
//                       metric: String = DEATHS,
//                       minPopulation: Int = 50000,
//                       idFilter: (String) -> Boolean = { true },
//                       min: Double = 5.0,
//                       minDelta: Int = 2) {
//    println("----")
//    println("$header ($metric)")
//    println("----")
//    println(HotspotInfo.header.joinToString("\t"))
//    hotspotPerCapitaInfo(metric, minPopulation, idFilter, { it >= min })
//            .filter { it.severityChange.absoluteValue >= minDelta && it.totalSeverity != it.severityChange }
//            .sortedByDescending { it.severityChange }
//            .forEach { it.toList().log() }
//}
//
///** Print hotspots of given metric. */
//fun printHotspots(header: String,
//                  metric: String = DEATHS,
//                  minPopulation: Int = 50000,
//                  idFilter: (String) -> Boolean = { true },
//                  min: Double = 5.0) {
//    println("----")
//    println("$header ($metric)")
//    println("----")
//    println(HotspotInfo.header.joinToString("\t"))
//    hotspotPerCapitaInfo(metric, minPopulation, idFilter, { it >= min })
//            .sortedByDescending { it.totalSeverity * 10000 + it.value.toDouble() }
//            .forEach { it.toList().log() }
//}
//
///** Compute hotspots of given metric. */
//fun hotspotPerCapitaInfo(metric: String = DEATHS,
//                         minPopulation: Int = 50000,
//                         idFilter: (String) -> Boolean = { true },
//                         valueFilter: (Double) -> Boolean = { it >= 5 })
//        = CovidTimeSeriesSources.dailyReports(idFilter).hotspotPerCapitaInfo(metric, minPopulation, valueFilter)

/** Compute hotspots of given metric. */
fun List<MetricTimeSeries>.hotspotPerCapitaInfo(metric: String = DEATHS,
                                                minPopulation: Int = 50000,
                                                valueFilter: (Double) -> Boolean = { it >= 5 }): List<HotspotInfo> {
    return filter { it.region.population?.let { it > minPopulation } ?: true }
            .filter { it.metric == metric && valueFilter(it.lastValue) }
            .filter { it.values.movingAverage(5).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .flatMap { it.hotspotPerCapitaInfo() }
}

/** Compute hotspot info given time series data. Uses average changes over the last N days. */
fun MetricTimeSeries.hotspotPerCapitaInfo(averageDays: Int = 7, includePriorDays: Boolean = false): List<HotspotInfo> {
    val changes = values.deltas().movingAverage(averageDays)
    val doublings = values.movingAverage(averageDays).doublingTimes()

    val minSize = minOf(changes.size, doublings.size, values.size)
    val info3 = if (minSize >= 3) hotspotPerCapitaInfo(region, "$metric -2", values.thirdToLast(), changes.thirdToLast(), doublings.thirdToLast()) else null
    val info2 = if (minSize >= 2) hotspotPerCapitaInfo(region, "$metric -1", values.penultimate(), changes.penultimate(), doublings.penultimate(), info3) else null
    val info = hotspotPerCapitaInfo(region, metric, values.lastOrNull(), changes.lastOrNull(), doublings.lastOrNull(), info2, info3)

    return if (includePriorDays) listOfNotNull(info, info2, info3) else listOfNotNull(info)
}

/** Compute hotspot info given values and information about the last few days to use when computing trends. */
private fun hotspotPerCapitaInfo(region: RegionInfo, metric: String, value: Number?, dailyChange: Double?, doublingTimeDays: Double?, vararg priorInfo: HotspotInfo?): HotspotInfo? {
    if (value == null || dailyChange == null || doublingTimeDays == null) {
        return null
    }
    val severityByChange = risk_PerCapitaDeathsPerDay(dailyChange)
    val severityByDoubling = risk_DoublingTime(doublingTimeDays)
    val riskTotal =  severityByChange.level + severityByDoubling.level
    val minPrior = priorInfo.mapNotNull { it?.totalSeverity }.min()
    val maxPrior = priorInfo.mapNotNull { it?.totalSeverity }.max()
    val severityChange = when {
        minPrior == null || maxPrior == null -> 0
        minPrior < riskTotal -> riskTotal - minPrior
        else -> riskTotal - maxPrior
    }
    return HotspotInfo(region, metric, value.toDouble(), dailyChange, doublingTimeDays, severityByChange, severityByDoubling, severityChange)
}

private fun <X> List<X>.penultimate() = getOrNull(size - 2)
private fun <X> List<X>.thirdToLast() = getOrNull(size - 3)