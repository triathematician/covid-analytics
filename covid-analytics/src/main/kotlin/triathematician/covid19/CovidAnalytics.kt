package triathematician.covid19

import triathematician.util.log
import java.time.LocalDate

fun main() {
    `compute moving average and doubling time series`()

    println("----")
    val italy = MetricTimeSeries("Italy", "Deaths", LocalDate.of(2020, 2, 21), listOf(1, 2, 3, 7, 10, 12, 17, 21, 29, 34, 52, 79, 107, 148, 197, 233, 366, 463, 631, 827, 827, 1266, 1441, 1809, 2158, 2503, 2978, 3405, 4032, 4825, 5476, 6077, 6820, 7503, 8215, 9134))
    italy.values.changes().movingAverage(7).log("ITALY deltas\t")
    italy.values.movingAverage(7).doublingTimes().log("   doubling\t")

    val `south korea` = MetricTimeSeries("Italy", "Deaths", LocalDate.of(2020, 2, 21), listOf(1, 2, 2, 6, 8, 10, 12, 13, 13, 16, 17, 28, 28, 35, 35, 42, 44, 50, 53, 54, 60, 66, 66, 72, 75, 75, 81, 84, 91, 94, 102, 111, 111, 120, 126, 131, 139))
    `south korea`.values.changes().movingAverage(7).log("SOUTH KOREA deltas\t")
    `south korea`.values.movingAverage(7).doublingTimes().log("   doubling\t")

    println("----")
    println("HOTSPOTS")
    println("location\tmetric\tvalue\tavg_change\tdoubling_time")
    CovidDailyReports.timeSeriesData3()
            .filter { ", US" in it.id && it.id.count { it == ',' } == 1 }
            .filter { it.metric == "Deaths" && it.lastValue >= 5 }
            .filter { it.values.movingAverage(5).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .sortedByDescending { it.lastValue }
//            .filter { it.values.movingAverage(3).doublingTimes().last() < 4 }
            .sortedByDescending { it.values.changes().movingAverage(5).last() }
            .map { listOf(it.id, it.metric, it.values.last(),
                    it.values.changes().movingAverage(5).last(),
                    it.values.movingAverage(5).doublingTimes().last()) }
            .forEach { it.log() }
}

fun `compute moving average and doubling time series`(metric: String = "Deaths") {
    CovidDailyReports.timeSeriesData3()
            .filter { it.metric == metric && it.lastValue >= 2 }
            .filter { it.values.movingAverage(3).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .sortedByDescending { it.lastValue }
            .sortedByDescending { it.values.changes().movingAverage(3).last() }
            .forEach {
                it.log()
                it.values.changes().movingAverage(3).log("  ")
                it.values.movingAverage(3).doublingTimes().log("  ")
            }
}