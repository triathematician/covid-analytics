/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.covid19.coda.utils

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Slider
import javafx.scene.control.Spinner
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.util.StringConverter
import tornadofx.*
import tri.util.logCsv
import tri.util.userFormat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

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
fun EventTarget.editablespinner(range: LongRange) = spinner(range.first, range.last, range.first, 1) {
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

//region TABLE UTILS

fun <X> copyTableDataToClipboard(table: TableView<X>) {
    val stream = ByteArrayOutputStream()
    val printer = PrintStream(stream)
    table.columns.map { it.text }.logCsv(printer)
    table.items.forEach { row ->
        table.columns.map { it.getCellData(row).forPrinting() }.logCsv(printer)
    }

    val string = stream.toString(Charset.defaultCharset())
    val clipboardContent = ClipboardContent().apply { putString(string) }
    Clipboard.getSystemClipboard().setContent(clipboardContent)

    println(string)
}

private fun Any?.forPrinting() = when (this) {
    is DoubleArray -> toList().joinToString(";")
    is Array<*> -> listOf(*this).joinToString("; ")
    else -> toString()
}

//endregion
