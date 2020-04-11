package triathematician.covid19.reports

import triathematician.covid19.*
import triathematician.covid19.COUNTRY_ID_FILTER
import triathematician.covid19.US_COUNTY_ID_FILTER
import triathematician.covid19.US_STATE_ID_FILTER
import triathematician.covid19.sources.dailyReports
import triathematician.population.lookupPopulation
import triathematician.timeseries.MetricTimeSeries
import triathematician.timeseries.changes
import triathematician.timeseries.doublingTimes
import triathematician.timeseries.movingAverage
import triathematician.util.log
import kotlin.math.absoluteValue

//
// computes "hotspot" metrics, looking at growth/trends of mortality rates
//

fun main() {
    printHotspotMovers("US COUNTY HOTSPOTS (MAJOR MOVES)", DEATHS_PER_100K, idFilter = US_COUNTY_ID_FILTER, min = 1.0, minDelta = 3)
    printHotspotMovers("US STATE HOTSPOTS (MAJOR MOVES)", DEATHS_PER_100K, idFilter = US_STATE_ID_FILTER, min = 0.0)
    printHotspotMovers("COUNTRY HOTSPOTS (MAJOR MOVES)", DEATHS_PER_100K, idFilter = COUNTRY_ID_FILTER, min = 0.01)

    println("")
    println("")
    println("")

    printHotspots("US STATE HOTSPOTS", DEATHS_PER_100K, idFilter = US_STATE_ID_FILTER, min = 0.0)
    printHotspots("US COUNTY HOTSPOTS", DEATHS_PER_100K, idFilter = US_COUNTY_ID_FILTER, min = 1.0)
    printHotspots("COUNTRY HOTSPOTS", DEATHS_PER_100K, idFilter = COUNTRY_ID_FILTER, min = 0.01)
}

fun printHotspotMovers(header: String,
                       metric: String = DEATHS,
                       idFilter: (String) -> Boolean = { true },
                       min: Double = 5.0,
                       minDelta: Int = 2) {
    println("----")
    println("$header ($metric)")
    println("----")
    println("location\tmetric\tvalue\tchange (avg)\tdoubling time (days)\tseverity (change)\tseverity (doubling)\tseverity\tseverity (delta)")
    hotspotInfo(metric, idFilter, { it >= min })
            .filter { (it[8] as Int).absoluteValue >= minDelta && it[7] != it[8] }
            .sortedByDescending { it[8] as Int }
            .forEach { it.log() }
}

fun printHotspots(header: String,
                  metric: String = DEATHS,
                  idFilter: (String) -> Boolean = { true },
                  min: Double = 5.0) {
    println("----")
    println("$header ($metric)")
    println("----")
    println("location\tmetric\tvalue\tchange (avg)\tdoubling time (days)\tseverity (change)\tseverity (doubling)\tseverity\tseverity (delta)")
    hotspotInfo(metric, idFilter, { it >= min })
            .sortedByDescending { (it[7] as Int * 10000) + (it[3] as Double) }
            .forEach { it.log() }
}

fun hotspotInfo(metric: String = DEATHS,
                idFilter: (String) -> Boolean = { true },
                metricFilter: (Double) -> Boolean = { it >= 5 }): List<List<Any>> {
    return dailyReports()
            .filter { idFilter(it.id) }
            .filter { lookupPopulation(it.id)?.let { it > 50000 } ?: false }
            .filter { it.metric == metric && metricFilter(it.lastValue) }
            .filter { it.values.movingAverage(5).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .flatMap { it.hotspotInfo() }
}

fun MetricTimeSeries.hotspotInfo(includePriorDays: Boolean = false): List<List<Any>> {
    val changes = values.changes().movingAverage(5)
    val doublings = values.movingAverage(5).doublingTimes()
    val cleanId = id.removeSuffix(", US")

    val info3 = hotspotInfo("$cleanId -2", "$metric -2", values.thirdToLast()!!, changes.thirdToLast()!!, doublings.thirdToLast()!!)
    val info2 = hotspotInfo("$cleanId -1", "$metric -1", values.penultimate()!!, changes.penultimate()!!, doublings.penultimate()!!, info3)
    val info1 = hotspotInfo(cleanId, metric, values.last(), changes.last(), doublings.last(), info2, info3)

    return if (includePriorDays) listOf(info1, info2, info3) else listOf(info1)
}

private fun hotspotInfo(id: String, metric: String, value: Any, lastDelta: Double, lastDoubling: Double, vararg priorInfo: List<Any>): List<Any> {
    val riskDelta = risk_PerCapitaDeathsPerDay(lastDelta)
    val riskDoubling = risk_DoublingTime(lastDoubling)
    val riskTotal =  riskDelta.level + riskDoubling.level
    val minPrior = priorInfo.map { it[7] as Int}.min()
    val maxPrior = priorInfo.map { it[7] as Int}.max()
    val delta = when {
        minPrior == null || maxPrior == null -> 0
        minPrior < riskTotal -> riskTotal - minPrior
        else -> riskTotal - maxPrior
    }
    return listOf(id, metric, value, lastDelta, lastDoubling, riskDelta.level, riskDoubling.level, riskTotal, delta)
}

private fun <X> List<X>.penultimate() = getOrNull(size - 2)
private fun <X> List<X>.thirdToLast() = getOrNull(size - 3)