package tri.covid19.forecaster.charts

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.attachTo
import tornadofx.gridpaneConstraints
import tri.covid19.forecaster.history.changeDoublingDataSeries
import tri.covid19.forecaster.utils.chartContextMenu
import tri.covid19.forecaster.utils.dataSeries
import tri.covid19.forecaster.utils.series
import tri.timeseries.MetricTimeSeries

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

    var series: Collection<MetricTimeSeries>
        get() = TODO()
        set(value) {
            dataSeries = value.map { it.changeDoublingDataSeries(1) }
                    .map { series(it.first.region.id, it.first.domain.shift(1, 0), it.first, it.second) }
        }

}

/** Creates line chart. */
fun EventTarget.doublingtotalchart(title: String, xTitle: String, yTitle: String, op: DoublingTotalChart.() -> Unit = {}) = DoublingTotalChart()
        .attachTo(this, op) {
            it.title = title
            it.xAxis.label = xTitle
            it.yAxis.label = yTitle
        }