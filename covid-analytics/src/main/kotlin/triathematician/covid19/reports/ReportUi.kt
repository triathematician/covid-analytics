package triathematician.covid19.reports

import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.ScatterChart
import javafx.scene.chart.XYChart
import javafx.scene.layout.Priority
import tornadofx.*
import triathematician.covid19.countryData
import triathematician.covid19.usCountyData
import triathematician.covid19.usStateData
import triathematician.util.DateRange
import java.lang.IllegalStateException
import java.lang.Math.log10

class TimeSeriesReportApp: App(TimeSeriesReportAppView::class)

fun main(args: Array<String>) {
    launch<TimeSeriesReportApp>(args)
}

/** Config for both plots. */
class PlotConfig(var onChange: () -> Unit = {}) {
    val regionTypes = listOf("Countries and Global Regions", "US States and Territories", "US Counties")
    val metricOptions = listOf("Confirmed", "Deaths", "Recovered", "Active")
    var regionLimit by property(10)

    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply {
        addListener { _ -> onChange() }
    }
    val selectedMetric = SimpleStringProperty(metricOptions[0]).apply {
        addListener { _ -> onChange() }
    }
    val regionLimitProperty = getProperty(PlotConfig::regionLimit).apply {
        addListener { _ -> onChange() }
    }
}

/** Config for logistic projection. */
class LogisticConfig(var onChange: () -> Unit = {}) {
    var movingAverage by property(4)
    var predictionDays by property(10)

    val movingAverageProperty = getProperty(LogisticConfig::movingAverage).apply {
        addListener { _ -> onChange() }
    }
    val predictionDaysProperty = getProperty(LogisticConfig::predictionDays).apply {
        addListener { _ -> onChange() }
    }
}

class Styles : Stylesheet() {
    companion object {
        val thinLineChart by cssclass()
        val thickLineChart by cssclass()
    }
    init {
        thinLineChart {
            chartSeriesLine {

                strokeWidth = 0.5.px
            }
        }
        thickLineChart {
            chartSeriesLine {
                strokeWidth = 2.px
            }
        }
    }
}

class TimeSeriesReportAppView: View() {

    val plotConfig = PlotConfig { updatePlots() }
    val logisticConfig = LogisticConfig { updateProjectionPlot() }

    lateinit var historicalChart: LineChart<Number, Number>
    lateinit var hubbertChart: LineChart<Number, Number>

    fun updatePlots() {
        updateHistoricalPlot()
        updateProjectionPlot()
    }

    private fun data() = when (plotConfig.selectedRegionType.get()) {
        "Countries and Global Regions" -> countryData(includeGlobal = false)
        "US States and Territories" -> usStateData(includeUS = false)
        "US Counties" -> usCountyData()
        else -> throw IllegalStateException()
    }

    private fun updateHistoricalPlot() {
        val metrics = data().filter { it.metric == plotConfig.selectedMetric.get() }
                .sortedByDescending { it.lastValue }
                .take(plotConfig.regionLimit)

        val start = metrics.map { it.firstPositiveDate }.min()!!
        val end = metrics.map { it.end }.max()!!
        historicalChart.data.clear()
        metrics.map { series ->
            val values = DateRange(start, end)
                    .mapIndexed { i, d -> XYChart.Data<Number,Number>(i, series[d]) }
            historicalChart.series(series.id, values.asObservable())
        }
        val width = when {
            plotConfig.regionLimit <= 5 -> "2px"
            plotConfig.regionLimit <= 10 -> "1.5px"
            plotConfig.regionLimit <= 20 -> "1px"
            else -> "0.8px"
        }
        historicalChart.data.forEach {
            it.nodeProperty().get().style = "-fx-stroke-width: $width"
        }
    }

    private fun updateProjectionPlot() {
        val selection = plotConfig.selectedMetric.get()
        val totals = data().filter { it.metric == selection }
                .sortedByDescending { it.lastValue }
                .take(plotConfig.regionLimit)
                .map { it.id to it }.toMap()
        val growths = data().filter { it.id in totals.keys && it.metric == "$selection (growth)" }
                .map { it.id to it }.toMap()

        val start = totals.map { it.value.firstPositiveDate }.min()!!
        val end = totals.map { it.value.end }.max()!!
        hubbertChart.data.clear()
        totals.map { (id, series) ->
            val values = DateRange(start, end).map { XYChart.Data<Number,Number>(totals[id]!![it]!!, growths[id]!![it]!!) }
            hubbertChart.series(id, values.asObservable())
        }
        val width = when {
            plotConfig.regionLimit <= 5 -> "2px"
            plotConfig.regionLimit <= 10 -> "1.5px"
            plotConfig.regionLimit <= 20 -> "1px"
            else -> "0.8px"
        }
        hubbertChart.data.forEach {
            it.nodeProperty().get().style = "-fx-stroke-width: $width"
        }
    }

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
                    // TODO - add configuration panel, e.g. log
                    historicalChart = linechart("Historical Data",
                            NumberAxis().apply { label = "Day" },
                            NumberAxis().apply {
                                label = "Count"
                            }) {
                        animated = false
                        createSymbols = false
                        addClass(Styles.thinLineChart)
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
                        addClass(Styles.thinLineChart)
                    }
                }
                item("Projections") {
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

    fun EventTarget.dataConfigForm() = form {
        fieldset("Regions") {
            field("Region Category") {
                combobox(plotConfig.selectedRegionType, plotConfig.regionTypes)
            }
            field("Metric") {
                combobox(plotConfig.selectedMetric, plotConfig.metricOptions)
            }
            field("Max # of Regions") {
                editableSpinner(1..200).bind(plotConfig.regionLimitProperty)
            }
        }
    }

    fun EventTarget.projectionConfigForm() = form {
        fieldset("Logistic Projection") {
            field("Moving Average (days)") {
                editableSpinner(1..21).bind(logisticConfig.movingAverageProperty)
            }
            field("# of Days for Fit") {
                editableSpinner(3..99).bind(logisticConfig.predictionDaysProperty)
            }
        }
    }
}

private fun EventTarget.editableSpinner(range: IntRange) = spinner(range.first, range.last, range.first, 1) {
    isEditable = true
}