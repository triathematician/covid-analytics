package tri.covid19.forecaster.utils

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.util.StringConverter
import tornadofx.*
import tri.util.monthDay
import java.time.LocalDate

typealias DataPoints = List<Pair<Number, Number>>

//region BUILDER UTILS

/** Text field with autocomplete suppport. */
fun EventTarget.autotextfield(values: Collection<String> = listOf(), op: TextField.() -> Unit = {}) = AutocompleteTextField(values.toSortedSet()).attachTo(this, op)

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

/** Creates line chart. */
fun EventTarget.linechart(title: String, xTitle: String, yTitle: String, xLog: Boolean = false, yLog: Boolean = false,
                          op: LineChart<Number, Number>.() -> Unit = {}): LineChart<Number, Number> {
    return linechart(title, (if (xLog) LogAxis() else NumberAxis()).apply { label = xTitle },
            (if (yLog) LogAxis() else NumberAxis()).apply { label = yTitle }, op)
}

/** Creates line chart. */
fun EventTarget.linechartRangedOnFirstSeries(title: String, xTitle: String, yTitle: String, xLog: Boolean = false, yLog: Boolean = false,
                          op: LineChart<Number, Number>.() -> Unit = {}): LineChart<Number, Number> {
    return LineChartOnFirstSeries((if (xLog) LogAxis() else NumberAxis()).apply { label = xTitle },
            (if (yLog) LogAxis() else NumberAxis()).apply { label = yTitle }, 3.0).attachTo(this, op) { it.title = title }
}

/** Creates line chart. */
fun EventTarget.linechartRangedOnFirstSeries(title: String, xAxis: NumberAxis, yAxis: NumberAxis,
                                             op: LineChart<Number, Number>.() -> Unit = {}): LineChart<Number, Number> {
    return LineChartOnFirstSeries(xAxis, yAxis, 3.0).attachTo(this, op) { it.title = title }
}

//endregion

//region LineChart XF

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