package triathematician.covid19

import triathematician.util.rangeTo
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** Time series of a single metric. */
data class MetricTimeSeries(var id: String = "", var metric: String = "", var start: LocalDate = LocalDate.now(), var values: List<Int> = listOf()) {

    constructor(id: String, metric: String, start: LocalDate, value: Int): this(id, metric, start, listOf(value))

    val lastValue
        get() = values.lastOrNull() ?: 0
    val end: LocalDate
        get() = start.plusDays((values.size - 1).toLong())

    operator fun get(date: LocalDate) = values.getOrElse(indexOf(date)) { 0 }

    private fun indexOf(date: LocalDate) = ChronoUnit.DAYS.between(start, date).toInt()

}

/** Merge a bunch of time series by id and metric. */
fun List<MetricTimeSeries>.regroupAndMerge() = groupBy { listOf(it.id, it.metric) }
        .mapValues { it.value.merge() }
        .values
        .onEach { it.coerceIncreasing() }
        .onEach { it.startWithNoMoreZerosThan(5) }

/** Merge a bunch of separate time series into a single time series object. */
private fun List<MetricTimeSeries>.merge(): MetricTimeSeries = reduce { s1, s2 ->
    val minDate = minOf(s1.start, s2.start)
    val maxDate = maxOf(s1.end, s2.end)
    val series = (minDate..maxDate).map { maxOf(s1[it], s2[it]) }
    MetricTimeSeries(s1.id, s1.metric, minDate, series)
}

/** Coerces time series so values always increase. */
private fun MetricTimeSeries.coerceIncreasing() {
    val res = values.toMutableList()
    for (i in 1 until res.size) {
        res[i] = max(res[i-1], res[i])
    }
    values = res
}

/** If time series starts with more than max zeros, trim so that it has max zeros. */
private fun MetricTimeSeries.startWithNoMoreZerosThan(max: Int) {
    val firstNonZeroIndex = values.indexOfFirst { it > 0 }
    if (firstNonZeroIndex > max) {
        values = values.drop(firstNonZeroIndex - max - 1)
        start = start.plusDays((firstNonZeroIndex - max - 1).toLong())
    }
}