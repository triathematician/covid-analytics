package tri.covid19.forecaster.charts

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.attachTo
import tornadofx.gridpaneConstraints
import tri.covid19.forecaster.utils.chartContextMenu

/** Displays total on x-axis, doubling time on vertical axis. */
class DeathCaseChart : LineChart<Number, Number>(NumberAxis(), NumberAxis()) {

    init {
        gridpaneConstraints { vhGrow = Priority.ALWAYS }
        animated = false
        createSymbols = false
        isLegendVisible = false
        chartContextMenu()
    }

}

/** Creates line chart. */
fun EventTarget.deathcasechart(title: String, xTitle: String, yTitle: String, op: DeathCaseChart.() -> Unit = {}) = DeathCaseChart()
        .attachTo(this, op) {
            it.title = title
            it.xAxis.label = xTitle
            it.yAxis.label = yTitle
        }