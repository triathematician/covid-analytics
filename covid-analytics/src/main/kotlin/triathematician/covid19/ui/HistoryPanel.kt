package triathematician.covid19.ui

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.SplitPane
import tornadofx.*
import triathematician.covid19.ui.utils.*

/** UI for exploring historical COVID time series data. */
class HistoryPanel: SplitPane() {

    private val historyPanelModel = HistoryPanelModel { updateBothCharts() }
    private val hubbertChartModel = HistoricalHubbertPlots { updateBothCharts() }

    private lateinit var historicalChart: LineChart<Number, Number>
    private lateinit var hubbertChart: LineChart<Number, Number>

    private val _logScale = historyPanelModel.getProperty(HistoryPanelModel::logScale).apply { addListener { _ -> resetCharts() } }

    init {
        configPanel()
        charts()
        updateHistoryChart()
        updateHubbertChart()
    }

    /** Main configuration panel. */
    private fun EventTarget.configPanel() = form {
        fieldset("Regions") {
            field("Region Category") {
                combobox(historyPanelModel.selectedRegionType, historyPanelModel.regionTypes)
            }
            field("Max # of Regions") {
                editablespinner(0..200).bind(historyPanelModel._regionLimit)
            }
            field("Include Regions") {
                checkbox().bind(historyPanelModel.includeRegionActive)
                textfield().bind(historyPanelModel.includeRegion)
            }
            field("Exclude Regions") {
                checkbox().bind(historyPanelModel.excludeRegionActive)
                textfield().bind(historyPanelModel.excludeRegion)
            }
            field("Min Population") {
                editablespinner(0..1000000).bind(historyPanelModel._minPopulation)
            }
        }

        fieldset("Metric") {
            field("Metric") {
                combobox(historyPanelModel._selectedMetric, METRIC_OPTIONS)
                checkbox("per capita").bind(historyPanelModel._perCapita)
                checkbox("per day").bind(historyPanelModel._perDay)
            }
            field("Smooth (days)") {
                editablespinner(1..14).bind(historyPanelModel._bucket)
                checkbox("log scale").bind(_logScale)
            }
        }

        fieldset("Growth Plot") {
            field("Peak Curve") {
                checkbox("show").bind(hubbertChartModel.showPeakCurve)
                slider(-2.0..8.0) {
                    blockIncrement = 0.01
                    enableWhen(hubbertChartModel.showPeakCurve)
                }.bind(hubbertChartModel.logPeakValueProperty)
            }
        }
    }

    /** Charts. */
    private fun EventTarget.charts() = vbox {
        historicalChart = linechart("Historical Data", "Day","Count", yLog = historyPanelModel.logScale) {
            animated = false
            createSymbols = false
        }
        hubbertChart = linechart("Percent Growth vs Total",
                NumberAxis().apply { label = "Total" },
                NumberAxis().apply {
                    label = "Percent Growth"
                    isAutoRanging = false
                    lowerBound = 0.0
                    tickUnit = 0.05
                    upperBound = 0.3
                }) {
            animated = false
            createSymbols = false
            axisSortingPolicy = LineChart.SortingPolicy.NONE
        }
    }

    /** Resets positioning of chart, must do when axes change. */
    private fun resetCharts() {
        val parent = historicalChart.parent
        hubbertChart.removeFromParent()
        historicalChart.removeFromParent()
        historicalChart = parent.linechart("Historical Data", "Day","Count", yLog = historyPanelModel.logScale) {
            animated = false
            createSymbols = false
        }
        parent.add(hubbertChart)
        updateHistoryChart()
    }

    /** Update both charts. */
    private fun updateBothCharts() {
        updateHistoryChart()
        updateHubbertChart()
    }

    /** Plot counts by date. */
    private fun updateHistoryChart() {
        val (domain, series) = historyPanelModel.historicalDataSeries()

        historicalChart.dataSeries = series
        (historicalChart.xAxis as NumberAxis).tickLabelFormatter = axisLabeler(domain.start)
        historicalChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertChartModel.showPeakCurve.get() && historyPanelModel.perDay) {
            val peak = hubbertChartModel.peakValue
            val label = hubbertChartModel.peakLabel
            val peakSeries = domain.mapIndexed { i, _ -> xy(i, peak) }
            historicalChart.series(label, peakSeries.asObservable()).also {
                it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
            }
        }
    }

    /** Plot growth vs. total. */
    private fun updateHubbertChart() {
        val series = historyPanelModel.hubbertDataSeries()
        hubbertChart.dataSeries = series
        hubbertChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertChartModel.showPeakCurve.get()) {
            val max = series.mapNotNull { it.points.map { it.first.toDouble() }.max() }.max()
            if (max != null) {
                val peak = hubbertChartModel.peakValue
                val label = hubbertChartModel.peakLabel
                val min = peak / 0.3
                val peakSeries = (0..100).map { min + (max - min) * it / 100.0 }.map { triathematician.covid19.ui.utils.xy(it, peak / it) }
                hubbertChart.series(label, peakSeries.asObservable()).also {
                    it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
                }
            }
        }
    }

    /** Set chart series as list of [ChartDataSeries]. */
    private var LineChart<Number, Number>.dataSeries: List<ChartDataSeries>
        get() = listOf()
        set(value) {
            data.clear()
            value.forEach {
                series(it.id, it.points.map { triathematician.covid19.ui.utils.xy(it.first, it.second) }.asObservable())
            }
        }

}