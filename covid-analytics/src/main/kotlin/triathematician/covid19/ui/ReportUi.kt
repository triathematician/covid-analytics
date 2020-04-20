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
import java.time.LocalDate

class TimeSeriesReportApp : App(TimeSeriesReportAppView::class)

fun main(args: Array<String>) {
    launch<TimeSeriesReportApp>(args)
}

/** View configuration for the app. */
class TimeSeriesReportAppView : View() {

    private val plotConfig = PlotConfig { updateHistoricalPlot(); updateHubbertPlot() }
    private val hubbertPlotConfig = HubbertPlotConfig { updateHubbertPlot() }
    private val projectionPlotConfig = ProjectionPlotConfig { updateProjectionPlot() }

    private lateinit var historicalChart: LineChart<Number, Number>
    private lateinit var hubbertChart: LineChart<Number, Number>
    private lateinit var projectionChart: LineChart<Number, Number>
    private lateinit var projectionChartChange: LineChart<Number, Number>
    private lateinit var projectionChartDays: LineChart<Number, Number>

    override val root = vbox {
        splitpane {
            vgrow = Priority.ALWAYS
            drawer {
                item("Historical Data", expanded = true) {
                    dataConfigForm()
                }
                item("Projections") {
                    projectionConfigForm()
                }
            }
            drawer {
                item("Historical Data", expanded = true) {
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
                item("Projections") {
                    projectionChart = linechart("Projections",
                            NumberAxis().apply { label = "Day (or Day of Projection)" },
                            NumberAxis().apply {
                                label = "Projection"
                            })
                    projectionChartChange = linechart("Projected Change per Day",
                            NumberAxis().apply { label = "Day" },
                            NumberAxis().apply {
                                label = "Projection"
                            })
                    projectionChartDays = linechart("Projected Days to Peak",
                            NumberAxis().apply { label = "Day of Projection" },
                            NumberAxis().apply {
                                label = "Projection"
                            })
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
    fun EventTarget.dataConfigForm() = form {
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
                slider(-2.0..8.0) {
                    blockIncrement = 0.01
                }.bind(hubbertPlotConfig.logPeakValueProperty)
            }
        }
    }

    /** Projection configuration panel. */
    fun EventTarget.projectionConfigForm() = form {
        fieldset("Logistic Projection") {
            field("Region") {
                textfield().bind(projectionPlotConfig.regionProperty)
            }
            field("Moving Average (days)") {
                editableSpinner(1..21).bind(projectionPlotConfig.movingAverageProperty)
            }
            field("# of Days for Fit") {
                editableSpinner(3..99).bind(projectionPlotConfig.predictionDaysProperty)
            }
        }
    }

    //region CHART UPDATERS

    /** Plot counts by date. */
    private fun updateHistoricalPlot() {
        var metrics = plotConfig.historicalData()
        if (plotConfig.bucket != 1) {
            metrics = metrics.map { it.movingAverage(plotConfig.bucket) }.toSet()
        }

        val domain = metrics.dateRange ?: DateRange(LocalDate.now(), LocalDate.now())

        historicalChart.data.clear()
        metrics.map { series ->
            val values = domain.mapIndexed { i, d -> xy(i, if (plotConfig.perDay) series[d] - series[d.minusDays(1L)] else series[d]) }
            historicalChart.series(series.id, values.asObservable())
        }
        historicalChart.setLineWidth(lineChartWidthForCount(plotConfig.regionLimit))
    }

    /** Plot growth vs. total. */
    private fun updateHubbertPlot() {
        val metrics = plotConfig.historicalData()

        val totals = metrics.map { it.id to it.movingAverage(7) }.toMap()
        val growths = metrics.map { it.id to it.movingAverage(7).growthPercentages() }.toMap()

        val domain = totals.values.dateRange ?: DateRange(LocalDate.now(), LocalDate.now())

        hubbertChart.data.clear()
        totals.map { (id, series) ->
            val values = domain.map { xy(totals[id]!![it], growths[id]!![it]) }
            hubbertChart.series(id, values.asObservable())
        }
        hubbertChart.setLineWidth(lineChartWidthForCount(plotConfig.regionLimit))

        if (hubbertPlotConfig.showPeakCurve.get()) {
            val max = totals.values.map { it.lastValue }.max()
            if (max != null) {
                val peak = hubbertPlotConfig.peakValue.toDouble()
                val label = "Peak at " + if (peak >= 10.0) peak.toInt() else if (peak >= 1.0) peak.format(1) else peak.format(2)
                val min = 0.3 / peak
                val peakSeries = (0..100).map { min + (max - min) * it / 100.0 }.map { xy(it, peak / it) }
                hubbertChart.series(label, peakSeries.asObservable()).also {
                    it.nodeProperty().get().style = "-fx-stroke-width: 8px; -fx-stroke: #88888888"
                }
            }
        }
    }

    /** Plot projected curves: min/avg/max totals predicted by day for a single region. */
    private fun updateProjectionPlot() {
        val regionMetrics = CovidTimeSeriesSources.dailyReports({ it == projectionPlotConfig.region })
                .filter { plotConfig.selectedMetric in it.metric }
                .map { it.metric to it }.toMap()
        val domain = regionMetrics.values.dateRange
        domain?.endInclusive = LocalDate.now().plusDays(60L)

        val ihmeProjections = IhmeProjections.allProjections.filter { it.id == projectionPlotConfig.region }
                .map { it.metric to it }.toMap()
        val ihmeDomain = ihmeProjections.values.dateRange

        projectionChart.data.clear()
        projectionChartChange.data.clear()
        projectionChartDays.data.clear()

        if (domain != null) {
            regionMetrics.filter { ("predicted" in it.key || "(" !in it.key) && "peak" !in it.key }
                    .map { series ->
                        val values = domain.mapIndexed { i, d -> xy(i, series.value[d]) }
                        projectionChart.series(series.key, values.asObservable())
                    }
            if (plotConfig.selectedMetric == DEATHS) {
                ihmeProjections.filter { "change" !in it.key }
                        .map { series ->
                            val values = domain.mapIndexed { i, d -> if (ihmeDomain != null && d in ihmeDomain) xy(i, series.value[d]) else null }
                                    .filterNotNull()
                            projectionChart.series(series.key, values.asObservable())
                        }
            }
            regionMetrics.filter { it.key == plotConfig.selectedMetric }
                    .map { series ->
                        val values = domain.mapIndexed { i, d -> xy(i, series.value[d]-series.value[d.minusDays(1L)]) }
                        projectionChartChange.series(series.key, values.asObservable())
                    }
            regionMetrics.filter { "predicted peak" in it.key }
                    .map { series ->
                        val values = domain.mapIndexed { i, d -> xy(i, series.value[d]) }
                        projectionChartChange.series(series.key, values.asObservable())
                    }
            if (plotConfig.selectedMetric == DEATHS) {
                ihmeProjections.filter { "change" in it.key }
                        .map { series ->
                            val values = domain.mapIndexed { i, d -> if (ihmeDomain != null && d in ihmeDomain) xy(i, series.value[d]) else null }
                                    .filterNotNull()
                            projectionChartChange.series(series.key, values.asObservable())
                        }
            }
            regionMetrics.filter { "days" in it.key }
                    .map { series ->
                        val values = domain.mapIndexed { i, d -> xy(i, series.value[d]) }
                        projectionChartDays.series(series.key, values.asObservable())
                    }
        }
    }

    private fun xy(x: Number, y: Number) = XYChart.Data(x, y)

    private fun LineChart<*, *>.setLineWidth(width: String) {
        data.forEach {
            it.nodeProperty().get().style = "-fx-stroke-width: $width"
        }
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