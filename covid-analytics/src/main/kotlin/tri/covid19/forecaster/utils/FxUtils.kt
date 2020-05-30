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

//region FORMATTERS

/** Converter for formatting number automatically. */
object UserStringConverter : StringConverter<Number>() {
    override fun toString(n: Number) = n.userFormat()
    override fun fromString(s: String) = TODO("Not yet implemented")
}

//endregion