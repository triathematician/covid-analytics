package tri.timeseries

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import tri.area.AreaInfo
import java.time.LocalDate

/**
 * Aggregates all time series for a single region.
 */
class RegionTimeSeries @JsonCreator constructor(var area: AreaInfo, var metrics: MutableList<MetricInfo> = mutableListOf()) {

    constructor(area: AreaInfo, vararg metrics: MetricTimeSeries): this(area) {
        metrics.forEach { this += it }
    }

    constructor(area: AreaInfo, vararg metrics: MetricInfo): this(area, mutableListOf(*metrics))

    operator fun plusAssign(m: MetricTimeSeries) {
        metrics.add(MetricInfo(m.metric, m.intSeries, m.start, m.defValue, m.values))
    }

}

/** Simple version of time series metric. */
class MetricInfo(var id: String, var intSeries: Boolean, var start: LocalDate, var defValue: Number, @get:JsonIgnore var values: List<Number>) {

    @get:JsonProperty("values")
    var simpleValues: List<Any>
        get() = when {
            intSeries -> values.map { it.toInt() }
            else -> values.map { it.toFloat() }
        }
        set(value) {
            values = value.map {
                when (it) {
                    is Number -> it
                    else -> it.toString().toDoubleOrNull() ?: throw IllegalArgumentException("Not a number: $it")
                }
            }
        }

    fun toMetricTimeSeries(area: AreaInfo) = MetricTimeSeries(area, id, intSeries, defValue.toDouble(), start, values.map { it.toDouble() })

}