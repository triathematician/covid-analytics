package triathematician.timeseries

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import triathematician.regions.RegionLookup
import triathematician.util.format
import triathematician.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

/**
 * Aggregates all time series for a single region.
 */
class RegionTimeSeries(var region: RegionInfo, var metrics: MutableList<MetricInfo> = mutableListOf()) {

    constructor(region: String, metrics: List<MetricTimeSeries>): this(RegionLookup(region)) {
        metrics.forEach { this += it }
    }

    operator fun plusAssign(m: MetricTimeSeries) {
        require(m.id == region.id) { "Expected ${region.id} but was ${m.id}"}
        metrics.add(MetricInfo(m.metric, m.intSeries, m.start, m.defValue, m.values))
    }

}

private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

class MetricInfo(var id: String, var intSeries: Boolean, @get:JsonIgnore var start: LocalDate, var defValue: Number, @get:JsonIgnore var values: List<Number>) {

    @get:JsonProperty("start")
    var dateString: String
        get() = start.toString()
        set(value) {
            start = value.toLocalDate(FORMAT)
        }

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

}