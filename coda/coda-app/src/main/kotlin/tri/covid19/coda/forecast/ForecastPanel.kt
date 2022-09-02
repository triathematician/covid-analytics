@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.covid19.coda.forecast

import com.sun.javafx.charts.Legend
import javafx.beans.binding.Bindings
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.SplitPane
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import javafx.util.StringConverter
import org.controlsfx.control.CheckComboBox
import org.controlsfx.control.RangeSlider
import tornadofx.*
import tri.covid19.coda.data.CovidForecasts
import tri.covid19.data.IHME
import tri.covid19.data.M_D_YYYY
import tri.covid19.data.YYG
import tri.covid19.coda.history.METRIC_OPTIONS
import tri.covid19.coda.installHoverEffect
import tri.covid19.coda.utils.*
import tri.util.math.Sigmoid
import tri.util.measureTime
import tri.util.minus
import tri.util.monthDay
import tri.util.toLocalDate
import tri.util.userFormat
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.milliseconds

class ForecastPanel : SplitPane() {

    val model = ForecastPanelModel { updateForecasts() }

    private lateinit var forecastTotals: LineChart<Number, Number>
    private lateinit var forecastDeltas: LineChart<Number, Number>
    private lateinit var forecastHubbert: LineChart<Number, Number>
    private lateinit var forecastChangeDoubling: LineChart<Number, Number>
    private lateinit var forecastResiduals: LineChart<Number, Number>
    private lateinit var legend: Legend

    //region UI INITALIZATION

    init {
        scrollpane {
            form {
                regionMetricFieldSet()
                forecastFieldSet()
                curveFittingFieldSet()
                modelEvaluationFieldSet()
                otherForecastFieldSet()
            }
        }
        charts()
        updateForecasts()
        this += TimeSeriesInfoPanel(model.mainSeries)
    }

    /** Charts. */
    private fun EventTarget.charts() {
        borderpane {
            top = hbox(10) {
                padding = Insets(10.0, 10.0, 10.0, 10.0)
                label(model._region) { style = "-fx-font-size: 20; -fx-font-weight: bold" }
                label(model._selectedMetric) { style = "-fx-font-size: 20; -fx-font-weight: bold" }
            }
            center = gridpane {
                row {
                    forecastTotals = forecastTotalsChart()
                    forecastDeltas = forecastDeltasChart()
                    forecastResiduals = forecastResidualsChart()
                }
                row {
                    forecastHubbert = forecastHubberChart()
                    forecastChangeDoubling = forecastChangeDoublingChart()
                }
            }
            bottom = hbox(alignment = Pos.CENTER) {
                legend = Legend()
                legend.alignment = Pos.CENTER
                val chartLegend = forecastTotals.childrenUnmodifiable.first { it is Legend } as Legend
                Bindings.bindContent(legend.items, chartLegend.items)
                this += legend
            }
        }
    }

    //endregion

    //region CONFIGURATION FORMS

    private fun EventTarget.regionMetricFieldSet() {
        fieldset("Region/Metric") {
            field("Region") {
                hbox {
                    alignment = Pos.BASELINE_CENTER
                    button("◂") {
                        style = "-fx-background-radius: 3 0 0 3; -fx-padding: 4"
                        action { model.goToPreviousUsState() }
                    }
                    autocompletetextfield(model.areas) {
                        hgrow = Priority.ALWAYS
                        style = "-fx-background-radius: 0"
                        contextmenu {
                            item("Next State") { action { model.goToNextUsState() } }
                            item("Previous State") { action { model.goToPreviousUsState() } }
                        }
                    }.bind(model._region)
                    button("▸") {
                        style = "-fx-background-radius: 0 3 3 0; -fx-padding: 4"
                        action { model.goToNextUsState() }
                    }
                }
            }
            field("Metric") {
                combobox(model._selectedMetric, METRIC_OPTIONS)
                checkbox("per capita").bind(model._perCapita)
                checkbox("smooth").bind(model._smooth)
            }
            field("Logistic Prediction") {
                checkbox("show").bind(model._showLogisticPrediction)
            }
        }
    }

    private fun EventTarget.forecastFieldSet() {
        fieldset("Forecast (S-Curve)") {
            label("Adjust curve parameters to manually fit data.")
            field("Model") {
                checkbox("Show").bind(model._showForecast)
                combobox(model._curve, Sigmoid.values().toList())
                button("Save") { action { model.save() } }
                button("Autofit") { action { model.autofit() } }
            }
            field("L (maximum)") {
                slider(0.01..100000.0) { blockIncrement = 0.1 }.bind(model._l)
                label(model._l, converter = UserStringConverter)
            }
            field("k (steepness)") {
                slider(0.01..2.0) { blockIncrement = 0.001 }.bind(model._k)
                label(model._k, converter = UserStringConverter)
            }
            field("x0 (midpoint)") {
                slider(0.0..250.0) { blockIncrement = 0.01 }.bind(model._x0)
                label(model._x0, converter = UserStringConverter)
            }
            field("v (exponent)") {
                slider(0.01..5.0) {
                    blockIncrement = 0.01
                    visibleWhen(model._vActive)
                    managedWhen(model._vActive)
                }.bind(model._v)
            }
            field("Equation") { label("").bind(model._manualEquation) }
            field("Peak") { label("").bind(model._manualPeak) }
        }
    }

