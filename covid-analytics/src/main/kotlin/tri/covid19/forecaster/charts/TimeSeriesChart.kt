package tri.covid19.forecaster.charts

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.attachTo
import tornadofx.gridpaneConstraints
import tri.covid19.forecaster.utils.*
import tri.covid19.forecaster.utils.chartContextMenu
import tri.util.DateRange

/** Displays time series as quantity vs. time. */
class TimeSeriesChart(logScale: Boolean = false) : LineChart<Number, Number>(NumberAxis(), if (logScale) LogAxis() else NumberAxis()) {

    init {
        gridpaneConstraints { vhGrow = Priority.ALWAYS }
        animated = false
        createSymbols = false
        isLegendVisible = false
        chartContextMenu()
    }

    /**
     * Updates chart data using given range of dates and data.
     * @param domain domain, including the date indicated by "0" in x values
     * @param series data to display
     */
    fun setTimeSeries(domain: DateRange, series: List<ChartDataSeries>) {
        dataSeries = series
        (xAxis as NumberAxis).tickLabelFormatter = axisLabeler(domain.start)
    }
}

/** Creates line chart. */
fun EventTarget.timeserieschart(title: String, xTitle: String, yTitle: String, log: Boolean = false, op: TimeSeriesChart.() -> Unit = {}) = TimeSeriesChart(log)
        .attachTo(this, op) {
            it.title = title
            it.xAxis.label = xTitle
            it.yAxis.label = yTitle
        }