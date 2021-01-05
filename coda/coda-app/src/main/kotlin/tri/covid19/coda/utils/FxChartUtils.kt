package tri.covid19.coda.utils

import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.util.StringConverter
import tornadofx.*
import tri.util.monthDay
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Set chart series as list of [ChartDataSeries]. */
var LineChart<Number, Number>.dataSeries: List<ChartDataSeries>
    get() = listOf()
    set(value) {
        data.clear()
        value.forEach {
            this.series(it.id, it.points.map { xy(it.first, it.second) }.asObservable())
        }
    }

/** Adds a context menu to chart with maximize/restore options. */
internal fun LineChart<*, *>.chartContextMenu() = contextmenu {
    item("Maximize").action { maximizeInParent() }
    item("Restore").action { restoreInParent() }
}

internal fun LineChart<*, *>.maximizeInParent() {
    val p = this.parent
    p.childrenUnmodifiable.filter { it != this && it is LineChart<*, *> }
            .forEach { it.isManaged = false;
/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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
 it.isVisible = false }
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