    private fun EventTarget.curveFittingFieldSet() {
        fieldset("Curve Fitting") {
            label(model._fitLabel)
            field("Dates for Curve Fit") {
                RangeSlider(60.0, model.curveFitter.nowInt.toDouble(), 60.0, 60.0).apply {
                    blockIncrement = 7.0
                    majorTickUnit = 7.0
                    minorTickCount = 6
                    isShowTickLabels = true
                    isShowTickMarks = true
                    isSnapToTicks = true

                    highValueProperty().bindBidirectional(model._lastFitDay)
                    lowValueProperty().bindBidirectional(model._firstFitDay)

                    labelFormatter = object : StringConverter<Number>() {
                        override fun toString(p0: Number) = model.curveFitter.numberToDate(p0).monthDay
                        override fun fromString(p0: String?) = TODO()
                    }
                }.attachTo(this)
            }
            field("Fit to") {
                checkbox("Cumulative Count", model._fitCumulative)
                button("Autofit") {
                    alignment = Pos.TOP_CENTER
                    action { model.autofit() }
                }
            }
        }
    }

    private fun EventTarget.modelEvaluationFieldSet() {
        fieldset("Model Evaluation") {
            label("Evaluate models within the given range of dates.")
            field("Eval Days") {
                RangeSlider(60.0, model.curveFitter.nowInt.toDouble(), 60.0, 60.0).apply {
                    blockIncrement = 7.0
                    majorTickUnit = 7.0
                    minorTickCount = 6
                    isShowTickLabels = true
                    isShowTickMarks = true
                    isSnapToTicks = true

                    highValueProperty().bindBidirectional(model._lastEvalDay)
                    lowValueProperty().bindBidirectional(model._firstEvalDay)

                    labelFormatter = object : StringConverter<Number>() {
                        override fun toString(p0: Number) = model.curveFitter.numberToDate(p0).monthDay
                        override fun fromString(p0: String?) = TODO()
                    }
                }.attachTo(this)
            }
            field("Error") {
                label("").bind(model._manualLogCumStdErr)
                label("").bind(model._manualDeltaStdErr)
            }
        }
    }

    private fun EventTarget.otherForecastFieldSet() {
        fieldset("Other Forecasts") {
            field("Forecasts") {
                CheckComboBox(CovidForecasts.FORECAST_OPTIONS.asObservable()).apply {
                    checkModel.check(IHME)
                    checkModel.check(YYG)
                    Bindings.bindContent(model.otherForecasts, checkModel.checkedItems)
                }.attachTo(this)
                checkbox("Show confidence intervals").bind(model._showConfidence)
            }
            field("Dates Visible") {
                RangeSlider(90.0, model.curveFitter.nowInt.toDouble(), 90.0, 90.0).apply {
                    blockIncrement = 7.0
                    majorTickUnit = 7.0
                    minorTickCount = 6
                    isShowTickLabels = true
                    isShowTickMarks = true
                    isSnapToTicks = true

                    highValueProperty().bindBidirectional(model._lastForecastDay)
                    lowValueProperty().bindBidirectional(model._firstForecastDay)

                    labelFormatter = object : StringConverter<Number>() {
                        override fun toString(p0: Number) = model.curveFitter.numberToDate(p0).monthDay
                        override fun fromString(p0: String?) = TODO()
                    }
                }.attachTo(this)
            }
            field("Evaluation") {
                button("Save to Table") { action { model.saveExternalForecastsToTable() } }
            }
        }
    }

    //endregion

    //region CHART INITIALIZERS

    private fun EventTarget.forecastTotalsChart(): DateRangeChart {
        return datechart("Totals", "Day (or Day of Forecast)", "Actual/Forecast") {
            gridpaneConstraints { vhGrow = Priority.ALWAYS }
            isLegendVisible = false
            chartContextMenu()
        }
    }

    private fun EventTarget.forecastDeltasChart(): DateRangeChart {
        return datechart("Change per Day", "Day", "Actual/Forecast") {
            gridpaneConstraints { vhGrow = Priority.ALWAYS }
            isLegendVisible = false
            chartContextMenu()
        }
    }

    private fun EventTarget.forecastResidualsChart(): DateRangeChart {
        return datechart("Residuals (Daily)", "Day", "# more than forecasted") {
            gridpaneConstraints { vhGrow = Priority.ALWAYS }
            isLegendVisible = false
            chartContextMenu()
        }
    }

    private fun EventTarget.forecastChangeDoublingChart(): LineChart<Number, Number> {
        return linechartRangedOnFirstSeries("Change per Day vs Doubling Time", "Doubling Time", "Change per Day") {
            gridpaneConstraints { vhGrow = Priority.ALWAYS }
            animated = false
            createSymbols = false
            isLegendVisible = false
            axisSortingPolicy = LineChart.SortingPolicy.NONE
            chartContextMenu()
        }
    }

