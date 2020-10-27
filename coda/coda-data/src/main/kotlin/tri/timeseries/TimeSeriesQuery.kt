package tri.timeseries

import tri.area.AreaInfo

/** Manages access to a variety of time series, and provides simple query access. */
open class TimeSeriesQuery(vararg _sources: TimeSeriesProcessor) {

    val sources = _sources.toList()

    /** Get all data as a sequence, one set of series for each source. */
    fun data() = sources.asSequence().map { it.data() }
    /** Get all areas in the source. */
    fun areas() = data().flatMap { it.map { it.area }.asSequence() }.toSet()

    //region QUERIES

    /** Query all data based on area. */
    fun byArea(areaFilter: (AreaInfo) -> Boolean) = by { areaFilter(it.area) }

    /** Query all data based on a generic filter. */
    fun by(filter: (TimeSeries) -> Boolean): List<TimeSeries> = data().flatMap { it.filter(filter).asSequence() }.toList()

    //endregion

}