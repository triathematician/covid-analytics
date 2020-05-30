package tri.covid19.forecaster.history

import com.sun.javafx.charts.Legend
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import tornadofx.*
import tri.covid19.forecaster.installHoverEffect
import tri.covid19.forecaster.utils.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
class HistoryPanelPlots constructor(val historyPanelModel: HistoryPanelModel, val hubbertPanelModel: HistoryHubbertModel) : View() {

    private lateinit var historicalChart: LineChart<Number, Number>
    private lateinit var hubbertChart: LineChart<Number, Number>
    private lateinit var legend: Legend

    override val root = borderpane {
        center = gridpane {
            row {
                historicalChart = linechart("Historical Data", "Day", "Count", yLog = historyPanelModel.logScale) {
                    gridpaneConstraints { vhGrow = Priority.ALWAYS }
                    animated = false
                    createSymbols = false
                    isLegendVisible = false
                    chartContextMenu()
                }
            }
            row {
                hubbertChart = linechart("Percent Growth vs Total",
                        NumberAxis().apply { label = "Total" },
                        NumberAxis("Percent Growth", 0.0, 0.3, 0.5).apply {
                            isAutoRanging = false
                        }) {
                    gridpaneConstraints { vhGrow = Priority.ALWAYS }
                    animated = false
                    createSymbols = false
                    isLegendVisible = false
                    axisSortingPolicy = LineChart.SortingPolicy.NONE
                    chartContextMenu()
                }
            }
        }
        bottom = hbox(alignment = Pos.CENTER) {
            legend = Legend()
            legend.alignment = Pos.CENTER
            val chartLegend = historicalChart.childrenUnmodifiable.first { it is Legend } as Legend
            Bindings.bindContent(legend.items, chartLegend.items)
            this += legend
        }
    }

    init {
        historyPanelModel.onChange = { updateBothCharts() }
        hubbertPanelModel.onChange = { updateBothCharts() }
        historyPanelModel._logScale.onChange { resetCharts() }
        updateBothCharts()
    }

    /** Resets positioning of chart, must do when axes change. */
    private fun resetCharts() {
        // TODO - this needs to reset and relink the legend
        val parent = historicalChart.parent
        hubbertChart.removeFromParent()
        historicalChart.removeFromParent()
        historicalChart = parent.linechart("Historical Data", "Day", "Count", yLog = historyPanelModel.logScale) {
            animated = false
            createSymbols = false
        }
        parent.add(hubbertChart)
        updateHistoryChart()
    }

    /** Update both charts. */
    private fun updateBothCharts() {
        if (this::historicalChart.isInitialized) {
            updateHistoryChart()
            updateHubbertChart()
        }
    }

    /** Plot counts by date. */
    private fun updateHistoryChart() {
        val (domain, series) = historyPanelModel.historicalDataSeries()

        historicalChart.dataSeries = series
        (historicalChart.xAxis as NumberAxis).tickLabelFormatter = axisLabeler(domain.start)
        historicalChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertPanelModel.showPeakCurve.get() && historyPanelModel.perDay) {
            val peak = hubbertPanelModel.peakValue
            val label = hubbertPanelModel.peakLabel
            val peakSeries = domain.mapIndexed { i, _ -> xy(i, peak) }
            historicalChart.series(label, peakSeries.asObservable()).also {
                it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
            }
        }

        historicalChart.data.forEach {
            it.node.installHoverEffect()
            Tooltip.install(it.node, Tooltip(it.name))
            it.data.forEach {
                it.node?.run { installHoverEffect() }
            }
        }
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

        hubbertChart.data.forEach {
            it.node.installHoverEffect()
            Tooltip.install(it.node, Tooltip(it.name))
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
