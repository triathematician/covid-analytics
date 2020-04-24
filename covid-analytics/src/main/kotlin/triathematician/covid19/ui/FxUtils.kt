package triathematician.covid19.ui

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Slider
import javafx.util.StringConverter
import tornadofx.*
import triathematician.timeseries.MetricTimeSeries
import triathematician.util.DateRange
import triathematician.util.monthDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

typealias DataPoints = List<Pair<Number, Number>>

/** Creates spinner for editing range of ints. */
fun EventTarget.editablespinner(range: IntRange) = spinner(range.first, range.last, range.first, 1) {
    isEditable = true
}

/** Creates spinner for editing range of ints. */
fun EventTarget.intslider(range: IntRange, op: Slider.() -> Unit = {}) = slider(range.first, range.last, range.first) {
    isSnapToTicks = true
    majorTickUnit = 1.0
    minorTickCount = 0
    op()
}

//region LineChart XF

/** Creates line chart. */
fun EventTarget.linechart(title: String, xTitle: String, yTitle: String,  op: LineChart<Number, Number>.() -> Unit = {}): LineChart<Number, Number> {
    return linechart(title, NumberAxis().apply { label = xTitle }, NumberAxis().apply { label = yTitle }, op)
}

/** Set chart series as list of [ChartDataSeries]. */
var LineChart<Number, Number>.dataSeries: List<ChartDataSeries>
    get() = listOf()
    set(value) {
        data.clear()
        value.forEach {
            this.series(it.id, it.points.map { xy(it.first, it.second) }.asObservable())
        }
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
    override fun toString(i: Number) = day0.plusDays(i.toLong()).monthDay
    override fun fromString(s: String): Number = TODO()
}

//endregion