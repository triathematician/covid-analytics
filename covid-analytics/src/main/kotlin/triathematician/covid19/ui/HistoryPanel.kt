package triathematician.covid19.ui

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.SplitPane
import tornadofx.*

/** UI for exploring historical COVID time series data. */
class HistoryPanel: SplitPane() {

    private val plotConfig = HistoryPanelConfig { updateHistoryChart(); updateHubbertChart() }
    private val hubbertConfig = HistoricalHubbertPlots { updateHistoryChart(); updateHubbertChart() }

    private lateinit var historicalChart: LineChart<Number, Number>
    private lateinit var hubbertChart: LineChart<Number, Number>

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
                combobox(plotConfig.selectedRegionType, plotConfig.regionTypes)
            }
            field("Max # of Regions") {
                editablespinner(0..200).bind(plotConfig.regionLimitProperty)
            }
            field("Include Regions") {
                checkbox().bind(plotConfig.includeRegionActive)
                textfield().bind(plotConfig.includeRegion)
            }
            field("Exclude Regions") {
                checkbox().bind(plotConfig.excludeRegionActive)
                textfield().bind(plotConfig.excludeRegion)
            }
            field("Min Population") {
                editablespinner(0..1000000).bind(plotConfig.minPopulationProperty)
            }
        }

        fieldset("Metric") {
            field("Metric") {
                combobox(plotConfig.selectedMetricProperty, METRIC_OPTIONS)
                checkbox("per capita").bind(plotConfig.perCapitaProperty)
                checkbox("per day").bind(plotConfig.perDayProperty)
            }
            field("Smooth (days)") {
                editablespinner(1..14).bind(plotConfig.bucketProperty)
            }
        }

        fieldset("Growth Plot") {
            field("Peak Curve") {
                checkbox("show").bind(hubbertConfig.showPeakCurve)
                slider(-2.0..8.0) {
                    blockIncrement = 0.01
                    enableWhen(hubbertConfig.showPeakCurve)
                }.bind(hubbertConfig.logPeakValueProperty)
            }
        }
    }

    /** Charts. */
    private fun EventTarget.charts() = vbox {
        historicalChart = linechart("Historical Data", "Day", "Count") {
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

    /** Plot counts by date. */
    private fun updateHistoryChart() {
        val (domain, series) = plotConfig.historicalDataSeries()

        historicalChart.dataSeries = series
        (historicalChart.xAxis as NumberAxis).tickLabelFormatter = axisLabeler(domain.start)
        historicalChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertConfig.showPeakCurve.get() && plotConfig.perDay) {
            val peak = hubbertConfig.peakValue
            val label = hubbertConfig.peakLabel
            val peakSeries = domain.mapIndexed { i, d -> xy(i, peak) }
            historicalChart.series(label, peakSeries.asObservable()).also {
                it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
            }
        }
    }

    /** Plot growth vs. total. */
    private fun updateHubbertChart() {
        val series = plotConfig.hubbertDataSeries()
        hubbertChart.dataSeries = series
        hubbertChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertConfig.showPeakCurve.get()) {
            val max = series.mapNotNull { it.points.map { it.first.toDouble() }.max() }.max()
            if (max != null) {
                val peak = hubbertConfig.peakValue
                val label = hubbertConfig.peakLabel
                val min = peak / 0.3
                val peakSeries = (0..100).map { min + (max - min) * it / 100.0 }.map { xy(it, peak / it) }
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
                series(it.id, it.points.map { xy(it.first, it.second) }.asObservable())
            }
        }

}