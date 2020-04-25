package triathematician.covid19.ui

import javafx.event.EventTarget
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.SplitPane
import tornadofx.*
import triathematician.covid19.ui.utils.*

class ForecastPanel : SplitPane() {

    private val config = ForecastPanelConfig { updateForecasts() }

    private lateinit var forecastTotals: LineChart<Number, Number>
    private lateinit var forecastDeltas: LineChart<Number, Number>
    private lateinit var forecastHubbert: LineChart<Number, Number>
    private lateinit var forecastHistory: LineChart<Number, Number>

    init {
        configPanel()
        charts()
        updateForecasts()
    }

    /** Projection configuration panel. */
    private fun EventTarget.configPanel() = form {
        fieldset("Region/Metric") {
            field("Region") { autotextfield(config.regions).bind(config._region) }
            field("Metric") { combobox(config._selectedRegion, METRIC_OPTIONS); checkbox("smooth").bind(config._smooth) }
        }
        fieldset("Forecast (S-Curve)") {
            label("Adjust curve parameters to fit data.")
            field("Model") {
                checkbox("Show").bind(config._showForecast)
                combobox(config._curve, SIGMOID_MODELS)
                button("Autofit") { action { config.autofit() } }
            }
            field("L (maximum)") { slider(0.01..100000.0) { blockIncrement = 0.1 }.bind(config._l) }
            field("k (steepness)") { slider(0.01..2.0) { blockIncrement = 0.001 }.bind(config._k) }
            field("x0 (midpoint)") { slider(-50.0..250.0) { blockIncrement = 0.01 }.bind(config._x0) }
            field("v (exponent)") { slider(0.01..5.0) { blockIncrement = 0.01; enableWhen(config._vActive) }.bind(config._v) }
            field("Equation") { label("").bind(config._manualEquation) }
            field("Peak") { label("").bind(config._manualPeak) }
            field("Fit") { label("").bind(config._manualLogCumStdErr); label("").bind(config._manualDeltaStdErr) }
        }
        fieldset("Curve Fitting") {
            label(config._fitLabel)
            field("First Day for Fit") { intslider(-60..0) { isShowTickLabels = true }.bind(config._autofitDay0) }
            field("# Days for Fit") { intslider(5..60) { isShowTickLabels = true }.bind(config._autofitDays) }
        }
        fieldset("Other Forecasts") {
            label("View other forecasts")
            field("Statistical") {
                checkbox("IHME").bind(config._showIhme)
                checkbox("LANL").bind(config._showLanl)
                checkbox("UT").bind(config._showUt)
            }
            field("Epidemiological") {
                checkbox("MOBS").bind(config._showMobs)
                checkbox("CU-80").bind(config._showCu80)
            }
        }
        fieldset("Forecast History") {
            label("This will let you generate forecasts for data in the past to assess the model.")
            field("Moving Average (days)") {
                editablespinner(1..21).bind(config._movingAverage)
            }
            field("# of Days for Fit") {
                editablespinner(3..99).bind(config._projectionDays)
            }
        }
    }

    /** Charts. */
    private fun EventTarget.charts() = gridpane {
        row {
            forecastTotals = linechart("Totals", "Day (or Day of Forecast)", "Actual/Forecast")
            forecastDeltas = linechart("Change per Day", "Day", "Actual/Forecast")
        }
        row {
            forecastHubbert = linechart("Percent Growth vs Total",
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
            forecastHistory = linechart("Days to Peak", "Day of Forecast", "Forecasted Days to Peak")
        }
    }

    /** Plot forecast curves: min/avg/max totals predicted by day for a single region. */
    private fun updateForecasts() {
        forecastTotals.dataSeries = config.cumulativeDataSeries()
        forecastDeltas.dataSeries = config.dailyDataSeries()
        forecastHubbert.dataSeries = config.hubbertDataSeries()
        forecastHistory.dataSeries = config.peakDataSeries()

        config.domain?.let {
            with(axisLabeler(it.start)) {
                (forecastTotals.xAxis as NumberAxis).tickLabelFormatter = this
                (forecastDeltas.xAxis as NumberAxis).tickLabelFormatter = this
                (forecastHistory.xAxis as NumberAxis).tickLabelFormatter = this
            }
        }

        listOf(forecastTotals, forecastDeltas, forecastHistory, forecastHubbert).forEach { chart ->
            chart.animated = false
            chart.data.forEach {
                if ("predicted" in it.name || "ihme" in it.name) {
                    it.nodeProperty().get().style = "-fx-opacity: 0.5"
                    it.data.forEach { it.node?.isVisible = false }
                }
                if ("curve" in it.name) {
                    it.nodeProperty().get().style = "-fx-opacity: 0.5; -fx-stroke-width: 4"
                    it.data.forEach { it.node?.isVisible = false }
                }
            }
        }
    }
}