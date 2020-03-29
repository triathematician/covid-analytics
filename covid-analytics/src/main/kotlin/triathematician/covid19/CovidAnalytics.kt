package triathematician.covid19

import triathematician.util.log
import java.time.LocalDate

fun main() {
    `compute moving average and doubling time series`()
//    reportCountyTrends(CASES, 50)
//    reportStateTrends(CASES, 50)
//    reportCountryTrends(CASES, 50)

//    reportCountyTrends(DEATHS, 4)
//    reportStateTrends(DEATHS, 5)
//    reportCountryTrends(DEATHS, 10)
}

fun reportStateTrends(metric: String = DEATHS, min: Int = 5) {
    println("----")
    println("US STATE HOTSPOTS ($metric)")
    reportHotspots(metric = metric, idFilter = US_STATE_ID_FILTER, metricFilter = { it >= min })
}

fun reportCountyTrends(metric: String = DEATHS, min: Int = 5) {
    println("----")
    println("US COUNTY HOTSPOTS ($metric)")
    reportHotspots(metric = metric, idFilter = US_COUNTY_ID_FILTER, metricFilter = { it >= min })
}

fun reportCountryTrends(metric: String = DEATHS, min: Int = 5) {
    println("----")
    println("COUNTRY HOTSPOTS ($metric)")
    reportHotspots(metric = metric, idFilter = COUNTRY_ID_FILTER, metricFilter = { it >= min })
}

//region FILTER UTILS

const val DEATHS = "Deaths"
const val CASES = "Confirmed"
const val RECOVERED = "Recovered"
const val ACTIVE = "Active"

private val US_STATE_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 1 }
private val US_COUNTY_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 }
private val COUNTRY_ID_FILTER: (String) -> Boolean = { !US_STATE_ID_FILTER(it) && !US_COUNTY_ID_FILTER(it) }

//endregion

//region REPORTING ANALYTICS

/** Reports on average increases and doubling time trends. */
fun reportHotspots(
        metric: String = "Deaths",
        idFilter: (String) -> Boolean = { true },
        metricFilter: (Int) -> Boolean = { it > 5 }
) {
    println("location\tmetric\tvalue\tchange (avg)\tdoubling time (days)\tseverity (change)\tseverity (doubling)\tseverity (total)")
    CovidDailyReports.timeSeriesData1()
            .filter { idFilter(it.id) }
            .filter { it.metric == metric && metricFilter(it.lastValue) }
            .filter { it.values.movingAverage(5).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .map { it.hotspotInfo() }
            .sortedByDescending { (it[7] as Int * 10000) + (it[3] as Double) }
            .forEach { it.log() }
}

fun MetricTimeSeries.hotspotInfo(): List<Any> {
    val lastDelta = values.changes().movingAverage(5).last()
    val riskDelta = riskGivenRecentCountPerDay(lastDelta)
    val lastDoubling = values.movingAverage(5).doublingTimes().last()
    val riskDoubling = riskGivenRecentDoublingRate(lastDoubling)
    return listOf(id, metric, values.last(), lastDelta, lastDoubling, riskDelta.level, riskDoubling.level, riskDelta.level + riskDoubling.level)
}

fun `compute moving average and doubling time series`(metric: String = "Deaths") {
    dailyReports()
            .filter { COUNTRY_ID_FILTER(it.id) }
            .filter { it.metric == metric && it.lastValue >= 2 }
            .filter { it.values.movingAverage(3).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .sortedByDescending { it.lastValue }
            .sortedByDescending { it.values.changes().movingAverage(3).last() }
            .forEach {
                it.log()
                it.values.changes().movingAverage(5).log("  ")
                it.values.movingAverage(5).doublingTimes().log("  ")
                it.values.changes().movingAverage(5).map { riskGivenRecentCountPerDay(it).level }.log("  ")
                it.values.movingAverage(5).doublingTimes().map { riskGivenRecentDoublingRate(it).level }.log("  ")
            }
}

fun `compute long term trends for italy and south korea`() {
    val italy = MetricTimeSeries("Italy", "Deaths", LocalDate.of(2020, 2, 21), listOf(1, 2, 3, 7, 10, 12, 17, 21, 29, 34, 52, 79, 107, 148, 197, 233, 366, 463, 631, 827, 827, 1266, 1441, 1809, 2158, 2503, 2978, 3405, 4032, 4825, 5476, 6077, 6820, 7503, 8215, 9134))
    italy.values.changes().movingAverage(7).log("ITALY deltas\t")
    italy.values.movingAverage(7).doublingTimes().log("   doubling\t")

    val `south korea` = MetricTimeSeries("Italy", "Deaths", LocalDate.of(2020, 2, 21), listOf(1, 2, 2, 6, 8, 10, 12, 13, 13, 16, 17, 28, 28, 35, 35, 42, 44, 50, 53, 54, 60, 66, 66, 72, 75, 75, 81, 84, 91, 94, 102, 111, 111, 120, 126, 131, 139))
    `south korea`.values.changes().movingAverage(7).log("SOUTH KOREA deltas\t")
    `south korea`.values.movingAverage(7).doublingTimes().log("   doubling\t")
}

//endregion

//region COVID RISK FUNCTIONS

/** Estimated risk based on doubling rates. */
fun riskGivenRecentDoublingRate(rate: Double) = when {
    rate < 2 -> RiskLevel.CRITICAL
    rate < 3 -> RiskLevel.SEVERE
    rate < 4 -> RiskLevel.URGENT
    rate < 7 -> RiskLevel.MODERATE
    rate < 14 -> RiskLevel.MARGINAL
    else -> RiskLevel.MINOR
}

/** Estimated risk based on recent deaths per day. */
fun riskGivenRecentCountPerDay(dailyAverage: Double, baseLevel: Int = 10) = when {
    dailyAverage >= 50*baseLevel -> RiskLevel.CRITICAL
    dailyAverage >= 10*baseLevel -> RiskLevel.SEVERE
    dailyAverage >= 2*baseLevel -> RiskLevel.URGENT
    dailyAverage >= 0.5*baseLevel -> RiskLevel.MODERATE
    dailyAverage >= 0.1*baseLevel -> RiskLevel.MARGINAL
    else -> RiskLevel.MINOR
}

enum class RiskLevel(var level: Int) {
    MINOR(0),
    MARGINAL(1),
    MODERATE(2),
    URGENT(3),
    SEVERE(4),
    CRITICAL(5)
}

//endregion