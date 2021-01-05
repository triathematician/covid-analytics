/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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
package tri.covid19.coda.history

import com.sun.javafx.charts.Legend
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import tornadofx.*
import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.coda.charts.*
import tri.covid19.coda.installStandardHoverAndTooltip
import tri.covid19.coda.utils.*
import kotlin.time.ExperimentalTime

/**
 * View with charts showing line plots for several selected regions.
 */
@ExperimentalTime
class HistoryPanelPlots constructor(val historyPanelModel: HistoryPanelModel, val hubbertPanelModel: HistoryHubbertModel) : View() {

    private lateinit var standardChart: TimeSeriesChart
    private lateinit var doublingTotalChart: DoublingTotalChart
    private lateinit var hubbertChart: HubbertChart
    private lateinit var deathCaseChart: DeathCaseChart
    private lateinit var legend: Legend

    override val root = borderpane {
        center = gridpane {
            row {
                standardChart = timeserieschart("Historical Data", "Day", "Count", historyPanelModel.logScale)
                doublingTotalChart = doublingtotalchart("Daily Count vs Doubling Time", "Doubling Time", "Daily Count")
            }
            row {
                hubbertChart = hubbertChart("Percent Growth vs Total", "Total", "Percent Growth")
                deathCaseChart = deathcasechart("Confirmed Cases vs Deaths", "Deaths", "Confirmed Cases")
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
        historyPanelModel.onChange = { updateAllCharts() }
        hubbertPanelModel.onChange = { updateAllCharts() }
        historyPanelModel._logScale.onChange { resetChartAxes() }
        updateAllCharts()
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

    /** Update all charts. */
    private fun updateAllCharts() {
        if (this::standardChart.isInitialized) {
            updateStandardChart()
            updateDoubleTotalChart()
            updateHubbertChart()
            updateDeathCaseChart()
        }
    }

    //region CHART UPDATERS

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

    private fun updateDoubleTotalChart() {
        with (doublingTotalChart) {
            series = historyPanelModel.smoothedData()
            lineWidth = lineChartWidthForCount(data.size)
            installStandardHoverAndTooltip()
        }
    }

    private fun updateDeathCaseChart() {
        with (deathCaseChart) {
            val perDay = historyPanelModel.perDay
            val deaths = historyPanelModel.smoothedData(metric = DEATHS)
            val cases = historyPanelModel.smoothedData(metric = CASES)
            series = if (perDay) deaths.map { it.deltas() } to cases.map { it.deltas() } else deaths to cases
            lineWidth = lineChartWidthForCount(data.size)
            installStandardHoverAndTooltip()
        }
    }

    //endregion

}
