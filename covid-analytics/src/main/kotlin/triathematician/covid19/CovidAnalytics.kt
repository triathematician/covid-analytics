package triathematician.covid19

import triathematician.population.lookupPopulation
import triathematician.timeseries.*
import triathematician.util.log
import java.time.LocalDate

const val DEATHS = "Deaths"
const val DEATHS_PER_MILLION = "Deaths (per million)"
const val CASES = "Confirmed"
const val CASES_PER_MILLION = "Confirmed (per million)"
const val RECOVERED = "Recovered"
const val ACTIVE = "Active"

fun main() {
//    `compute moving average and doubling time series`(DEATHS_PER_MILLION, 0.1)

//    reportCountyTrends(CASES_PER_MILLION, min = 100.0)
//    reportStateTrends(CASES_PER_MILLION, min = 100.0)
//    reportCountryTrends(CASES_PER_MILLION, min = 100.0)

    reportCountyTrends(DEATHS_PER_MILLION, min = 0.1)
    reportStateTrends(DEATHS_PER_MILLION, min = 0.0)
    reportCountryTrends(DEATHS_PER_MILLION, min = 0.1)
}

fun reportStateTrends(metric: String = DEATHS, min: Double = 5.0) {
    println("----")
    println("US STATE HOTSPOTS ($metric)")
    reportHotspots(metric = metric, idFilter = US_STATE_ID_FILTER, metricFilter = { it >= min })
}

fun reportCountyTrends(metric: String = DEATHS, min: Double = 5.0) {
    println("----")
    println("US COUNTY HOTSPOTS ($metric)")
    reportHotspots(metric = metric, idFilter = US_COUNTY_ID_FILTER, metricFilter = { it >= min })
}

fun reportCountryTrends(metric: String = DEATHS, min: Double = 5.0) {
    println("----")
    println("COUNTRY HOTSPOTS ($metric)")
    reportHotspots(metric = metric, idFilter = COUNTRY_ID_FILTER, metricFilter = { it >= min })
}

//region FILTER UTILS

private val US_STATE_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 1 }
private val US_COUNTY_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 }
private val COUNTRY_ID_FILTER: (String) -> Boolean = { !US_STATE_ID_FILTER(it) && !US_COUNTY_ID_FILTER(it) }

//endregion

//region REPORTING ANALYTICS

/** Reports on average increases and doubling time trends. */
fun reportHotspots(
        metric: String = DEATHS,
        idFilter: (String) -> Boolean = { true },
        metricFilter: (Double) -> Boolean = { it >= 5 }
) {
    println("location\tmetric\tvalue\tchange (avg)\tdoubling time (days)\tseverity (change)\tseverity (doubling)\tseverity (total)")
    CsseCovid19DailyReports.allTimeSeriesData()
            .filter { idFilter(it.id) }
            .filter { lookupPopulation(it.id)?.let { it > 50000 } ?: false }
            .filter { it.metric == metric && metricFilter(it.lastValue) }
            .filter { it.values.movingAverage(5).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .flatMap { it.hotspotInfo() }
            .sortedByDescending { (it[7] as Int * 10000) + (it[3] as Double) }
            .forEach { it.log() }
}

fun MetricTimeSeries.hotspotInfo(): List<List<Any>> {
    val changes = values.changes().movingAverage(5)
    val doublings = values.movingAverage(5).doublingTimes()

    return listOf(hotspotInfo(id, metric, values.last(), changes.last(), doublings.last()),
            hotspotInfo("$id -1", "$metric -1", values.penultimate()!!, changes.penultimate()!!, doublings.penultimate()!!),
            hotspotInfo("$id -2", "$metric -2", values.thirdToLast()!!, changes.thirdToLast()!!, doublings.thirdToLast()!!)
    )
}

private fun hotspotInfo(id: String, metric: String, value: Any, lastDelta: Double, lastDoubling: Double): List<Any> {
    val riskDelta = risk_PerCapitaDeathsPerDay(lastDelta)
    val riskDoubling = risk_DoublingTime(lastDoubling)
    return listOf(id, metric, value, lastDelta, lastDoubling, riskDelta.level, riskDoubling.level, riskDelta.level + riskDoubling.level)
}

private fun <X> List<X>.penultimate() = getOrNull(size - 2)
private fun <X> List<X>.thirdToLast() = getOrNull(size - 3)

fun `compute moving average and doubling time series`(metric: String, min: Double) {
    dailyReports()
            .filter { COUNTRY_ID_FILTER(it.id) }
            .filter { it.metric == metric && it.lastValue >= min }
            .filter { it.values.movingAverage(3).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .sortedByDescending { it.lastValue }
            .sortedByDescending { it.values.changes().movingAverage(3).last() }
            .forEach {
                it.log()
                it.values.changes().movingAverage(5).log("  ")
                it.values.movingAverage(5).doublingTimes().log("  ")
                it.values.changes().movingAverage(5).map { risk_PerCapitaDeathsPerDay(it).level }.log("  ")
                it.values.movingAverage(5).doublingTimes().map { risk_DoublingTime(it).level }.log("  ")
            }
}

fun `compute long term trends for italy and south korea`() {
    val italy = intTimeSeries("Italy", DEATHS, LocalDate.of(2020, 2, 21),
            listOf(1, 2, 3, 7, 10, 12, 17, 21, 29, 34, 52, 79, 107, 148, 197, 233, 366, 463, 631, 827, 827, 1266, 1441, 1809, 2158, 2503, 2978, 3405, 4032, 4825, 5476, 6077, 6820, 7503, 8215, 9134))
    italy.values.changes().movingAverage(7).log("ITALY deltas\t")
    italy.values.movingAverage(7).doublingTimes().log("   doubling\t")

    val `south korea` = intTimeSeries("Italy", DEATHS, LocalDate.of(2020, 2, 21),
            listOf(1, 2, 2, 6, 8, 10, 12, 13, 13, 16, 17, 28, 28, 35, 35, 42, 44, 50, 53, 54, 60, 66, 66, 72, 75, 75, 81, 84, 91, 94, 102, 111, 111, 120, 126, 131, 139))
    `south korea`.values.changes().movingAverage(7).log("SOUTH KOREA deltas\t")
    `south korea`.values.movingAverage(7).doublingTimes().log("   doubling\t")
}

//endregion

//region COVID RISK FUNCTIONS

//endregion