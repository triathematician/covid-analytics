package tri.covid19.forecaster.utils

import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Slider
import javafx.scene.control.Spinner
import javafx.scene.control.TextField
import javafx.util.StringConverter
import tornadofx.*
import tri.util.monthDay
import tri.util.userFormat
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

typealias DataPoints = List<Pair<Number, Number>>

//region BUILDER UTILS

/** Text field with autocomplete suppport. */
fun EventTarget.autocompletetextfield(values: Collection<String> = listOf(), op: TextField.() -> Unit = {}) = AutocompleteTextField(values.toSortedSet()).attachTo(this, op)

/** Text field with autocomplete suppport. */
fun EventTarget.autocompletespinner(values: Collection<String> = listOf(), op: Spinner<String>.() -> Unit = {}) = AutocompleteSpinner(values.toSortedSet()).attachTo(this, op)

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

internal fun LineChart<*, *>.maximizeInParent() {
    val p = this.parent
    p.childrenUnmodifiable.filter { it != this && it is LineChart<*, *> }
            .forEach { it.isManaged = false; it.isVisible = false }
}

internal fun LineChart<*, *>.restoreInParent() {
    val p = this.parent
    p.childrenUnmodifiable.forEach { it.isManaged = true; it.isVisible = true }
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

private fun NumberAxis.limitMaxTo(maxOther: Double?, max: Double?, multiplier: Double) {
    when {
        max == null || maxOther == null -> isAutoRanging = true
        maxOther >= max*multiplier -> {
            isAutoRanging = false
            lowerBound = 0.0
            upperBound = (max*multiplier).logRound()
        }
        else -> isAutoRanging = true
    }
}

private fun Double.logRound(): Double {
    val base = 10.0.pow(floor(log10(this)))
    return when {
        base >= this -> base
        2*base >= this -> 2*base
        else -> 5*base
    }
}

//endregion

//region FORMATTERS

/** Converter for formatting number automatically. */
object UserStringConverter : StringConverter<Number>() {
    override fun toString(n: Number) = n.userFormat()
    override fun fromString(s: String) = TODO("Not yet implemented")
}

//endregion