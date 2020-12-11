package tri.timeseries

import tri.area.AreaInfo

/** Manages access to a variety of time series, and provides simple query access. */
open class TimeSeriesQuery(vararg _sources: TimeSeriesProcessor) {

    val sources = _sources.toList()
    /** Load all data into memory, grouped by area. */
    val data by lazy { sources.flatMap { it.data() }.groupByArea() }
    /** Flat version of all data. */
    val flatData = data.flatMap { it.value }
    /** List of all areas in the data. */
    val areas by lazy { data.keys }

    //region QUERIES

    /** Query all data based on area. */
    fun byArea(area: AreaInfo) = data[area] ?: emptyList()
    /** Query all data based on area. */
    fun byArea(areaFilter: (AreaInfo) -> Boolean) = data.filterKeys(areaFilter)

    /** Query by area and metric. */
    fun by(area: AreaInfo, metric: String? = null, qualifier: String? = null) =
            data[area]?.filter { (metric == null || it.metric == metric) && (qualifier == null || it.qualifier == qualifier) } ?: emptyList()

    /** Query all data based on area and metric. */
    fun by(areaFilter: (AreaInfo) -> Boolean, metricFilter: (String) -> Boolean) = data.filterKeys(areaFilter)
            .mapValues { it.value.filter { metricFilter(it.metric) } }
            .filterValues { it.isNotEmpty() }

    /** Query all data based on a generic filter. */
    fun by(filter: (TimeSeries) -> Boolean) = flatData.filter(filter)

    /** Query for daily version of timeseries. */
    open fun daily(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for weekly average version of timeseries. */
    open fun weeklyAverage(area: AreaInfo, metric: String): TimeSeries? = null
    /** Query for weekly total version of timeseries. */
    open fun weeklyTotal(area: AreaInfo, metric: String): TimeSeries? = null

    //endregion

}