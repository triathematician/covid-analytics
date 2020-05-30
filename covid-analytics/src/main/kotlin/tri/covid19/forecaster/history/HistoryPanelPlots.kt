package tri.covid19.forecaster.history

import com.sun.javafx.charts.Legend
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Tooltip
import tornadofx.*
import tri.covid19.forecaster.charts.HubbertChart
import tri.covid19.forecaster.charts.TimeSeriesChart
import tri.covid19.forecaster.charts.hubbertChart
import tri.covid19.forecaster.charts.timeserieschart
import tri.covid19.forecaster.installHoverEffect
import tri.covid19.forecaster.installStandardHoverAndTooltip
import tri.covid19.forecaster.utils.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
class HistoryPanelPlots constructor(val historyPanelModel: HistoryPanelModel, val hubbertPanelModel: HistoryHubbertModel) : View() {

    private lateinit var standardChart: TimeSeriesChart
    private lateinit var hubbertChart: HubbertChart
    private lateinit var legend: Legend

    override val root = borderpane {
        center = gridpane {
            row {
                standardChart = timeserieschart("Historical Data", "Day", "Count", historyPanelModel.logScale)
            }
            row {
                hubbertChart = hubbertChart("Percent Growth vs Total", "Total", "Percent Growth")
            }
        }
        bottom = hbox(alignment = Pos.CENTER) {
            legend = Legend()
            legend.alignment = Pos.CENTER
            val chartLegend = standardChart.childrenUnmodifiable.first { it is Legend } as Legend
            Bindings.bindContent(legend.items, chartLegend.items)
            this += legend
        }
    }

    init {
        historyPanelModel.onChange = { updateBothCharts() }
        hubbertPanelModel.onChange = { updateBothCharts() }
        historyPanelModel._logScale.onChange { resetChartAxes() }
        updateBothCharts()
    }

    /** Resets positioning of chart, must do when axes change. */
    private fun resetChartAxes() {
        // TODO - this needs to reset and relink the legend
        val parent = standardChart.parent
        hubbertChart.removeFromParent()
        standardChart.removeFromParent()
        standardChart = timeserieschart("Historical Data", "Day", "Count", historyPanelModel.logScale)
        parent.add(hubbertChart)
        updateStandardChart()
    }

    /** Update both charts. */
    private fun updateBothCharts() {
        if (this::standardChart.isInitialized) {
            updateStandardChart()
            updateHubbertChart()
        }
    }

    /** Plot counts by date. */
    private fun updateStandardChart() {
        val (domain, series) = historyPanelModel.historicalDataSeries()
        standardChart.setTimeSeries(domain, series)
        standardChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertPanelModel.showPeakCurve.get() && historyPanelModel.perDay) {
            val peak = hubbertPanelModel.peakValue
            val label = hubbertPanelModel.peakLabel
            val peakSeries = domain.mapIndexed { i, _ -> xy(i, peak) }
            standardChart.series(label, peakSeries.asObservable()).also {
                it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
            }
        }

        standardChart.installStandardHoverAndTooltip()
    }

    /** Plot growth vs. total. */
    private fun updateHubbertChart() {
        val series = historyPanelModel.hubbertDataSeries()
        hubbertChart.dataSeries = series
        hubbertChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertPanelModel.showPeakCurve.get()) {
            val max = series.mapNotNull { it.points.map { it.first.toDouble() }.max() }.max()
            if (max != null) {
                val peak = hubbertPanelModel.peakValue
                val label = hubbertPanelModel.peakLabel
                val min = peak / 0.3
                val peakSeries = (0..100).map { min + (max - min) * it / 100.0 }.map { xy(it, peak / it) }
                hubbertChart.series(label, peakSeries.asObservable()).also {
                    it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
                }
            }
        }

        hubbertChart.installStandardHoverAndTooltip()
    }
}
