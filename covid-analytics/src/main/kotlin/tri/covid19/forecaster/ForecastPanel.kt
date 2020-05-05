package tri.covid19.forecaster

import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.SplitPane
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import tornadofx.*
import tri.covid19.data.IHME
import tri.covid19.data.LANL
import tri.covid19.forecaster.CovidForecasterStyles.Companion.chartHover
import tri.covid19.forecaster.utils.*
import tri.math.SIGMOID_MODELS
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ForecastPanel : SplitPane() {

    val model = ForecastPanelModel { updateForecasts() }

    private lateinit var forecastTotals: LineChart<Number, Number>
    private lateinit var forecastDeltas: LineChart<Number, Number>
    private lateinit var forecastHubbert: LineChart<Number, Number>
    private lateinit var forecastResiduals: LineChart<Number, Number>

    init {
        configPanel()
        charts()
        updateForecasts()
    }

    /** Projection configuration panel. */
    private fun EventTarget.configPanel() = form {
        fieldset("Region/Metric") {
            field("Region") {
                autotextfield(model.regions) {
                    contextmenu {
                        item("Next State") { action { model.goToNextUsState() } }
                        item("Previous State") { action { model.goToPreviousUsState() } }
                    }
                }.bind(model._region)
            }
            field("Metric") { combobox(model._selectedMetric, METRIC_OPTIONS); checkbox("smooth").bind(model._smooth) }
        }
        fieldset("Forecast (S-Curve)") {
            label("Adjust curve parameters to fit data.")
            field("Model") {
                checkbox("Show").bind(model._showForecast)
                combobox(model._curve, SIGMOID_MODELS)
                button("Autofit") { action { model.autofit() } }
                button("Save") { action { model.save() } }
            }
            field("L (maximum)") { slider(0.01..100000.0) { blockIncrement = 0.1 }.bind(model._l) }
            field("k (steepness)") { slider(0.01..2.0) { blockIncrement = 0.001 }.bind(model._k) }
            field("x0 (midpoint)") { slider(-50.0..250.0) { blockIncrement = 0.01 }.bind(model._x0) }
            field("v (exponent)") { slider(0.01..5.0) { blockIncrement = 0.01; enableWhen(model._vActive) }.bind(model._v) }
            field("Equation") { label("").bind(model._manualEquation) }
            field("Peak") { label("").bind(model._manualPeak) }
            field("Fit") { label("").bind(model._manualLogCumStdErr); label("").bind(model._manualDeltaStdErr) }
        }
        fieldset("Curve Fitting") {
            label(model._fitLabel)
            field("First Day for Fit") { intslider(-60..0) { isShowTickLabels = true; blockIncrement = 7.0 }.bind(model._autofitLastDay) }
            field("# Days for Fit") { intslider(5..60) { isShowTickLabels = true; blockIncrement = 7.0 }.bind(model._autofitDays) }
        }
        fieldset("Other Forecasts") {
            label("View other forecasts")
            field("Statistical") {
                checkbox("IHME").bind(model._showIhme)
                checkbox("LANL").bind(model._showLanl)
            }
            field("Epidemiological") {
                checkbox("YYG").bind(model._showYyg)
            }
        }
        fieldset("Forecast History") {
            label("This will let you generate forecasts for data in the past to assess the model.")
            field("Moving Average (days)") {
                editablespinner(1..21).bind(model._movingAverage)
            }
            field("# of Days for Fit") {
                editablespinner(3..99).bind(model._projectionDays)
            }
        }
    }

    /** Charts. */
    private fun EventTarget.charts() = borderpane {
        top = hbox(10) {
            padding = Insets(10.0, 10.0, 10.0, 10.0)
            label(model._region) { style = "-fx-font-size: 20; -fx-font-weight: bold" }
            label(model._selectedMetric) { style = "-fx-font-size: 20; -fx-font-weight: bold" }
        }
        center = gridpane {
            row {
                forecastTotals = linechartRangedOnFirstSeries("Totals", "Day (or Day of Forecast)", "Actual/Forecast") {
                    gridpaneConstraints { vhGrow = Priority.ALWAYS }
                }
                forecastDeltas = linechartRangedOnFirstSeries("Change per Day", "Day", "Actual/Forecast")
            }
            row {
                forecastHubbert = linechartRangedOnFirstSeries("Percent Growth vs Total",
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
                forecastResiduals = linechart("Residuals (Daily)", "Day", "# more than forecasted") {
                    gridpaneConstraints { vhGrow = Priority.ALWAYS }
                }
            }
        }
    }

    /** Plot forecast curves: min/avg/max totals predicted by day for a single region. */
    private fun updateForecasts() {
        val data0 = model.cumulativeDataSeries()
        val max0 = data0.getOrNull(0)?.maxY()
//        val maxOther = data0.drop(1).map { it.maxY() ?: 0.0 }.max()
//        (forecastTotals.yAxis as NumberAxis).limitMaxTo(maxOther, max0, 3.0)

        forecastTotals.dataSeries = data0
        forecastDeltas.dataSeries = model.dailyDataSeries()
        forecastHubbert.dataSeries = model.hubbertDataSeries()
        forecastResiduals.dataSeries = model.residualDataSeries()
//
//        val max2 = forecastDeltas.data.getOrNull(0)?.data?.map { it.yValue.toDouble() }?.max()
//        max2?.let { (forecastTotals.yAxis as NumberAxis).limitMaxTo(3*it) }
//
//        val max1 = forecastHubbert.data.getOrNull(0)?.data?.map { it.xValue.toDouble() }?.max()
//        max1?.let { (forecastTotals.xAxis as NumberAxis).limitMaxTo(3*it) }

        model.domain?.let {
            with(axisLabeler(it.start)) {
                (forecastTotals.xAxis as NumberAxis).tickLabelFormatter = this
                (forecastDeltas.xAxis as NumberAxis).tickLabelFormatter = this
                (forecastResiduals.xAxis as NumberAxis).tickLabelFormatter = this
            }
        }

        listOf(forecastTotals, forecastDeltas, forecastResiduals, forecastHubbert).forEach { chart ->
            chart.animated = false
            chart.data.forEach {
                if ("predicted" in it.name) {
                    it.node.style = "-fx-opacity: 0.5; -fx-stroke-width: 2; -fx-stroke-dash-array: 2,2"
                    it.data.forEach { it.node?.isVisible = false }
                }
                if (IHME in it.name || LANL in it.name) {
                    it.node.style = "-fx-stroke: ${modelColor(it.name)}; -fx-stroke-width: ${modelStrokeWidth(it.name)}; -fx-stroke-dash-array: 3,3"
                    it.data.forEach { it.node?.isVisible = false }
                }
                if ("curve" in it.name) {
                    it.node.style = "-fx-opacity: 0.5; -fx-stroke-width: 4"
                    it.node.onMouseEntered = EventHandler { _ -> it.node.addClass(chartHover) }
                    it.node.onMouseExited = EventHandler { _ -> it.node.removeClass(chartHover) }
                    it.data.forEach { it.node?.isVisible = false }
                }
            }

            chart.data.forEach {
                it.node.onMouseEntered = EventHandler { _ -> it.node.addClass(chartHover) }
                it.node.onMouseExited = EventHandler { _ -> it.node.removeClass(chartHover) }
                it.data.forEach {
                    it.node?.run {
                        Tooltip.install(this, Tooltip("${it.xValue} -> ${it.yValue}"))
                        onMouseEntered = EventHandler { _ -> it.node.addClass(chartHover) }
                        onMouseExited = EventHandler { _ -> it.node.removeClass(chartHover) }
                    }
                }
            }
        }
    }

    private fun NumberAxis.limitMaxTo(maxOther: Double?, max: Double?, multiplier: Double) {
        when {
            max == null || maxOther == null -> isAutoRanging = true
            maxOther >= max*multiplier -> {
                isAutoRanging = false
                lowerBound = 0.0
                upperBound = (max*multiplier).logRound()
            }
            else -> isAutoRanging = true
        }
    }

    private fun Double.logRound(): Double {
        val base = 10.0.pow(floor(log10(this)))
        return when {
            base >= this -> base
            2*base >= this -> 2*base
            else -> 5*base
        }
    }

    private fun modelColor(name: String): String {
        val color = when {
            IHME in name -> "008000"
            LANL in name -> "4682b4"
            else -> "808080"
        }
        val opacity = when {
            "4-26" in name || "4-28" in name -> "ff"
            "4-19" in name || "4-20" in name -> "80"
            else -> "40"
        }
        return "#$color$opacity"
    }

    private fun modelStrokeWidth(name: String) = when {
        "lower" in name || "upper" in name -> "1"
        else -> "2"
    }
}