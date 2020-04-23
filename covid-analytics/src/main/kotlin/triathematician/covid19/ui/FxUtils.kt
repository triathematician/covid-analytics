package triathematician.covid19.ui

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.util.StringConverter
import tornadofx.linechart
import tornadofx.spinner
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.DateRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

typealias DataPoints = List<Pair<Number, Number>>

/** Creates spinner for editing range of ints. */
fun EventTarget.editableSpinner(range: IntRange) = spinner(range.first, range.last, range.first, 1) {
    isEditable = true
}

//region LineChart XF

/** Creates line chart. */
fun EventTarget.linechart(title: String, xTitle: String, yTitle: String,  op: LineChart<Number, Number>.() -> Unit = {}): LineChart<Number, Number> {
    return linechart(title, NumberAxis().apply { label = xTitle }, NumberAxis().apply { label = yTitle }, op)
}

/** Shortcut for chart data point. */
fun xy(x: Number, y: Number) = XYChart.Data(x, y)

/** Set chart line width for all series. */
var LineChart<*, *>.lineWidth: String
    get() = "1px"
    set(value) = data.forEach {
        it.nodeProperty().get().style = "-fx-stroke-width: $value"
    }

fun lineChartWidthForCount(count: Int) = when {
    count <= 5 -> "2px"
    count <= 10 -> "1.5px"
    count <= 20 -> "1px"
    else -> "0.8px"
}

/** For labeling axis by dates. */
fun axisLabeler(day0: LocalDate) = object : StringConverter<Number>() {
    override fun toString(i: Number) = day0.plusDays(i.toLong()).format(DateTimeFormatter.ofPattern("M/d"))
    override fun fromString(s: String): Number = TODO()
}

//endregion

/** A named set of (x,y) data points. */
data class DataSeries(var id: String, var points: DataPoints) {

    /** Construct series with given date range. */
    constructor(id: String, domain: DateRange, inDomain: DateRange? = null, series: MetricTimeSeries) : this(id,
            domain.mapIndexed { i, d -> if (inDomain == null || d in inDomain) i to series[d] else null }.filterNotNull())

    /** Construct series with given date range. */
    constructor(id: String, domain: DateRange, inDomain: DateRange? = null, x: MetricTimeSeries, y: MetricTimeSeries) : this(id,
            domain.mapNotNull { d -> if (inDomain == null || d in inDomain) x[d] to y[d] else null })

}

fun series(id: String?, dom: DateRange?, s: MetricTimeSeries?) = series(id, dom, null as DateRange?, s)
fun series(id: String?, dom: DateRange?, x: MetricTimeSeries?, y: MetricTimeSeries?) = series(id, dom, null, x, y)

/** Factory method that returns null if arguments are null. */
fun series(id: String?, dom: DateRange?, dom2: DateRange? = null, s: MetricTimeSeries?) = when {
    id != null && dom != null && dom2 != null && s != null -> DataSeries(id, dom, dom2, s)
    id != null && dom != null && s != null -> DataSeries(id, dom, null, s)
    else -> null
}

/** Factory method that returns null if arguments are null. */
fun series(id: String?, dom: DateRange?, dom2: DateRange? = null, x: MetricTimeSeries?, y: MetricTimeSeries?) = when {
    id != null && dom != null && dom2 != null && x != null && y != null -> DataSeries(id, dom, dom2, x, y)
    id != null && dom != null && x != null && y != null -> DataSeries(id, dom, null, x, y)
    else -> null
}