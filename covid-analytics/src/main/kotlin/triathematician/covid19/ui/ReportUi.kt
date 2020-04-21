package triathematician.covid19.ui

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.Priority
import tornadofx.*
import triathematician.covid19.CovidTimeSeriesSources
import triathematician.covid19.DEATHS
import triathematician.covid19.sources.IhmeProjections
import triathematician.timeseries.*
import triathematician.util.DateRange
import triathematician.util.format

class TimeSeriesReportApp : App(TimeSeriesReportAppView::class)

fun main(args: Array<String>) {
    launch<TimeSeriesReportApp>(args)
}

/** View configuration for the app. */
class TimeSeriesReportAppView : View() {

    private val plotConfig = PlotConfig { updateHistoricalPlot(); updateHistoricalHubbert() }
    private val hubbertPlotConfig = HubbertPlotConfig { updateHistoricalPlot(); updateHistoricalHubbert() }
    private val projectionConfig = ProjectionPlotConfig { updateProjections() }

    private lateinit var historicalChart: LineChart<Number, Number>
    private lateinit var hubbertChart: LineChart<Number, Number>

    private lateinit var projectionChart: LineChart<Number, Number>
    private lateinit var projectionHubbert: LineChart<Number, Number>
    private lateinit var projectionChartChange: LineChart<Number, Number>
    private lateinit var projectionChartDays: LineChart<Number, Number>

    //region UI LAYOUT

