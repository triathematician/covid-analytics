package tri.covid19.coda.charts

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.attachTo
import tornadofx.gridpaneConstraints
import tri.covid19.coda.utils.chartContextMenu
import tri.covid19.coda.utils.dataSeries
import tri.covid19.coda.utils.series
import tri.timeseries.MetricTimeSeries

/** Displays total on x-axis, doubling time on vertical axis. */
class DeathCaseChart : LineChart<Number, Number>(NumberAxis(), NumberAxis()) {

    init {
        gridpaneConstraints { vhGrow = Priority.ALWAYS }
        animated = false
        createSymbols = false
        isLegendVisible = false
        axisSortingPolicy = SortingPolicy.NONE
        chartContextMenu()
    }

    /** Set mapping of deaths (first set of series) to cases (second set of series). */
    var series: Pair<List<MetricTimeSeries>, List<MetricTimeSeries>>
        get() = TODO()
        set(value) {
            val commonIndices = value.first.indices.intersect(value.second.indices)
            dataSeries = commonIndices.map { value.first[it] to value.second[it] }
                    .map { series(it.first.region.id, it.first, it.second) }
        }

}

/** Creates line chart. */
fun EventTarget.deathcasechart(title: String, xTitle: String, yTitle: String, op: DeathCaseChart.() -> Unit = {}) = DeathCaseChart()
        .attachTo(this, op) {
            it.title = title
            it.xAxis.label = xTitle
            it.yAxis.label = yTitle
        }