    private fun EventTarget.forecastHubberChart(): LineChart<Number, Number> {
        return linechartRangedOnFirstSeries("Percent Growth vs Total",
                NumberAxis().apply { label = "Total" },
                NumberAxis().apply {
                    label = "Percent Growth"
                    isAutoRanging = false
                    lowerBound = 0.0
                    tickUnit = 0.05
                    upperBound = 0.3
                }) {
            gridpaneConstraints { vhGrow = Priority.ALWAYS }
            animated = false
            createSymbols = false
            isLegendVisible = false
            axisSortingPolicy = LineChart.SortingPolicy.NONE
            chartContextMenu()
        }
    }

    //endregion

    //region UPDATE METHOD

    /** Plot forecast curves: min/avg/max totals predicted by day for a single region. */
    private fun updateForecasts() {
        if (!this::forecastTotals.isInitialized) return
        measureTime {
            val data0 = model.cumulativeDataSeries()
            val max0 = data0.getOrNull(0)?.maxY()
//        val maxOther = data0.drop(1).map { it.maxY() ?: 0.0 }.max()
//        (forecastTotals.yAxis as NumberAxis).limitMaxTo(maxOther, max0, 3.0)

            forecastTotals.dataSeries = data0
            forecastDeltas.dataSeries = model.dailyDataSeries()
            forecastHubbert.dataSeries = model.hubbertDataSeries()
            forecastChangeDoubling.dataSeries = model.changeDoublingDataSeries()
            forecastResiduals.dataSeries = model.residualDataSeries()
//
//        val max2 = forecastDeltas.data.getOrNull(0)?.data?.map { it.yValue.toDouble() }?.max()
//        max2?.let { (forecastTotals.yAxis as NumberAxis).limitMaxTo(3*it) }
//
//        val max1 = forecastHubbert.data.getOrNull(0)?.data?.map { it.xValue.toDouble() }?.max()
//        max1?.let { (forecastTotals.xAxis as NumberAxis).limitMaxTo(3*it) }

            val day0 = model.domain?.start

            day0?.let {
                with(axisLabeler(it)) {
                    (forecastTotals.xAxis as NumberAxis).tickLabelFormatter = this
                    (forecastDeltas.xAxis as NumberAxis).tickLabelFormatter = this
                    (forecastResiduals.xAxis as NumberAxis).tickLabelFormatter = this
                }
            }

            listOf(forecastTotals, forecastDeltas, forecastResiduals, forecastChangeDoubling, forecastHubbert).forEach { chart ->
                chart.animated = false
                chart.data.forEach { series ->
                    series.node.installHoverEffect()
                    Tooltip.install(series.node, Tooltip(series.name))

                    if ("predicted" in series.name) {
                        series.node.style = "-fx-opacity: 0.5; -fx-stroke-width: 2; -fx-stroke-dash-array: 2,2"
                        series.data.forEach { it.node?.isVisible = false }
                    }
                    if (CovidForecasts.FORECAST_OPTIONS.any { f -> f in series.name }) {
                        series.node.style = "-fx-stroke: ${modelColor(series.name)}; -fx-stroke-width: ${modelStrokeWidth(series.name)}; -fx-stroke-dash-array: 3,3"
                        series.data.forEach { it.node?.isVisible = false }
                    }
                    if ("curve" in series.name) {
                        series.node.style = "-fx-opacity: 0.5; -fx-stroke-width: 4"
                        series.data.forEach { it.node?.isVisible = false }
                    }

                    series.data.forEach {
                        it.node?.run {
                            val domainValue = if (it.xValue is Int && day0 != null) day0.plusDays(it.xValue.toLong()).monthDay else it.xValue
                            Tooltip.install(this, Tooltip("${series.name}: $domainValue -> ${it.yValue.userFormat()}"))
                        }
                    }
                }
            }
        }.also {
            if (it > 100.milliseconds) println("Forecast plots updated in $it")
        }
    }

    private fun modelColor(name: String): String {
        val color = CovidForecasts.modelColor(name)
        val opacity = opacityByDate(name.substringAfter("-").substringBefore(" "))
        return "#$color$opacity"
    }

    private fun opacityByDate(date: String) = try {
        val ld = "$date-2020".toLocalDate(M_D_YYYY)
        val age = LocalDate.now().minus(ld).toInt()
        when {
            age <= 7 -> 255.hex
            age >= 28 -> 64.hex
            else -> interpolate(age, 28, 7, 64, 255).hex
        }
    } catch (x: DateTimeParseException) {
        println("Invalid date: $date")
        "00"
    }

    private val Int.hex
        get() = toString(16)

    private fun interpolate(x: Int, from1: Int, from2: Int, to1: Int, to2: Int)
            = (to1 + (to2 - to1) / (from2 - from1).toDouble() * (x - from1)).toInt()

    private fun modelStrokeWidth(name: String) = when {
        "lower" in name || "upper" in name -> "1"
        else -> "2"
    }

    //endregion
}