    override val root = vbox {
        drawer {
            vgrow = Priority.ALWAYS
            item("Historical Data", expanded = true) {
                splitpane {
                    dataForm()
                    vbox {
                        historicalChart = linechart("Historical Data",
                                NumberAxis().apply { label = "Day" },
                                NumberAxis().apply {
                                    label = "Count"
                                }) {
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
                }
            }
            item("Projections") {
                splitpane {
                    projectionForm()
                    gridpane {
                        row {
                            projectionChart = linechart("Projections", "Day (or Day of Projection)", "Projection")
                            projectionChartChange = linechart("Projected Change per Day", "Day", "Projection")
                        }
                        row {
                            projectionHubbert = linechart("Percent Growth vs Total",
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
                            projectionChartDays = linechart("Projected Days to Peak", "Day of Projection", "Projection")
                        }
                    }
                }
            }
        }
        hbox {
            vgrow = Priority.NEVER
            label("")
            pane {
                hgrow = Priority.ALWAYS
            }
            label("")
        }
    }

    /** Main configuration panel. */
    fun EventTarget.dataForm() = form {
        fieldset("Regions") {
            field("Region Category") {
                combobox(plotConfig.selectedRegionType, plotConfig.regionTypes)
            }
            field("Max # of Regions") {
                editableSpinner(0..200).bind(plotConfig.regionLimitProperty)
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
                editableSpinner(0..1000000).bind(plotConfig.minPopulationProperty)
            }
        }

        fieldset("Metric") {
            field("Metric") {
                combobox(plotConfig.selectedMetricProperty, METRIC_OPTIONS)
                checkbox("per capita").bind(plotConfig.perCapitaProperty)
                checkbox("per day").bind(plotConfig.perDayProperty)
            }
            field("Smooth (days)") {
                editableSpinner(1..14).bind(plotConfig.bucketProperty)
            }
        }

        fieldset("Growth Plot") {
            field("Peak Curve") {
                checkbox("show").bind(hubbertPlotConfig.showPeakCurve)
                slider(-2.0..8.0) { blockIncrement = 0.01 }.bind(hubbertPlotConfig.logPeakValueProperty)
            }
        }
    }

    /** Projection configuration panel. */
    fun EventTarget.projectionForm() = form {
        fieldset("Region/Metric") {
            field("Region") {
                textfield().bind(projectionConfig._region)
            }
            field("Metric") {
                combobox(projectionConfig._selectedRegion, METRIC_OPTIONS)
            }
            field("IHME Projections") {
                checkbox("Show").bind(projectionConfig._showIhme)
            }
        }
        fieldset("Projection (Manual)") {
            label("This will let you manually adjust model parameters to fit visually.")
            field("Model") {
                checkbox("Show").bind(projectionConfig._showManual)
                combobox(projectionConfig._manualModel, SIGMOID_MODELS)
                checkbox("Show R^2").bind(projectionConfig._showR2)
            }
            field("L (maximum)") { slider(0.01..100000.0) { blockIncrement = 0.1 }.bind(projectionConfig._l) }
            field("k (steepness)") { slider(0.01..2.0) { blockIncrement = 0.001 }.bind(projectionConfig._k) }
            field("x0 (midpoint)") { slider(-50.0..250.0) { blockIncrement = 0.01 }.bind(projectionConfig._x0) }
            field("v (exponent)") { slider(0.01..5.0) { blockIncrement = 0.01 }.bind(projectionConfig._v) }
        }
        fieldset("Projection (Fit)") {
            label("This will let you automatically adjust model parameters for best statistical fit.")
            field("Model") {
                checkbox("Show").bind(projectionConfig._showFit)
                combobox(projectionConfig._fitModel, SIGMOID_MODELS)
            }
        }
        fieldset("Projection History") {
            label("This will let you generate projections for data in the past to assess the model.")
            field("Moving Average (days)") {
                editableSpinner(1..21).bind(projectionConfig._movingAverage)
            }
            field("# of Days for Fit") {
                editableSpinner(3..99).bind(projectionConfig._projectionDays)
            }
        }
    }

    //endregion

    //region CHART UPDATERS

    /** Plot counts by date. */
    private fun updateHistoricalPlot() {
        val (domain, series) = plotConfig.historicalDataSeries()

        historicalChart.dataSeries = series
        historicalChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertPlotConfig.showPeakCurve.get() && plotConfig.perDay) {
            val peak = hubbertPlotConfig.peakValue
            val peakSeries = domain.mapIndexed { i, d -> xy(i, peak) }
            val label = "Peak at " + if (peak >= 10.0) peak.toInt() else if (peak >= 1.0) peak.format(1) else peak.format(2)
            historicalChart.series(label, peakSeries.asObservable()).also {
                it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
            }
        }
    }

    /** Plot growth vs. total. */
    private fun updateHistoricalHubbert() {
        val series = plotConfig.hubbertDataSeries()
        hubbertChart.dataSeries = series
        hubbertChart.lineWidth = lineChartWidthForCount(series.size)

        if (hubbertPlotConfig.showPeakCurve.get()) {
            val max = series.mapNotNull { it.points.map { it.first.toDouble() }.max() }.max()
            if (max != null) {
                val peak = hubbertPlotConfig.peakValue
                val label = "Peak at " + if (peak >= 10.0) peak.toInt() else if (peak >= 1.0) peak.format(1) else peak.format(2)
                val min = peak / 0.3
                val peakSeries = (0..100).map { min + (max - min) * it / 100.0 }.map { xy(it, peak / it) }
                hubbertChart.series(label, peakSeries.asObservable()).also {
                    it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
                }
            }
        }
    }

    /** Plot projected curves: min/avg/max totals predicted by day for a single region. */
    private fun updateProjections() {
        val metric = projectionConfig.selectedMetric
        val regionMetrics = CovidTimeSeriesSources.dailyReports({ it == projectionConfig.region }).filter { metric in it.metric }
        val mainSeries = regionMetrics.firstOrNull { it.metric == metric }
        val ihmeProjections = IhmeProjections.allProjections.filter { it.id == projectionConfig.region }

        val domain = regionMetrics.dateRange
        val ihmeDomain = ihmeProjections.dateRange
        val totalDomain = if (domain != null) DateRange(domain.start, domain.endInclusive.plusDays(30)) else null

        val manualProjection = if (domain != null && totalDomain != null) projectionConfig.manualProjection(totalDomain, domain.start) else null

        projectionChart.data.clear()
        projectionChartChange.data.clear()
        projectionChartDays.data.clear()
        projectionHubbert.data.clear()

        if (domain != null && totalDomain != null && mainSeries != null && manualProjection != null) {
            // cumulative chart
            projectionChart.dataSeries = listOf(DataSeries(domain, mainSeries), DataSeries(totalDomain, manualProjection)) +
                    regionMetrics.filter { "predicted" in it.metric && "peak" !in it.metric }.map { DataSeries(domain, it) } +
                    ihmeProjections.filter { metric == DEATHS && "change" !in it.metric }.map { DataSeries(totalDomain, ihmeDomain, it) }

            // change chart
            projectionChartChange.dataSeries = listOf(DataSeries(domain, mainSeries.deltas()), DataSeries(totalDomain, manualProjection.deltas())) +
                    regionMetrics.filter { "predicted peak" in it.id }.map { DataSeries(domain, it) } +
                    ihmeProjections.filter { "change" in it.metric }.map { DataSeries(totalDomain, ihmeDomain, it) }

            // hubbert chart
            projectionHubbert.dataSeries = listOf(mainSeries.hubbertSeries(7)).map { DataSeries(domain.plus(1, 0), it.first, it.second) } +
                    listOf(manualProjection.hubbertSeries(1)).map { DataSeries(totalDomain.plus(1, 0), it.first, it.second) } +
                    ihmeProjections.filter { metric == DEATHS && "change" !in it.metric }.map { it.hubbertSeries(1) }
                            .map { DataSeries(totalDomain, ihmeDomain?.plus(1, 0), it.first, it.second) }

            // peak days chart
            projectionChartDays.dataSeries = regionMetrics.filter { "days" in it.id }.map { DataSeries(domain, it) }
        }

        listOf(projectionChart, projectionChartChange, projectionChartDays, projectionHubbert).forEach { chart ->
            chart.animated = false
            chart.data.forEach {
                if ("predicted" in it.name || "ihme" in it.name) {
                    it.nodeProperty().get().style = "-fx-opacity: 0.5"
                    it.data.forEach { it.node?.isVisible = false }
                }
            }
        }
    }

    //endregion

    //region UTILS

    private fun EventTarget.linechart(title: String, xTitle: String, yTitle: String): LineChart<Number, Number> {
        return linechart(title, NumberAxis().apply { label = xTitle }, NumberAxis().apply { label = yTitle })
    }

    private fun xy(x: Number, y: Number) = XYChart.Data(x, y)

    private var LineChart<Number, Number>.dataSeries: List<DataSeries>
        get() = listOf()
        set(value) {
            data.clear()
            value.forEach {
                series(it.id, it.points.map { xy(it.first, it.second) }.asObservable())
            }
        }

    private var LineChart<*, *>.lineWidth: String
        get() = "1px"
        set(value) = data.forEach {
            it.nodeProperty().get().style = "-fx-stroke-width: $value"
        }

    private fun lineChartWidthForCount(count: Int) = when {
        count <= 5 -> "2px"
        count <= 10 -> "1.5px"
        count <= 20 -> "1px"
        else -> "0.8px"
    }

    //endregion

}

private fun EventTarget.editableSpinner(range: IntRange) = spinner(range.first, range.last, range.first, 1) {
    isEditable = true
}