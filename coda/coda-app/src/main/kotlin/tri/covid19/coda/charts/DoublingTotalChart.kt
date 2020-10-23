package tri.covid19.coda.charts

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.attachTo
import tornadofx.gridpaneConstraints
import tri.covid19.coda.history.changeDoublingDataSeries
import tri.covid19.coda.utils.chartContextMenu
import tri.covid19.coda.utils.dataSeries
import tri.covid19.coda.utils.series
import tri.timeseries.TimeSeries

/** Displays total on x-axis, doubling time on vertical axis. */
class DoublingTotalChart : LineChart<Number, Number>(NumberAxis(0.0, 56.0, 7.0), NumberAxis()) {

    init {
        gridpaneConstraints { vhGrow = Priority.ALWAYS }
        animated = false
        createSymbols = false
        isLegendVisible = false
        axisSortingPolicy = SortingPolicy.NONE
        chartContextMenu()
    }

    var series: Collection<TimeSeries>
        get() = TODO()
        set(value) {
            dataSeries = value.map { it.changeDoublingDataSeries(1) }
                    .map { series(it.first.areaId, it.first.domain.shift(1, 0), it.first, it.second) }
        }

}

/** Creates line chart. */
fun EventTarget.doublingtotalchart(title: String, xTitle: String, yTitle: String, op: DoublingTotalChart.() -> Unit = {}) = DoublingTotalChart()
        .attachTo(this, op) {
            it.title = title
            it.xAxis.label = xTitle
            it.yAxis.label = yTitle
        }