package tri.covid19.forecaster.utils

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