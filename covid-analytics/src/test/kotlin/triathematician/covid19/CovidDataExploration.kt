package triathematician.covid19

import triathematician.covid19.sources.dailyReports
import triathematician.timeseries.changes
import triathematician.timeseries.doublingTimes
import triathematician.timeseries.intTimeSeries
import triathematician.timeseries.movingAverage
import triathematician.util.log
import java.time.LocalDate

fun `compute moving average and doubling time series`(metric: String, min: Double) {
    dailyReports()
            .filter { COUNTRY_ID_FILTER(it.id) }
            .filter { it.metric == metric && it.lastValue >= min }
            .filter { it.values.movingAverage(3).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .sortedByDescending { it.lastValue }
            .sortedByDescending { it.values.changes().movingAverage(3).last() }
            .forEach {
                it.log()
                it.values.changes().movingAverage(5).log(prefix = "  ")
                it.values.movingAverage(5).doublingTimes().log(prefix = "  ")
                it.values.changes().movingAverage(5).map { risk_PerCapitaDeathsPerDay(it).level }.log(prefix = "  ")
                it.values.movingAverage(5).doublingTimes().map { risk_DoublingTime(it).level }.log(prefix = "  ")
            }
}

fun `compute long term trends for italy and south korea`() {
    val italy = intTimeSeries("Italy", DEATHS, LocalDate.of(2020, 2, 21),
            listOf(1, 2, 3, 7, 10, 12, 17, 21, 29, 34, 52, 79, 107, 148, 197, 233, 366, 463, 631, 827, 827, 1266, 1441, 1809, 2158, 2503, 2978, 3405, 4032, 4825, 5476, 6077, 6820, 7503, 8215, 9134))
    italy.values.changes().movingAverage(7).log(prefix = "ITALY deltas\t")
    italy.values.movingAverage(7).doublingTimes().log(prefix = "   doubling\t")

    val `south korea` = intTimeSeries("Italy", DEATHS, LocalDate.of(2020, 2, 21),
            listOf(1, 2, 2, 6, 8, 10, 12, 13, 13, 16, 17, 28, 28, 35, 35, 42, 44, 50, 53, 54, 60, 66, 66, 72, 75, 75, 81, 84, 91, 94, 102, 111, 111, 120, 126, 131, 139))
    `south korea`.values.changes().movingAverage(7).log(prefix = "SOUTH KOREA deltas\t")
    `south korea`.values.movingAverage(7).doublingTimes().log(prefix = "   doubling\t")
}