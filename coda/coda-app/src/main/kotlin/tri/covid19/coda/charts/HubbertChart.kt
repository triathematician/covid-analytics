package tri.covid19.coda.charts

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.attachTo
import tornadofx.gridpaneConstraints
import tri.covid19.coda.utils.chartContextMenu

/** Displays hubbert chart as growth vs total. */
class HubbertChart: LineChart<Number, Number>(NumberAxis(), NumberAxis(0.0, 0.3, 0.5).apply { isAutoRanging = false }) {

    init {
        gridpaneConstraints { vhGrow = Priority.ALWAYS }
        animated = false
        createSymbols = false
        isLegendVisible = false
        axisSortingPolicy = SortingPolicy.NONE
        chartContextMenu()
    }

}

/** Creates line chart. */
fun EventTarget.hubbertChart(title: String, xTitle: String, yTitle: String, op: HubbertChart.() -> Unit = {}) = HubbertChart()
        .attachTo(this, op) {
            it.title = title
            it.xAxis.label = xTitle
            it.yAxis.label = yTitle
        }