package triathematician.timeseries

import java.time.LocalDate

/**
 * Collection of related time series for a single grouping.
 */
class RegionTimeSeries(var id: String = "", var id2: String, var start: LocalDate = LocalDate.now()) {

    /** Stores whether each metric is int series or not. */
    val intSeries = mutableSetOf<String>()
    /** Stores values for each metric. */
    val valuesTable = mutableMapOf<String, List<Double>>()

    val size: Int
        get() = valuesTable.values.map { it.size }.max() ?: 0
    val end: LocalDate
        get() = start.plusDays((size - 1).toLong())

    val timeSeries: List<MetricTimeSeries>
        get() = valuesTable.map { MetricTimeSeries(id, id2, it.key, intSeries.contains(it.key), 0.0, start, it.value) }

    val valuesAsMap: Map<String, Map<LocalDate, Double>>
        get() = timeSeries.map { it.metric to it.valuesAsMap }.toMap()

    //region MUTATORS

    operator fun plusAssign(mts: MetricTimeSeries) {
        if (start < mts.start) {
            // add n 0's to beginning of mts
        } else if (start > mts.start) {
            // adjust start date and add n 0's to beginning of all lists in tables
        } else {
            if (mts.intSeries) {
                intSeries.add(mts.metric)
            }
            valuesTable[mts.metric] = mts.values
        }
    }

    //endregion

}